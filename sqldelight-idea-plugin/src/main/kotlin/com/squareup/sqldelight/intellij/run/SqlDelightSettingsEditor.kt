package com.squareup.sqldelight.intellij.run

import com.intellij.openapi.options.SettingsEditor
import javax.swing.JComponent
import javax.swing.JPanel

internal class SqlDelightSettingsEditor : SettingsEditor<SqlDelightRunConfiguration>() {
  override fun resetEditorFrom(s: SqlDelightRunConfiguration) {
  }

  override fun applyEditorTo(s: SqlDelightRunConfiguration) {
  }

  override fun createEditor(): JComponent {
    return JPanel()
  }
}