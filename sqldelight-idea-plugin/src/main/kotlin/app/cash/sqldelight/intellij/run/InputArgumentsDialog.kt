package app.cash.sqldelight.intellij.run

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.panel
import javax.swing.JComponent

internal class InputArgumentsDialog(
  project: Project,
  private val parameters: List<SqlParameter>
) : DialogWrapper(project) {

  init {
    init()
  }

  private val _result = mutableListOf<SqlParameter>()
  val result: List<SqlParameter> = _result

  override fun createCenterPanel(): JComponent {
    return panel {
      parameters.forEach { parameter ->
        row("${parameter.name}:") {
          textField(parameter::value, {
            _result.add(parameter.copy(value = it))
          })
        }
      }
    }
  }
}
