package com.squareup.sqldelight.intellij.run

import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.layout.panel
import com.jakewharton.picnic.BorderStyle
import com.jakewharton.picnic.TextAlignment
import com.jakewharton.picnic.renderText
import com.jakewharton.picnic.table
import com.squareup.sqldelight.core.lang.SqlDelightFile
import com.squareup.sqldelight.core.lang.SqlDelightFileType
import com.squareup.sqldelight.core.lang.util.findChildOfType
import com.squareup.sqldelight.core.lang.util.rawSqlText
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.sql.ResultSet
import java.sql.SQLException
import java.util.concurrent.CountDownLatch
import javax.swing.JComponent

internal class ExecuteSqlTask(
  project: Project,
  private val processHandler: SqlDelightProcessHandler,
  private val sql: String,
  private val parameters: List<SqlParameter>
) : Task.Backgroundable(project, "SQLDelight") {

  private val path = ("${project.basePath}/Database.db")

  override fun run(indicator: ProgressIndicator) {
    try {
      processHandler.startNotify()
      execSql(sql, parameters)
    } catch (e: SQLException) {
      val message = e.message ?: "Unknown error during execution of query $sql"
      processHandler.notifyTextAvailable(message, ProcessOutputType.STDERR)
    } finally {
      processHandler.destroyProcess()
    }
  }

  private fun execSql(sql: String, parameters: List<SqlParameter>) {
    var sql = sql
    if (parameters.isNotEmpty()) {
      if (!showDialog(parameters)) {
        return
      }
      sql = bindParameters(sql, parameters)
      if (sql.isEmpty()) {
        return
      }
    }
    ConnectionManager.getConnection(path).use { connection ->
      val statement = connection.createStatement()
      val hasResult = statement.execute(sql)
      if (hasResult) {
        processResultSet(statement.resultSet)
      } else {
        val text = "Query executed successfully (affected ${statement.updateCount} rows)"
        processHandler.notifyTextAvailable(text, ProcessOutputType.STDOUT)
      }
    }
  }

  private fun showDialog(parameters: List<SqlParameter>): Boolean {
    val latch = CountDownLatch(1)
    var ok = false
    ApplicationManager.getApplication().invokeLater {
      val dialog = InputArgumentsDialog(project, parameters)
      dialog.window.addWindowListener(object : WindowAdapter() {
        override fun windowClosed(e: WindowEvent?) {
          latch.countDown()
        }
      })
      ok = dialog.showAndGet()
    }
    latch.await()
    return ok
  }

  private fun bindParameters(
    sql: String,
    parameters: List<SqlParameter>
  ): String {
    val replacements = parameters.mapNotNull { p ->
      if (p.value.isNullOrEmpty()) {
        return@mapNotNull null
      }
      p.range to "'${p.value!!}'"
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

  private fun processResultSet(resultSet: ResultSet) {
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
    processHandler.notifyTextAvailable(table.renderText(), ProcessOutputType.STDOUT)
  }
}

internal class InputArgumentsDialog(
  project: Project,
  private val parameters: List<SqlParameter>
) : DialogWrapper(project) {

  init {
    init()
  }

  override fun createCenterPanel(): JComponent {
    return panel {
      parameters.forEach { rangeName ->
        row("${rangeName.name}:") {
          textField({ rangeName.value.orEmpty() }, { rangeName.value = it })
        }
      }
    }
  }
}
