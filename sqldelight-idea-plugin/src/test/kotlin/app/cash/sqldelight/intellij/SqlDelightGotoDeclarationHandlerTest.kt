package app.cash.sqldelight.intellij

import com.alecstrong.sql.psi.core.psi.SqlColumnName
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
import org.jetbrains.kotlin.idea.KotlinFileType

class SqlDelightGotoDeclarationHandlerTest : SqlDelightFixtureTestCase() {

  override val fixtureDirectory: String = "find-usages"

  fun testProperty() {
    myFixture.configureByText(KotlinFileType.INSTANCE, """
      |package com.example
      |
      |fun main() {
      |  val exampleQueries = ExampleQueries()
      |  val query = exampleQueries.selectById(id = 1L)
      |  val id = query.executeAsOneOrNull()?.i<caret>d
      |}
    """.trimMargin())

    myFixture.copyFileToProject("Example.sq", "sqldelight/com/example/Example.sq")
    myFixture.copyFileToProject("Example.kt", "build/com/example/Example.kt")
    myFixture.copyFileToProject("ExampleQueries.kt", "build/com/example/ExampleQueries.kt")
    myFixture.copyFileToProject("Query.kt", "build/com/example/Query.kt")

    val targetElements = GotoDeclarationAction.findAllTargetElements(
      myFixture.project,
      myFixture.editor,
      myFixture.caretOffset
    )

    assertThat(targetElements.filterIsInstance<SqlColumnName>()).isNotEmpty()
  }
  fun testNamedParameter() {
    myFixture.configureByText(KotlinFileType.INSTANCE, """
      |package com.example
      |
      |fun main() {
      |  val exampleQueries = ExampleQueries()
      |  val query = exampleQueries.selectById(i<caret>d = 1L)
      |  val id = query.executeAsOneOrNull()?.id
      |}
    """.trimMargin())

    myFixture.copyFileToProject("Example.sq", "sqldelight/com/example/Example.sq")
    myFixture.copyFileToProject("Example.kt", "build/com/example/Example.kt")
    myFixture.copyFileToProject("ExampleQueries.kt", "build/com/example/ExampleQueries.kt")
    myFixture.copyFileToProject("Query.kt", "build/com/example/Query.kt")

    val targetElements = GotoDeclarationAction.findAllTargetElements(
      myFixture.project,
      myFixture.editor,
      myFixture.caretOffset
    )

    assertThat(targetElements.filterIsInstance<SqlColumnName>()).isNotEmpty()
  }
}
