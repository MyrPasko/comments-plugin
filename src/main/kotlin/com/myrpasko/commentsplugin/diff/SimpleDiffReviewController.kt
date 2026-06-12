package com.myrpasko.commentsplugin.diff

import com.intellij.diff.tools.simple.SimpleDiffChange
import com.intellij.diff.tools.simple.SimpleDiffViewer
import com.intellij.diff.util.Side
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.myrpasko.commentsplugin.model.CommentLocation
import com.myrpasko.commentsplugin.model.ReviewComment
import com.myrpasko.commentsplugin.review.ReviewCommentStore
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JPanel

class SimpleDiffReviewController(
    private val project: Project,
    private val viewer: SimpleDiffViewer,
    private val store: ReviewCommentStore,
    private val filePath: String,
) : Disposable {
    private val editor: EditorEx = viewer.getEditor(Side.RIGHT)
    private val commentableLines: Set<Int> = buildCommentableLines()
    private var hoverLine: Int? = null
    private var hoverHighlighter: RangeHighlighter? = null
    private var bottomPanel: DiffActionPanel? = null

    private val motionListener = object : EditorMouseMotionListener {
        override fun mouseMoved(event: EditorMouseEvent) {
            updateHover(event)
        }
    }

    private val mouseListener = object : EditorMouseListener {
        override fun mouseExited(event: EditorMouseEvent) {
            clearHover()
        }
    }

    init {
        if (commentableLines.isNotEmpty()) {
            installBottomPanel()
            editor.addEditorMouseMotionListener(motionListener)
            editor.addEditorMouseListener(mouseListener)
        }
    }

    override fun dispose() {
        editor.removeEditorMouseMotionListener(motionListener)
        editor.removeEditorMouseListener(mouseListener)
        clearHover()
        bottomPanel?.let(store::removeListener)
        bottomPanel = null
    }

    private fun installBottomPanel() {
        val component = viewer.component
        if (component.layout !is BorderLayout) {
            return
        }

        val panel = DiffActionPanel(project, store)
        (component as? JPanel)?.add(panel, BorderLayout.SOUTH)
        component.revalidate()
        component.repaint()
        bottomPanel = panel
    }

    private fun buildCommentableLines(): Set<Int> {
        return viewer.getDiffChanges()
            .asSequence()
            .filter(SimpleDiffChange::isValid)
            .flatMap { change ->
                val start = change.getStartLine(Side.RIGHT)
                val end = change.getEndLine(Side.RIGHT)
                (start until end).asSequence()
            }
            .toSet()
    }

    private fun updateHover(event: EditorMouseEvent) {
        val line = hoveredCommentableLine(event) ?: run {
            clearHover()
            return
        }

        if (hoverLine == line) {
            return
        }

        clearHover()
        hoverLine = line

        val highlighter = editor.markupModel.addLineHighlighter(line, HighlighterLayer.SELECTION - 1, null)
        highlighter.gutterIconRenderer = HoverIconRenderer(line)
        hoverHighlighter = highlighter
    }

    private fun clearHover() {
        hoverLine = null
        hoverHighlighter?.let(editor.markupModel::removeHighlighter)
        hoverHighlighter = null
    }

    private fun hoveredCommentableLine(event: EditorMouseEvent): Int? {
        if (event.area !in SUPPORTED_GUTTER_AREAS) {
            return null
        }

        val line = editor.xyToLogicalPosition(event.mouseEvent.point).line
        return line.takeIf(commentableLines::contains)
    }

    private fun openDialog(line: Int) {
        val location = CommentLocation(filePath, line + 1)
        val existing = store.find(location)
        val dialog = CommentDialog(project, existing?.commentText, existing != null)
        when (val result = dialog.showDialog()) {
            is CommentDialog.Result.Comment -> {
                store.upsert(
                    ReviewComment(
                        id = location.id(),
                        filePath = location.filePath,
                        lineNumber = location.lineNumber,
                        commentText = result.text,
                        lineTextPreview = linePreview(line),
                        viewerKind = "simple-diff",
                    ),
                )
            }

            CommentDialog.Result.Remove -> store.remove(location)
            CommentDialog.Result.Cancel -> Unit
        }
    }

    private fun linePreview(line: Int): String? {
        val document = editor.document
        if (line < 0 || line >= document.lineCount) {
            return null
        }

        val startOffset = document.getLineStartOffset(line)
        val endOffset = document.getLineEndOffset(line)
        return document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset)).trim().ifEmpty { null }
    }

    private inner class HoverIconRenderer(
        private val line: Int,
    ) : GutterIconRenderer() {
        override fun getIcon(): Icon = AllIcons.General.Add

        override fun getTooltipText(): String {
            val hasComment = store.find(CommentLocation(filePath, line + 1)) != null
            return if (hasComment) "Edit review comment" else "Add review comment"
        }

        override fun equals(other: Any?): Boolean {
            return other is HoverIconRenderer && other.line == line
        }

        override fun hashCode(): Int = line

        override fun getClickAction(): com.intellij.openapi.actionSystem.AnAction {
            return object : com.intellij.openapi.actionSystem.AnAction() {
                override fun actionPerformed(event: com.intellij.openapi.actionSystem.AnActionEvent) {
                    openDialog(line)
                }
            }
        }
    }

    companion object {
        private val SUPPORTED_GUTTER_AREAS = setOf(
            EditorMouseEventArea.LINE_NUMBERS_AREA,
            EditorMouseEventArea.ANNOTATIONS_AREA,
            EditorMouseEventArea.LINE_MARKERS_AREA,
        )

        fun attach(project: Project, viewer: SimpleDiffViewer) {
            val filePath = com.myrpasko.commentsplugin.util.ProjectPathResolver
                .resolveRelativePath(project, viewer.getContent(Side.RIGHT))
                ?: return
            val store = project.getService(ReviewCommentStore::class.java)
            val controller = SimpleDiffReviewController(project, viewer, store, filePath)
            Disposer.register(viewer, controller)
        }
    }
}
