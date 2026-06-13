package com.myrpasko.commentsplugin.diff

import com.intellij.diff.FrameDiffTool
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
    val rightEditor: EditorEx
    val filePath: String
    val primaryComponent: JComponent
    val viewerKind: String

    fun collectChangedLines(): Set<Int>
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
        return AdapterImpl(
            viewer = viewer,
            rightEditor = viewer.getEditor(Side.RIGHT),
            filePath = filePath,
            primaryComponent = viewer.component,
            viewerKind = viewer.javaClass.name,
            changedLinesProvider = {
                viewer.getDiffChanges()
                    .asSequence()
                    .filter(SimpleDiffChange::isValid)
                    .flatMap { change ->
                        val start = change.getStartLine(Side.RIGHT)
                        val end = change.getEndLine(Side.RIGHT)
                        (start until end).asSequence()
                    }
                    .toSet()
            },
        )
    }

    private fun createReflectiveAdapter(
        viewer: TwosideTextDiffViewer,
        filePath: String,
    ): DiffViewerAdapter? {
        val diffChangesMethod = findMethod(viewer.javaClass, "getDiffChanges") ?: return null

        return AdapterImpl(
            viewer = viewer,
            rightEditor = viewer.getEditor(Side.RIGHT),
            filePath = filePath,
            primaryComponent = viewer.component,
            viewerKind = viewer.javaClass.name,
            changedLinesProvider = {
                collectReflectiveChangedLines(viewer, diffChangesMethod)
            },
        )
    }

    private fun collectReflectiveChangedLines(
        viewer: TwosideTextDiffViewer,
        diffChangesMethod: Method,
    ): Set<Int> {
        val changeList = diffChangesMethod.invoke(viewer) as? Iterable<*> ?: return emptySet()
        val firstChange = changeList.firstOrNull() ?: return emptySet()
        val accessors = ReflectiveChangeAccessors.from(firstChange) ?: return emptySet()
        return buildReflectiveChangedLines(changeList, accessors)
    }

    private fun buildReflectiveChangedLines(
        changeList: Iterable<*>,
        accessors: ReflectiveChangeAccessors,
    ): Set<Int> {
        return changeList
            .asSequence()
            .filterNotNull()
            .filter(accessors::isValid)
            .flatMap { change ->
                val start = accessors.getStartLine(change)
                val end = accessors.getEndLine(change)
                (start until end).asSequence()
            }
            .toSet()
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
        override val rightEditor: EditorEx,
        override val filePath: String,
        override val primaryComponent: JComponent,
        override val viewerKind: String,
        private val changedLinesProvider: () -> Set<Int>,
    ) : DiffViewerAdapter {
        override fun collectChangedLines(): Set<Int> = changedLinesProvider()
    }

    private data class ReflectiveChangeAccessors(
        private val isValidMethod: Method?,
        private val getStartLineMethod: Method,
        private val getEndLineMethod: Method,
    ) {
        fun isValid(change: Any): Boolean {
            return (isValidMethod?.invoke(change) as? Boolean) ?: true
        }

        fun getStartLine(change: Any): Int {
            return getStartLineMethod.invoke(change, Side.RIGHT) as Int
        }

        fun getEndLine(change: Any): Int {
            return getEndLineMethod.invoke(change, Side.RIGHT) as Int
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
