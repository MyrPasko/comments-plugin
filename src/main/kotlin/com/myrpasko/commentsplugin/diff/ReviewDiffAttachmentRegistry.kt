package com.myrpasko.commentsplugin.diff

import com.intellij.diff.FrameDiffTool

object ReviewDiffAttachmentRegistry {
    private const val ATTACHED_PROPERTY = "com.myrpasko.commentsplugin.reviewDiffAttached"

    fun markAttached(viewer: FrameDiffTool.DiffViewer): Boolean {
        val component = viewer.component
        if (component.getClientProperty(ATTACHED_PROPERTY) == true) {
            return false
        }
        component.putClientProperty(ATTACHED_PROPERTY, true)
        return true
    }
}
