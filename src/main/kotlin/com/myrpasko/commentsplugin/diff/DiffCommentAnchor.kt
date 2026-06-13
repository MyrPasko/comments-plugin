package com.myrpasko.commentsplugin.diff

import com.intellij.openapi.editor.ex.EditorEx

data class DiffCommentAnchor(
    val id: String,
    val editor: EditorEx,
    val editorLine: Int,
    val filePath: String,
    val lineNumber: Int,
    val lineLabel: String = lineNumber.toString(),
)

enum class DiffEditorSide {
    LEFT,
    RIGHT,
}

data class DiffCommentAnchorPlan(
    val side: DiffEditorSide,
    val editorLine: Int,
    val lineNumber: Int,
    val lineLabel: String = lineNumber.toString(),
)

object DiffCommentAnchorPlanner {
    fun planSideBySide(
        oldStart: Int,
        oldEnd: Int,
        newStart: Int,
        newEnd: Int,
    ): List<DiffCommentAnchorPlan> {
        val plans = mutableListOf<DiffCommentAnchorPlan>()

        for (line in oldStart until oldEnd) {
            val lineNumber = line + 1
            plans += DiffCommentAnchorPlan(
                side = DiffEditorSide.LEFT,
                editorLine = line,
                lineNumber = lineNumber,
                lineLabel = "$lineNumber deleted",
            )
        }

        for (line in newStart until newEnd) {
            val lineNumber = line + 1
            plans += DiffCommentAnchorPlan(
                side = DiffEditorSide.RIGHT,
                editorLine = line,
                lineNumber = lineNumber,
            )
        }

        return plans
    }
}
