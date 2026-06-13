package com.myrpasko.commentsplugin.diff

import com.intellij.diff.FrameDiffTool
import com.intellij.diff.fragments.LineFragment
import com.intellij.diff.tools.simple.SimpleDiffChange
import com.intellij.diff.tools.simple.SimpleDiffViewer
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer
import com.intellij.diff.util.Side
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.myrpasko.commentsplugin.util.ProjectPathResolver
import java.lang.reflect.Method
import javax.swing.JComponent

interface DiffViewerAdapter {
    val viewer: FrameDiffTool.DiffViewer
    val filePath: String
    val primaryComponent: JComponent
    val viewerKind: String

    fun collectCommentAnchors(): Set<DiffCommentAnchor>
}

enum class DiffViewerSupportKind {
    SIMPLE,
    REFLECTIVE_TWOSIDE,
    UNSUPPORTED,
}

object DiffViewerAdapterClassifier {
    fun classify(
        viewerClassName: String,
        isSimpleViewer: Boolean,
        isTwosideTextViewer: Boolean,
        hasDiffChangesAccessor: Boolean,
    ): DiffViewerSupportKind {
        if (viewerClassName.contains("UnifiedDiffViewer")) {
            return DiffViewerSupportKind.UNSUPPORTED
        }
        if (isSimpleViewer) {
            return DiffViewerSupportKind.SIMPLE
        }
        if (isTwosideTextViewer && hasDiffChangesAccessor) {
            return DiffViewerSupportKind.REFLECTIVE_TWOSIDE
        }
        return DiffViewerSupportKind.UNSUPPORTED
    }
}

object DiffViewerAdapters {
    private val log = Logger.getInstance(DiffViewerAdapters::class.java)

    fun create(project: Project, viewer: FrameDiffTool.DiffViewer): DiffViewerAdapter? {
        if (viewer !is TwosideTextDiffViewer) {
            return null
        }

        val filePath = ProjectPathResolver.resolveRelativePath(project, viewer.getContent(Side.RIGHT)) ?: return null
        val hasDiffChangesAccessor = viewer.javaClass.methods.any { method ->
            method.name == "getDiffChanges" && method.parameterCount == 0
        }
        val supportKind = DiffViewerAdapterClassifier.classify(
            viewerClassName = viewer.javaClass.name,
            isSimpleViewer = viewer is SimpleDiffViewer,
            isTwosideTextViewer = true,
            hasDiffChangesAccessor = hasDiffChangesAccessor,
        )

        return when (supportKind) {
            DiffViewerSupportKind.SIMPLE -> createSimpleAdapter(viewer as SimpleDiffViewer, filePath)
            DiffViewerSupportKind.REFLECTIVE_TWOSIDE -> createReflectiveAdapter(viewer, filePath)
            DiffViewerSupportKind.UNSUPPORTED -> {
                log.warn("Unsupported local side-by-side diff viewer: ${viewer.javaClass.name}")
                null
            }
        }
    }

    private fun createSimpleAdapter(viewer: SimpleDiffViewer, filePath: String): DiffViewerAdapter {
        val leftEditor = viewer.getEditor(Side.LEFT)
        val rightEditor = viewer.getEditor(Side.RIGHT)
        return AdapterImpl(
            viewer = viewer,
            filePath = filePath,
            primaryComponent = viewer.component,
            viewerKind = viewer.javaClass.name,
            anchorsProvider = {
                buildAllRightSideAnchors(filePath, rightEditor) +
                    viewer.getDiffChanges()
                        .asSequence()
                        .filter(SimpleDiffChange::isValid)
                        .flatMap { change ->
                            buildDeletedAnchors(
                                filePath = filePath,
                                leftEditor = leftEditor,
                                fragment = change.fragment,
                            ).asSequence()
                        }
                        .toSet()
            },
        )
    }

    private fun createReflectiveAdapter(
        viewer: TwosideTextDiffViewer,
        filePath: String,
    ): DiffViewerAdapter? {
        val leftEditor = viewer.getEditor(Side.LEFT)
        val rightEditor = viewer.getEditor(Side.RIGHT)
        val diffChangesMethod = findMethod(viewer.javaClass, "getDiffChanges") ?: return null

        return AdapterImpl(
            viewer = viewer,
            filePath = filePath,
            primaryComponent = viewer.component,
            viewerKind = viewer.javaClass.name,
            anchorsProvider = {
                buildAllRightSideAnchors(filePath, rightEditor) +
                    collectReflectiveDeletedAnchors(
                        viewer = viewer,
                        diffChangesMethod = diffChangesMethod,
                        filePath = filePath,
                        viewerKind = viewer.javaClass.name,
                        leftEditor = leftEditor,
                    )
            },
        )
    }

