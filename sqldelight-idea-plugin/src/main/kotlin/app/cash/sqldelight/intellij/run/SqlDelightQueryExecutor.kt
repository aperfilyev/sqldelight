package app.cash.sqldelight.intellij.run

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.execution.ui.actions.CloseAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.jakewharton.picnic.BorderStyle
import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.renderText
import com.jakewharton.picnic.table
import java.awt.BorderLayout
import java.sql.ResultSet
import java.sql.SQLException
import javax.swing.JComponent
import javax.swing.JPanel

internal interface SqlDelightQueryExecutor {
  fun execute(statement: String)

  companion object {
    fun getInstance(project: Project): SqlDelightQueryExecutor {
      return ServiceManager.getService(project, SqlDelightQueryExecutor::class.java)!!
    }
  }
}

internal class SqlDelightQueryExecutorImpl(
  private val project: Project
) : SqlDelightQueryExecutor {

  override fun execute(statement: String) {
    val consoleView = getConsoleView(project) ?: return
    try {
      val connectionOptions = ConnectionOptions(project)
      var path = connectionOptions.filePath
      if (path.isEmpty()) {
        val ok = SelectConnectionTypeDialog(project).showAndGet()
        if (!ok) return
        path = connectionOptions.filePath
      }
      execSql(path, statement, consoleView)
    } catch (e: SQLException) {
      val message = e.message ?: "Unknown error during execution of query $statement"
      consoleView.print("$message\n", ConsoleViewContentType.LOG_ERROR_OUTPUT)
    }
  }

  private fun execSql(path: String, sql: String, consoleView: ConsoleViewImpl) {
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
    val toolbar =
      ActionManager.getInstance().createActionToolbar("RunIdeConsole", toolbarActions, false)
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
