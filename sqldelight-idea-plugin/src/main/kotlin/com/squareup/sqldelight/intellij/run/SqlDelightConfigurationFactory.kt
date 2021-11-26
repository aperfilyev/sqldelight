package com.squareup.sqldelight.intellij.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

internal class SqlDelightConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
  override fun getId(): String {
    return CONFIGURATION_ID
  }

  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    return SqlDelightRunConfiguration(project, this)
  }

  override fun getOptionsClass(): Class<out BaseState> {
    return SqlDelightRunConfigurationOptions::class.java
  }
}