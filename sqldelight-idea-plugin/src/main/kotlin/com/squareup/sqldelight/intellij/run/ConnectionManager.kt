package com.squareup.sqldelight.intellij.run

import java.sql.Connection
import java.sql.DriverManager

object ConnectionManager {

  init {
    Class.forName("org.sqlite.JDBC")
  }

  fun getConnection(path: String): Connection {
    return DriverManager.getConnection("jdbc:sqlite:$path")
  }
}