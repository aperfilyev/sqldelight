package com.squareup.sqldelight.intellij.run

import com.intellij.execution.process.ProcessHandler
import java.io.OutputStream

class SqlDelightProcessHandler : ProcessHandler() {

  override fun detachProcessImpl() {
    notifyProcessDetached()
  }

  override fun destroyProcessImpl() {
    notifyProcessTerminated(0)
  }

  override fun detachIsDefault(): Boolean {
    return false
  }

  override fun getProcessInput(): OutputStream? {
    return null
  }
}
