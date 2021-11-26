package com.squareup.sqldelight.intellij.run

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.icons.AllIcons

internal const val CONFIGURATION_ID = "SqlDelight"

internal class SqlDelightRunConfigurationType : ConfigurationTypeBase(
  id = CONFIGURATION_ID,
  displayName = "Sql Statement",
  description = null,
  icon = AllIcons.Providers.Sqlite
) {

  init {
    addFactory(SqlDelightConfigurationFactory(this))
  }

  companion object {
    fun getInstance(): SqlDelightRunConfigurationType {
      return runConfigurationType()
    }
  }
}