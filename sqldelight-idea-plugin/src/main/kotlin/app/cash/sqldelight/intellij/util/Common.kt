package app.cash.sqldelight.intellij.util

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId

internal val isAndroidPluginEnabled: Boolean
  get() = !PluginManagerCore.isDisabled(PluginId.getId("org.jetbrains.android"))
