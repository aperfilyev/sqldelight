package com.squareup.sqldelight.intellij.run

import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.squareup.sqldelight.intellij.util.isAndroidPluginEnabled
import com.squareup.sqldelight.intellij.util.isSqlite
import com.squareup.sqldelight.intellij.util.sqlDialect

internal class SqlDelightRunLineMarkerContributor : RunLineMarkerContributor() {

  override fun getInfo(element: PsiElement): Info? {
    if (isAndroidPluginEnabled) {
      return null
    }
    if (!element.sqlDialect.isSqlite) {
      return null
    }
    if (element !is SqlStmt) return null

    val actions = ExecutorAction.getActions()
    return Info(AllIcons.RunConfigurations.TestState.Run, actions, null)
  }
}