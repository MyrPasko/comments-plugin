package com.myrpasko.commentsplugin.diff

import com.intellij.diff.FrameDiffTool
import com.intellij.diff.tools.util.base.DiffViewerBase
import com.intellij.diff.tools.util.base.DiffViewerListener
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.myrpasko.commentsplugin.model.CommentLocation
import com.myrpasko.commentsplugin.model.ReviewComment
import com.myrpasko.commentsplugin.review.ReviewCommentStore
import java.awt.BorderLayout
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.lang.reflect.Method
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

class SideBySideDiffReviewController(
    private val project: Project,
    private val adapter: DiffViewerAdapter,
    private val store: ReviewCommentStore,
) : Disposable {
    private val editor: EditorEx = adapter.rightEditor
    private var commentableLines: Set<Int> = emptySet()
    private var hoverLine: Int? = null
    private var hoverHighlighter: RangeHighlighter? = null
    private var actionPanel: DiffActionPanel? = null
    private var actionSurface: ActionSurfaceAttachment? = null
    private val viewerListener = object : DiffViewerListener() {
        override fun onAfterRediff() {
            refreshCommentableLines()
        }
    }

    private val gutterMotionListener = object : MouseMotionAdapter() {
        override fun mouseMoved(event: MouseEvent) {
            updateHover(event.y)
        }
    }

    private val gutterMouseListener = object : MouseAdapter() {
        override fun mouseExited(event: MouseEvent) {
            clearHover()
        }
    }

    init {
        installActionSurface()
        registerViewerListener()
        if (commentableLines.isNotEmpty()) {
            editor.gutterComponentEx.addMouseMotionListener(gutterMotionListener)
            editor.gutterComponentEx.addMouseListener(gutterMouseListener)
        }
        refreshCommentableLines()
    }

    override fun dispose() {
        editor.gutterComponentEx.removeMouseMotionListener(gutterMotionListener)
        editor.gutterComponentEx.removeMouseListener(gutterMouseListener)
        unregisterViewerListener()
        clearHover()
        actionSurface?.dispose()
        actionSurface = null
        actionPanel?.dispose()
        actionPanel = null
    }

    private fun installActionSurface() {
        val panel = DiffActionPanel(project, store)
        val bottomInstalled = installBottomPanel(panel)
        val plan = DiffActionSurfacePlanner.plan(bottomInstalled)

        actionSurface = when {
            plan.showBottomPanel -> PanelAttachment(panel)
            plan.showToolbarFallback -> installToolbarFallback(panel)
            else -> null
        }

        if (actionSurface == null) {
            panel.dispose()
            return
        }

        actionPanel = panel
    }

    private fun installBottomPanel(panel: DiffActionPanel): Boolean {
        val statusPanel = findStatusPanel(adapter.viewer) ?: return false
        statusPanel.add(
            panel,
            GridBagConstraints().apply {
                gridx = GridBagConstraints.RELATIVE
                gridy = 0
                weightx = 0.0
                fill = GridBagConstraints.NONE
                anchor = GridBagConstraints.EAST
            },
        )
        statusPanel.revalidate()
        statusPanel.repaint()
        return true
    }

    private fun installToolbarFallback(panel: DiffActionPanel): ActionSurfaceAttachment? {
        val root = adapter.primaryComponent
        val wrappedNorth = wrapNorthComponent(root, panel)
        if (wrappedNorth != null) {
            return wrappedNorth
        }

        val toolbarAnchor = findToolbarComponent(root) ?: return null
        val toolbarParent = toolbarAnchor.parent as? JComponent ?: return null
        toolbarParent.add(panel)
        toolbarParent.revalidate()
        toolbarParent.repaint()
        return PanelAttachment(panel)
    }

    private fun registerViewerListener() {
        val viewer = adapter.viewer as? DiffViewerBase ?: return
        viewer.addListener(viewerListener)
    }

    private fun unregisterViewerListener() {
        val viewer = adapter.viewer as? DiffViewerBase ?: return
        viewer.removeListener(viewerListener)
    }

    private fun refreshCommentableLines() {
        val nextLines = adapter.collectChangedLines()
        val hadLines = commentableLines.isNotEmpty()
        val hasLines = nextLines.isNotEmpty()
        commentableLines = nextLines

        if (!hadLines && hasLines) {
            editor.gutterComponentEx.addMouseMotionListener(gutterMotionListener)
            editor.gutterComponentEx.addMouseListener(gutterMouseListener)
            return
        }

        if (hadLines && !hasLines) {
            editor.gutterComponentEx.removeMouseMotionListener(gutterMotionListener)
            editor.gutterComponentEx.removeMouseListener(gutterMouseListener)
            clearHover()
        }
    }

    private fun updateHover(y: Int) {
        val line = hoveredCommentableLine(y) ?: run {
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

    private fun hoveredCommentableLine(y: Int): Int? {
        if (commentableLines.isEmpty()) {
            return null
        }
        val visualLine = editor.yToVisualLine(y)
        val line = editor.visualToLogicalPosition(VisualPosition(visualLine, 0)).line
        return line.takeIf(commentableLines::contains)
    }

    private fun openDialog(line: Int) {
        val location = CommentLocation(adapter.filePath, line + 1)
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
                        viewerKind = adapter.viewerKind,
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
            val hasComment = store.find(CommentLocation(adapter.filePath, line + 1)) != null
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
        fun attach(project: Project, viewer: FrameDiffTool.DiffViewer) {
            val adapter = DiffViewerAdapters.create(project, viewer) ?: return
            if (!ReviewDiffAttachmentRegistry.markAttached(viewer)) {
                return
            }
            val store = project.getService(ReviewCommentStore::class.java)
            val controller = SideBySideDiffReviewController(project, adapter, store)
            Disposer.register(viewer, controller)
        }

        private fun findStatusPanel(viewer: FrameDiffTool.DiffViewer): JComponent? {
            if (viewer !is DiffViewerBase) {
                return null
            }

            val method = findMethod(viewer.javaClass, "getStatusPanel") ?: return null
            return try {
                method.invoke(viewer) as? JComponent
            } catch (_: ReflectiveOperationException) {
                null
            }
        }

        private fun findMethod(type: Class<*>, name: String): Method? {
            var current: Class<*>? = type
            while (current != null) {
                current.declaredMethods.firstOrNull { method ->
                    method.name == name && method.parameterCount == 0
                }?.let { method ->
                    method.isAccessible = true
                    return method
                }
                current = current.superclass
            }
            return null
        }

        private fun wrapNorthComponent(
            root: JComponent,
            panel: DiffActionPanel,
        ): ActionSurfaceAttachment? {
            val borderLayout = root.layout as? BorderLayout ?: return null
            val north = borderLayout.getLayoutComponent(BorderLayout.NORTH) ?: return null

            root.remove(north)

            val wrapper = JPanel(BorderLayout()).apply {
                isOpaque = false
                add(north, BorderLayout.CENTER)
                add(panel, BorderLayout.EAST)
            }

            root.add(wrapper, BorderLayout.NORTH)
            root.revalidate()
            root.repaint()

            return object : ActionSurfaceAttachment {
                override fun dispose() {
                    root.remove(wrapper)
                    root.add(north, BorderLayout.NORTH)
                    root.revalidate()
                    root.repaint()
                }
            }
        }

        private fun findToolbarComponent(component: JComponent): JComponent? {
            if (component.javaClass.name.contains("ActionToolbar")) {
                return component
            }
            component.components.forEach { child ->
                val childComponent = child as? JComponent ?: return@forEach
                findToolbarComponent(childComponent)?.let { return it }
            }
            return null
        }
    }

    private interface ActionSurfaceAttachment : Disposable

    private class PanelAttachment(
        private val panel: DiffActionPanel,
    ) : ActionSurfaceAttachment {
        override fun dispose() {
            val parent = panel.parent
            parent?.remove(panel)
            parent?.revalidate()
            parent?.repaint()
        }
    }
}
