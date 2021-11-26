package com.squareup.sqldelight.intellij.run

import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlStmtList
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.sqldelight.core.compiler.model.NamedMutator
import com.squareup.sqldelight.core.lang.psi.StmtIdentifierMixin
import com.squareup.sqldelight.core.lang.util.range
import com.squareup.sqldelight.core.lang.util.rawSqlText
import com.squareup.sqldelight.intellij.util.isSqlite
import com.squareup.sqldelight.intellij.util.sqlDialect

internal class SqlDelightRunConfigurationProducer : LazyRunConfigurationProducer<SqlDelightRunConfiguration>() {
  override fun getConfigurationFactory(): ConfigurationFactory {
    return SqlDelightRunConfigurationType.getInstance().configurationFactories.first()
  }

  override fun isConfigurationFromContext(
    configuration: SqlDelightRunConfiguration,
    context: ConfigurationContext
  ): Boolean {
    val psiLocation: PsiElement = context.psiLocation ?: return false
    val sqlStmt = psiLocation.findSqlStmt() ?: return false

    return configuration.getSql() == sqlStmt.rawSqlText()
  }

  override fun setupConfigurationFromContext(
    configuration: SqlDelightRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement>
  ): Boolean {
    if (sourceElement.isNull) {
      return false
    }
    val psiElement = sourceElement.get()
    if (!psiElement.sqlDialect.isSqlite) {
      return false
    }

    val sqlStmt: SqlStmt = psiElement.findSqlStmt() ?: return false

    val stmtIdentifier = sqlStmt.prevVisibleSibling() as? StmtIdentifierMixin
    if (stmtIdentifier != null) {
      val parameters = findParameters(sqlStmt, stmtIdentifier)
      configuration.setParameters(parameters)
    }
    val label = stmtIdentifier?.identifier()?.text ?: "statement"
    configuration.name = label
    configuration.setSql(sqlStmt.rawSqlText())
    return true
  }

  private fun findParameters(sqlStmt: SqlStmt, identifier: StmtIdentifierMixin): List<SqlParameter> {
    val mutator = when {
      sqlStmt.deleteStmtLimited != null -> NamedMutator.Delete(
        sqlStmt.deleteStmtLimited!!, identifier
      )
      sqlStmt.insertStmt != null -> NamedMutator.Insert(
        sqlStmt.insertStmt!!, identifier
      )
      sqlStmt.updateStmtLimited != null -> NamedMutator.Update(
        sqlStmt.updateStmtLimited!!, identifier
      )
      else -> null
    } ?: return emptyList()
    val offset = sqlStmt.textOffset
    val argumentList: List<IntRange> = mutator.arguments
      .flatMap { it.bindArgs }
      .map {
        val textRange = it.range
        IntRange(textRange.first - offset, textRange.last - offset)
      }
    val parameters: List<String> = mutator.parameters
      .map { it.name }
    return argumentList.zip(parameters) { range, name ->
        SqlParameter(range, name)
      }
  }

  private fun PsiElement.findSqlStmt(): SqlStmt? =
    if (parent is SqlStmtList) {
      this as? SqlStmt
    } else {
      PsiTreeUtil.getTopmostParentOfType(this, SqlStmt::class.java)
    }

  private fun PsiElement.prevVisibleSibling(): PsiElement? {
    return generateSequence(prevSibling) { it.prevSibling }
      .firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
  }
}