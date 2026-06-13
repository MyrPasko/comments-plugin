package com.myrpasko.commentsplugin.diff

import com.intellij.diff.FrameDiffTool
import com.intellij.diff.tools.util.base.DiffViewerBase
import com.intellij.diff.tools.util.base.DiffViewerListener
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.myrpasko.commentsplugin.model.ReviewComment
import com.myrpasko.commentsplugin.review.ReviewCommentStore
import java.awt.BorderLayout
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
) : Disposable, ReviewCommentStore.Listener {
    private var anchorsById: Map<String, DiffCommentAnchor> = emptyMap()
    private var anchorsByEditorAndLine: Map<EditorEx, Map<Int, DiffCommentAnchor>> = emptyMap()
    private val editorListeners = linkedMapOf<EditorEx, EditorListeners>()
    private val commentHighlighters = linkedMapOf<EditorEx, MutableMap<String, RangeHighlighter>>()
    private var hoverAnchorId: String? = null
    private var hoverHighlighter: HoverHighlighter? = null
    private var activePopupAnchorId: String? = null
    private var activePopup: com.intellij.openapi.ui.popup.JBPopup? = null
    private var actionPanel: DiffActionPanel? = null
    private var actionSurface: ActionSurfaceAttachment? = null

    private val viewerListener = object : DiffViewerListener() {
        override fun onAfterRediff() {
            refreshCommentAnchors()
        }
    }

    init {
        installActionSurface()
        registerViewerListener()
        store.addListener(this)
        refreshCommentAnchors()
    }

    override fun dispose() {
        store.removeListener(this)
        unregisterAllEditorListeners()
        unregisterViewerListener()
        clearHover()
        clearCommentIndicators()
        activePopup?.cancel()
        activePopup = null
        activePopupAnchorId = null
        actionSurface?.dispose()
        actionSurface = null
        actionPanel?.dispose()
        actionPanel = null
    }

    override fun commentsChanged() {
        if (hoverAnchorId?.let(::anchorHasComment) == true) {
            clearHover()
        }
        refreshCommentIndicators()
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

    private fun refreshCommentAnchors() {
        val nextAnchors = adapter.collectCommentAnchors()
        anchorsById = nextAnchors.associateBy(DiffCommentAnchor::id)
        anchorsByEditorAndLine = nextAnchors
            .groupBy(DiffCommentAnchor::editor)
            .mapValues { (_, anchors) -> anchors.associateBy(DiffCommentAnchor::editorLine) }

        syncEditorListeners()
        refreshCommentIndicators()

        if (hoverAnchorId != null && hoverAnchorId !in anchorsById) {
            clearHover()
        }
    }

    private fun syncEditorListeners() {
        val nextEditors = anchorsByEditorAndLine.keys
        val removedEditors = editorListeners.keys - nextEditors
        removedEditors.forEach { editor ->
            editorListeners.remove(editor)?.dispose()
        }

        nextEditors.forEach { editor ->
            if (editorListeners.containsKey(editor)) {
                return@forEach
            }

            val motionListener = object : MouseMotionAdapter() {
                override fun mouseMoved(event: MouseEvent) {
                    updateHover(editor, event.y)
                }
            }
            val mouseListener = object : MouseAdapter() {
                override fun mouseExited(event: MouseEvent) {
                    if (hoverHighlighter?.editor == editor) {
                        clearHover()
                    }
                }
            }

            editor.gutterComponentEx.addMouseMotionListener(motionListener)
            editor.gutterComponentEx.addMouseListener(mouseListener)
            editorListeners[editor] = EditorListeners(editor, motionListener, mouseListener)
        }
    }

    private fun unregisterAllEditorListeners() {
        editorListeners.values.forEach(EditorListeners::dispose)
        editorListeners.clear()
    }

    private fun refreshCommentIndicators() {
        val targetIds = store.getAll()
            .asSequence()
            .map(ReviewComment::id)
            .filter(anchorsById::containsKey)
            .toSet()

        commentHighlighters.forEach { (editor, highlightersById) ->
            val removedIds = highlightersById.keys - targetIds
            removedIds.forEach { id ->
                highlightersById.remove(id)?.let(editor.markupModel::removeHighlighter)
            }
        }

        targetIds.forEach { id ->
            if (commentHighlighters.values.any { highlighters -> id in highlighters }) {
                return@forEach
            }

            val anchor = anchorsById[id] ?: return@forEach
            val highlighter = anchor.editor.markupModel.addLineHighlighter(anchor.editorLine, HighlighterLayer.SELECTION - 2, null)
            highlighter.gutterIconRenderer = CommentIconRenderer(id)
            commentHighlighters.getOrPut(anchor.editor) { linkedMapOf() }[id] = highlighter
        }

        commentHighlighters.keys.forEach { editor ->
            editor.gutterComponentEx.revalidate()
            editor.gutterComponentEx.repaint()
        }
    }

    private fun clearCommentIndicators() {
        commentHighlighters.forEach { (editor, highlightersById) ->
            highlightersById.values.forEach(editor.markupModel::removeHighlighter)
            editor.gutterComponentEx.revalidate()
            editor.gutterComponentEx.repaint()
        }
        commentHighlighters.clear()
    }

    private fun updateHover(editor: EditorEx, y: Int) {
        val anchor = hoveredAnchor(editor, y) ?: run {
            if (hoverHighlighter?.editor == editor) {
                clearHover()
            }
            return
        }

        if (anchorHasComment(anchor.id)) {
            if (hoverHighlighter?.editor == editor) {
                clearHover()
            }
            return
        }

        if (hoverAnchorId == anchor.id) {
            return
        }

        clearHover()
        hoverAnchorId = anchor.id

        val highlighter = editor.markupModel.addLineHighlighter(anchor.editorLine, HighlighterLayer.SELECTION - 1, null)
        highlighter.gutterIconRenderer = HoverIconRenderer(anchor.id)
        hoverHighlighter = HoverHighlighter(editor, highlighter)
    }

    private fun clearHover() {
        val currentHover = hoverHighlighter
        hoverAnchorId = null
        currentHover?.highlighter?.let(currentHover.editor.markupModel::removeHighlighter)
        hoverHighlighter = null
    }

    private fun hoveredAnchor(editor: EditorEx, y: Int): DiffCommentAnchor? {
        val anchorsByLine = anchorsByEditorAndLine[editor] ?: return null
        val visualLine = editor.yToVisualLine(y)
        val line = editor.visualToLogicalPosition(VisualPosition(visualLine, 0)).line
        return anchorsByLine[line]
    }

    private fun openCommentInput(anchorId: String) {
        val anchor = anchorsById[anchorId] ?: return
        if (activePopupAnchorId == anchorId && activePopup?.isDisposed == false) {
            return
        }

        activePopup?.cancel()
        val existing = store.find(anchorId)
        activePopupAnchorId = anchorId
        activePopup = CommentInputPopup.show(
            project = project,
            editor = anchor.editor,
            line = anchor.editorLine,
            initialText = existing?.commentText,
            allowRemove = existing != null,
            onSubmit = { text ->
                store.upsert(
                    ReviewComment(
                        id = anchor.id,
                        filePath = anchor.filePath,
                        lineNumber = anchor.lineNumber,
                        lineLabel = anchor.lineLabel,
                        commentText = text,
                        lineTextPreview = linePreview(anchor),
                        viewerKind = adapter.viewerKind,
                    ),
                )
            },
            onRemove = {
                store.remove(anchor.id)
            },
        ).also { popup ->
            popup.addListener(
                object : com.intellij.openapi.ui.popup.JBPopupListener {
                    override fun onClosed(event: com.intellij.openapi.ui.popup.LightweightWindowEvent) {
                        if (activePopup === popup) {
                            activePopup = null
                            activePopupAnchorId = null
                        }
                    }
                },
            )
        }
    }

    private fun linePreview(anchor: DiffCommentAnchor): String? {
        val document = anchor.editor.document
        if (anchor.editorLine < 0 || anchor.editorLine >= document.lineCount) {
            return null
        }

        val startOffset = document.getLineStartOffset(anchor.editorLine)
        val endOffset = document.getLineEndOffset(anchor.editorLine)
        return document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset)).trim().ifEmpty { null }
    }

    private fun anchorHasComment(anchorId: String): Boolean = store.find(anchorId) != null

    private inner class HoverIconRenderer(
        private val anchorId: String,
    ) : GutterIconRenderer() {
        override fun getIcon(): Icon = AllIcons.General.Add

        override fun getAlignment(): Alignment = Alignment.RIGHT

        override fun getTooltipText(): String = "Add review comment"

        override fun equals(other: Any?): Boolean {
            return other is HoverIconRenderer && other.anchorId == anchorId
        }

        override fun hashCode(): Int = anchorId.hashCode()

        override fun getClickAction(): com.intellij.openapi.actionSystem.AnAction {
            return object : com.intellij.openapi.actionSystem.AnAction() {
                override fun actionPerformed(event: com.intellij.openapi.actionSystem.AnActionEvent) {
                    openCommentInput(anchorId)
                }
            }
        }
    }

    private inner class CommentIconRenderer(
        private val anchorId: String,
    ) : GutterIconRenderer() {
        override fun getIcon(): Icon = AllIcons.General.Balloon

        override fun getAlignment(): Alignment = Alignment.RIGHT

        override fun getTooltipText(): String = "Edit review comment"

        override fun equals(other: Any?): Boolean {
            return other is CommentIconRenderer && other.anchorId == anchorId
        }

        override fun hashCode(): Int = anchorId.hashCode()

        override fun getClickAction(): com.intellij.openapi.actionSystem.AnAction {
            return object : com.intellij.openapi.actionSystem.AnAction() {
                override fun actionPerformed(event: com.intellij.openapi.actionSystem.AnActionEvent) {
                    openCommentInput(anchorId)
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

    private data class HoverHighlighter(
        val editor: EditorEx,
        val highlighter: RangeHighlighter,
    )

    private data class EditorListeners(
        val editor: EditorEx,
        val motionListener: MouseMotionAdapter,
        val mouseListener: MouseAdapter,
    ) : Disposable {
        override fun dispose() {
            editor.gutterComponentEx.removeMouseMotionListener(motionListener)
            editor.gutterComponentEx.removeMouseListener(mouseListener)
        }
    }

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
