package com.myrpasko.commentsplugin.diff

import com.intellij.diff.tools.fragmented.UnifiedDiffChange
import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.actions.diff.lst.ChangeListDiffViewerDecorator
import com.intellij.openapi.vcs.changes.actions.diff.lst.LocalTrackerDiffUtil

class LocalChangeListReviewDecorator : ChangeListDiffViewerDecorator {
    override fun initialize(viewer: TwosideTextDiffViewer) {
        val project = viewer.project ?: return
        SideBySideDiffReviewController.attach(project, viewer)
    }

    override fun decorateFragments(
        toggleableLineRanges: List<LocalTrackerDiffUtil.ToggleableLineRange>,
        viewer: TwosideTextDiffViewer,
    ) {
        // No-op for now. Gutter interaction is handled by the side-by-side controller.
    }

    override fun isAvailable(viewer: TwosideTextDiffViewer): Boolean {
        return viewer.javaClass.name == LOCAL_CHANGE_LIST_VIEWER
    }

    override fun initialize(viewer: UnifiedDiffViewer) = Unit

    override fun decorateFragments(
        changes: List<UnifiedDiffChange>,
        viewer: UnifiedDiffViewer,
    ) = Unit

    override fun isAvailable(viewer: UnifiedDiffViewer): Boolean = false

    private companion object {
        const val LOCAL_CHANGE_LIST_VIEWER =
            "com.intellij.openapi.vcs.changes.actions.diff.lst.SimpleLocalChangeListDiffViewer"
    }
}
