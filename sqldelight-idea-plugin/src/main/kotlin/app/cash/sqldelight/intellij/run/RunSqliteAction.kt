package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.lang.SqlDelightFile
import app.cash.sqldelight.core.lang.SqlDelightFileType
import app.cash.sqldelight.core.lang.util.findChildOfType
import app.cash.sqldelight.core.lang.util.rawSqlText
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.execution.ui.actions.CloseAction
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.layout.panel
import com.jakewharton.picnic.BorderStyle
import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.renderText
import com.jakewharton.picnic.table
import java.awt.BorderLayout
import java.sql.ResultSet
import java.sql.SQLException
import javax.swing.JComponent
import javax.swing.JPanel

internal class RunSqliteAction(
  private val project: Project,
  private val sql: String,
  private val parameters: List<SqlParameter>
) : AnAction() {

  override fun actionPerformed(actionEvent: AnActionEvent) {
    val consoleView = getConsoleView(project) ?: return
    try {
      var path = PropertiesComponent.getInstance(project).getValue(DB_PATH_PROPERTY)
      if (path == null) {
        val ok = SelectConnectionTypeDialog(project).showAndGet()
        if (!ok) return
        path = PropertiesComponent.getInstance(project).getValue(DB_PATH_PROPERTY)
      }
      execSql(path!!, sql, parameters, consoleView)
    } catch (e: SQLException) {
      val message = e.message ?: "Unknown error during execution of query $sql"
      consoleView.print("$message\n", ConsoleViewContentType.LOG_ERROR_OUTPUT)
    }
  }

  private fun execSql(
    path: String,
    sql: String,
    parameters: List<SqlParameter>,
    consoleView: ConsoleViewImpl
  ) {
    var sql = sql
    if (parameters.isNotEmpty()) {
      val dialog = InputArgumentsDialog(project, parameters)
      val ok = dialog.showAndGet()
      if (!ok) return

      sql = bindParameters(sql, dialog.result)
      if (sql.isEmpty()) {
        return
      }
    }
    ConnectionManager.getConnection(path).use { connection ->
      val statement = connection.createStatement()
      val hasResult = statement.execute(sql)
      if (hasResult) {
        processResultSet(consoleView, statement.resultSet)
      } else {
        val text = "Query executed successfully (affected ${statement.updateCount} rows)"
        consoleView.print("$text\n", ConsoleViewContentType.NORMAL_OUTPUT)
      }
    }
  }

  private fun bindParameters(
    sql: String,
    parameters: List<SqlParameter>
  ): String {
    val replacements = parameters.mapNotNull { p ->
      if (p.value.isEmpty()) {
        return@mapNotNull null
      }
      p.range to "'${p.value}'"
    }
    if (replacements.isEmpty()) {
      return ""
    }

    val factory = PsiFileFactory.getInstance(project)
    return runReadAction {
      val dummyFile = factory.createFileFromText(
        "_Dummy_.${SqlDelightFileType.EXTENSION}",
        SqlDelightFileType,
        sql
      ) as SqlDelightFile
      val stmt = dummyFile.findChildOfType<SqlStmt>()
      stmt?.rawSqlText(replacements).orEmpty()
    }
  }

  private fun processResultSet(consoleView: ConsoleViewImpl, resultSet: ResultSet) {
    val metaData = resultSet.metaData
    val range = 1..metaData.columnCount
    val columnNames = range.map(metaData::getColumnName)
    val rows = mutableListOf<List<String>>()
    while (resultSet.next()) {
      rows += range.map(resultSet::getString)
    }
    val table = table {
      style {
        borderStyle = BorderStyle.Solid
      }
      cellStyle {
        alignment = TextAlignment.MiddleCenter
        border = true
      }
      row(*columnNames.toTypedArray())
      rows.forEach { row(*it.toTypedArray()) }
    }
    consoleView.print("${table.renderText()}\n", ConsoleViewContentType.NORMAL_OUTPUT)
  }

  private fun getConsoleView(project: Project): ConsoleViewImpl? {
    val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
    val toolbarActions = DefaultActionGroup()
    val panel: JComponent = JPanel(BorderLayout())
    panel.add(consoleView.component, BorderLayout.CENTER)
    val toolbar = ActionManager.getInstance().createActionToolbar("RunIdeConsole", toolbarActions, false)
    toolbar.setTargetComponent(consoleView.component)
    panel.add(toolbar.component, BorderLayout.WEST)
    val descriptor = RunContentDescriptor(consoleView, null, panel, "SqlDelight")
    val executor = DefaultRunExecutor.getRunExecutorInstance()
    toolbarActions.addAll(*consoleView.createConsoleActions())
    toolbarActions.add(CloseAction(executor, descriptor, project))
    RunContentManager.getInstance(project).showRunContent(executor, descriptor)
    return descriptor.executionConsole as? ConsoleViewImpl
  }
}

internal class InputArgumentsDialog(
  project: Project,
  private val parameters: List<SqlParameter>
) : DialogWrapper(project) {

  init {
    init()
  }

  private val _result = mutableListOf<SqlParameter>()
  val result: List<SqlParameter> = _result

  override fun createCenterPanel(): JComponent {
    return panel {
      parameters.forEach { parameter ->
        row("${parameter.name}:") {
          textField({ parameter.value }, {
            _result.add(parameter.copy(value = it))
          })
        }
      }
    }
  }
}
