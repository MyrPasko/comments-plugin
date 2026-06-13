package com.myrpasko.commentsplugin.diff

import kotlin.test.Test
import kotlin.test.assertEquals

class DiffActionSurfacePlannerTest {
    @Test
    fun `uses bottom panel when injection succeeds`() {
        assertEquals(
            DiffActionSurfacePlan(
                showBottomPanel = true,
                showToolbarFallback = false,
            ),
            DiffActionSurfacePlanner.plan(bottomInjectionSucceeded = true),
        )
    }

    @Test
    fun `falls back to toolbar when bottom injection is unavailable`() {
        assertEquals(
            DiffActionSurfacePlan(
                showBottomPanel = false,
                showToolbarFallback = true,
            ),
            DiffActionSurfacePlanner.plan(bottomInjectionSucceeded = false),
        )
    }
}
