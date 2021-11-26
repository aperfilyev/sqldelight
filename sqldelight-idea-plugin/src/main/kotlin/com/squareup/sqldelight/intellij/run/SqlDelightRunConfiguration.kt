package com.squareup.sqldelight.intellij.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

internal class SqlDelightRunConfiguration(
  project: Project,
  factory: ConfigurationFactory
) : LocatableConfigurationBase<SqlDelightRunConfigurationOptions>(project, factory) {

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    val sql = getSql()
    val parameters = getParameters()
    return SqlDelightRunProfileState(environment.project, sql, parameters)
  }

  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    return SqlDelightSettingsEditor()
  }

  override fun getOptions(): SqlDelightRunConfigurationOptions {
    return super.getOptions() as SqlDelightRunConfigurationOptions
  }

  fun setSql(sql: String) {
    options.sqlScript = sql
  }

  fun getSql(): String {
    return options.sqlScript.orEmpty()
  }

  fun getParameters(): List<SqlParameter> {
    return options.sqlParameters
  }

  fun setParameters(parameters: List<SqlParameter>) {
    options.sqlParameters = parameters.toMutableList()
  }
}
