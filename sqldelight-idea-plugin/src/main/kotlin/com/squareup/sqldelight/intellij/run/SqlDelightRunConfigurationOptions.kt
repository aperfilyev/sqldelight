package com.squareup.sqldelight.intellij.run

import com.intellij.execution.configurations.LocatableRunConfigurationOptions
import com.intellij.openapi.components.BaseState

internal class SqlDelightRunConfigurationOptions : LocatableRunConfigurationOptions() {
  var sqlScript by string()
  var sqlParameters by list<SqlParameter>()
}

internal class SqlParameter private constructor(): BaseState() {
  var name by string()
  var value by string()
  var start by property(0)
  var end by property(0)

  val range: IntRange
    get() = IntRange(start, end)

  companion object {
    internal operator fun invoke(range: IntRange, name: String): SqlParameter {
      return SqlParameter().apply {
        start = range.first
        end = range.last
        this@apply.name = name
      }
    }
  }
}