    private fun collectReflectiveDeletedAnchors(
        viewer: TwosideTextDiffViewer,
        diffChangesMethod: Method,
        filePath: String,
        viewerKind: String,
        leftEditor: EditorEx,
    ): Set<DiffCommentAnchor> {
        val changeList = diffChangesMethod.invoke(viewer) as? Iterable<*> ?: return emptySet()
        val firstChange = changeList.firstOrNull() ?: return emptySet()
        val accessors = ReflectiveChangeAccessors.from(firstChange) ?: return emptySet()
        return changeList
            .asSequence()
            .filterNotNull()
            .filter(accessors::isValid)
            .flatMap { change ->
                DiffCommentAnchorPlanner.planSideBySide(
                    oldStart = accessors.getStartLine(change, Side.LEFT),
                    oldEnd = accessors.getEndLine(change, Side.LEFT),
                    newStart = accessors.getStartLine(change, Side.RIGHT),
                    newEnd = accessors.getEndLine(change, Side.RIGHT),
                )
                    .asSequence()
                    .filter { plan -> plan.side == DiffEditorSide.LEFT }
                    .map { plan ->
                        plan.toAnchor(
                            filePath = filePath,
                            leftEditor = leftEditor,
                            rightEditor = leftEditor,
                        )
                    }
            }
            .toSet()
    }

    private fun buildAllRightSideAnchors(
        filePath: String,
        rightEditor: EditorEx,
    ): Set<DiffCommentAnchor> {
        val lineCount = rightEditor.document.lineCount
        return (0 until lineCount)
            .map { line ->
                DiffCommentAnchor(
                    id = buildAnchorId(filePath, DiffEditorSide.RIGHT, line + 1, line),
                    editor = rightEditor,
                    editorLine = line,
                    filePath = filePath,
                    lineNumber = line + 1,
                )
            }
            .toSet()
    }

    private fun buildDeletedAnchors(
        filePath: String,
        leftEditor: EditorEx,
        fragment: LineFragment,
    ): Set<DiffCommentAnchor> {
        return DiffCommentAnchorPlanner.planSideBySide(
            oldStart = fragment.startLine1,
            oldEnd = fragment.endLine1,
            newStart = fragment.startLine2,
            newEnd = fragment.endLine2,
        )
            .asSequence()
            .filter { plan -> plan.side == DiffEditorSide.LEFT }
            .map { plan ->
                plan.toAnchor(
                    filePath = filePath,
                    leftEditor = leftEditor,
                    rightEditor = leftEditor,
                )
            }
            .toSet()
    }

    private fun DiffCommentAnchorPlan.toAnchor(
        filePath: String,
        leftEditor: EditorEx,
        rightEditor: EditorEx,
    ): DiffCommentAnchor {
        val editor = when (side) {
            DiffEditorSide.LEFT -> leftEditor
            DiffEditorSide.RIGHT -> rightEditor
        }
        return DiffCommentAnchor(
            id = buildAnchorId(filePath, side, lineNumber, editorLine),
            editor = editor,
            editorLine = editorLine,
            filePath = filePath,
            lineNumber = lineNumber,
            lineLabel = lineLabel,
        )
    }

    private fun buildAnchorId(
        filePath: String,
        side: DiffEditorSide,
        lineNumber: Int,
        editorLine: Int,
    ): String {
        return "$filePath:${side.name}:$lineNumber:$editorLine"
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

    private data class AdapterImpl(
        override val viewer: FrameDiffTool.DiffViewer,
        override val filePath: String,
        override val primaryComponent: JComponent,
        override val viewerKind: String,
        private val anchorsProvider: () -> Set<DiffCommentAnchor>,
    ) : DiffViewerAdapter {
        override fun collectCommentAnchors(): Set<DiffCommentAnchor> = anchorsProvider()
    }

    private data class ReflectiveChangeAccessors(
        private val isValidMethod: Method?,
        private val getStartLineMethod: Method,
        private val getEndLineMethod: Method,
    ) {
        fun isValid(change: Any): Boolean {
            return (isValidMethod?.invoke(change) as? Boolean) ?: true
        }

        fun getStartLine(change: Any, side: Side): Int {
            return getStartLineMethod.invoke(change, side) as Int
        }

        fun getEndLine(change: Any, side: Side): Int {
            return getEndLineMethod.invoke(change, side) as Int
        }

        companion object {
            fun from(change: Any?): ReflectiveChangeAccessors? {
                if (change == null) {
                    return null
                }

                val type = change.javaClass
                val start = findSideMethod(type, "getStartLine") ?: return null
                val end = findSideMethod(type, "getEndLine") ?: return null
                val isValid = type.methods.firstOrNull { method ->
                    method.name == "isValid" && method.parameterCount == 0
                }
                isValid?.isAccessible = true

                return ReflectiveChangeAccessors(
                    isValidMethod = isValid,
                    getStartLineMethod = start,
                    getEndLineMethod = end,
                )
            }

            private fun findSideMethod(type: Class<*>, name: String): Method? {
                return type.methods.firstOrNull { method ->
                    method.name == name &&
                        method.parameterCount == 1 &&
                        method.parameterTypes.singleOrNull() == Side::class.java
                }?.apply {
                    isAccessible = true
                }
            }
        }
    }
}
