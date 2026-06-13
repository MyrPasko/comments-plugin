package com.myrpasko.commentsplugin.diff

import kotlin.test.Test
import kotlin.test.assertEquals

class DiffCommentAnchorPlannerTest {
    @Test
    fun `plans right side anchors for visible current lines`() {
        assertEquals(
            listOf(
                DiffCommentAnchorPlan(side = DiffEditorSide.RIGHT, editorLine = 4, lineNumber = 5),
                DiffCommentAnchorPlan(side = DiffEditorSide.RIGHT, editorLine = 5, lineNumber = 6),
            ),
            DiffCommentAnchorPlanner.planSideBySide(
                oldStart = 4,
                oldEnd = 4,
                newStart = 4,
                newEnd = 6,
            ).filter { it.side == DiffEditorSide.RIGHT },
        )
    }

    @Test
    fun `plans deleted line anchors on the left side`() {
        assertEquals(
            listOf(
                DiffCommentAnchorPlan(side = DiffEditorSide.LEFT, editorLine = 2, lineNumber = 3, lineLabel = "3 deleted"),
                DiffCommentAnchorPlan(side = DiffEditorSide.LEFT, editorLine = 3, lineNumber = 4, lineLabel = "4 deleted"),
            ),
            DiffCommentAnchorPlanner.planSideBySide(
                oldStart = 2,
                oldEnd = 4,
                newStart = 2,
                newEnd = 2,
            ).filter { it.side == DiffEditorSide.LEFT },
        )
    }
}
