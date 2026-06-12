package com.myrpasko.commentsplugin.diff

data class DiffActionSurfacePlan(
    val showBottomPanel: Boolean,
    val showToolbarFallback: Boolean,
)

object DiffActionSurfacePlanner {
    fun plan(bottomInjectionSucceeded: Boolean): DiffActionSurfacePlan {
        return if (bottomInjectionSucceeded) {
            DiffActionSurfacePlan(
                showBottomPanel = true,
                showToolbarFallback = false,
            )
        } else {
            DiffActionSurfacePlan(
                showBottomPanel = false,
                showToolbarFallback = true,
            )
        }
    }
}
