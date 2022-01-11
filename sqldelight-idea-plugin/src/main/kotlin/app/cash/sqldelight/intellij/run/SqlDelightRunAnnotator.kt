package app.cash.sqldelight.intellij.run

import app.cash.sqldelight.core.compiler.model.NamedMutator
import app.cash.sqldelight.core.lang.psi.StmtIdentifierMixin
import app.cash.sqldelight.core.lang.util.range
import app.cash.sqldelight.core.lang.util.rawSqlText
import com.alecstrong.sql.psi.core.psi.SqlStmt
import com.alecstrong.sql.psi.core.psi.SqlStmtList
import com.intellij.icons.AllIcons
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon

class SqlDelightRunAnnotator : Annotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element.parent !is SqlStmtList || element !is SqlStmt) return

    if (PsiTreeUtil.hasErrorElements(element)) {
      return
    }

    val text = element.rawSqlText().trim().replace("\\s+".toRegex(), " ")

    val stmtIdentifier = element.prevVisibleSibling() as? StmtIdentifierMixin
    var parameters = emptyList<SqlParameter>()
    if (stmtIdentifier != null) {
      parameters = findParameters(element, stmtIdentifier)
    }

    holder.newAnnotation(HighlightSeverity.INFORMATION, "")
      .gutterIconRenderer(
        RunSqliteStatementGutterIconRenderer(
          element.project,
          text,
          parameters
        )
      )
      .create()
  }

  private fun PsiElement.prevVisibleSibling(): PsiElement? {
    return generateSequence(prevSibling) { it.prevSibling }
      .firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
  }

  private fun findParameters(
    sqlStmt: SqlStmt,
    identifier: StmtIdentifierMixin
  ): List<SqlParameter> {
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

  private data class RunSqliteStatementGutterIconRenderer(
    private val project: Project,
    private val sql: String,
    private val parameters: List<SqlParameter>,
  ) : GutterIconRenderer() {
    override fun getIcon(): Icon {
      return AllIcons.RunConfigurations.TestState.Run
    }

    override fun getTooltipText(): String {
      return "Run Sqlite statement"
    }

    override fun getClickAction(): AnAction {
      return RunSqliteAction(project, sql, parameters)
    }
  }
}

internal data class SqlParameter(
  val name: String,
  val value: String,
  val start: Int,
  val end: Int
) {

  val range: IntRange = IntRange(start, end)

  companion object {
    internal operator fun invoke(range: IntRange, name: String): SqlParameter {
      return SqlParameter(
        name = name,
        value = "",
        start = range.first,
        end = range.last
      )
    }
  }
}
