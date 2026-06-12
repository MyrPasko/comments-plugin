package com.myrpasko.commentsplugin.diff

import kotlin.test.Test
import kotlin.test.assertEquals

class DiffViewerAdapterClassifierTest {
    @Test
    fun `selects simple adapter for simple diff viewer`() {
        assertEquals(
            DiffViewerSupportKind.SIMPLE,
            DiffViewerAdapterClassifier.classify(
                viewerClassName = "com.intellij.diff.tools.simple.SimpleDiffViewer",
                isSimpleViewer = true,
                isTwosideTextViewer = true,
                hasDiffChangesAccessor = true,
            ),
        )
    }

    @Test
    fun `selects reflective adapter for supported non-simple twoside viewer`() {
        assertEquals(
            DiffViewerSupportKind.REFLECTIVE_TWOSIDE,
            DiffViewerAdapterClassifier.classify(
                viewerClassName = "com.jetbrains.python.diff.CommitChangesViewer",
                isSimpleViewer = false,
                isTwosideTextViewer = true,
                hasDiffChangesAccessor = true,
            ),
        )
    }

    @Test
    fun `rejects unified diff viewers`() {
        assertEquals(
            DiffViewerSupportKind.UNSUPPORTED,
            DiffViewerAdapterClassifier.classify(
                viewerClassName = "com.intellij.diff.tools.fragmented.UnifiedDiffViewer",
                isSimpleViewer = false,
                isTwosideTextViewer = false,
                hasDiffChangesAccessor = false,
            ),
        )
    }

    @Test
    fun `rejects twoside viewers without changed ranges access`() {
        assertEquals(
            DiffViewerSupportKind.UNSUPPORTED,
            DiffViewerAdapterClassifier.classify(
                viewerClassName = "com.jetbrains.python.diff.SomeUnsupportedViewer",
                isSimpleViewer = false,
                isTwosideTextViewer = true,
                hasDiffChangesAccessor = false,
            ),
        )
    }
}
