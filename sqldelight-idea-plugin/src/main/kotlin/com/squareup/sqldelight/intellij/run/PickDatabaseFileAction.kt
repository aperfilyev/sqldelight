package com.squareup.sqldelight.intellij.run

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileTypeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.RecentsManager
import com.intellij.ui.layout.panel
import javax.swing.JComponent

internal const val RECENTS_KEY = "ActionDialog.RECENTS_KEY"
internal const val DB_PATH_PROPERTY = "com.squareup.sqldelight.dbPath"

internal class PickDatabaseFileAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    if (project != null) {
      PickDatabaseFileDialog(project).show()
    }
  }

  internal class PickDatabaseFileDialog(private val project: Project) : DialogWrapper(project) {

    init {
      init()
    }

    private var selectedPath: String? = null

    override fun createCenterPanel(): JComponent {
      val history = RecentsManager.getInstance(project).getRecentEntries(RECENTS_KEY)
      val chooserDescriptor = FileTypeDescriptor("Choose Database", "db")
      return panel {
        row("Database file:") {
          textFieldWithHistoryWithBrowseButton(
            browseDialogTitle = "File",
            value = PropertiesComponent.getInstance(project).getValue(DB_PATH_PROPERTY),
            fileChooserDescriptor = chooserDescriptor,
            historyProvider = { history.orEmpty() }
          ) { vFile -> vFile.path.also { selectedPath = it } }
        }
      }
    }

    override fun doOKAction() {
      super.doOKAction()
      if (!selectedPath.isNullOrEmpty()) {
        RecentsManager.getInstance(project).registerRecentEntry(RECENTS_KEY, selectedPath)
        PropertiesComponent.getInstance(project).setValue(DB_PATH_PROPERTY, selectedPath)
      }
    }
  }
}