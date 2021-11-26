package com.squareup.sqldelight.intellij.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project

internal class SqlDelightRunProfileState(
  private val project: Project,
  private val sql: String,
  private val parameters: List<SqlParameter>,
) : RunProfileState {
  override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
    val handler = SqlDelightProcessHandler()
    val task = ExecuteSqlTask(
      project = project,
      processHandler = handler,
      sql = sql,
      parameters = parameters
    )
    val consoleView = TextConsoleBuilderFactory.getInstance()
      .createBuilder(project)
      .console
    consoleView.attachToProcess(handler)
    ProgressManager.getInstance().run(task)
    return DefaultExecutionResult(consoleView, handler)
  }
}