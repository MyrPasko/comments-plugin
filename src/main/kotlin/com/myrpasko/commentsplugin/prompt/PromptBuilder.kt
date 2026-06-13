package com.myrpasko.commentsplugin.prompt

import com.myrpasko.commentsplugin.model.ReviewComment
import com.myrpasko.commentsplugin.settings.PromptSettingsService

sealed interface PromptBuildResult {
    data object Empty : PromptBuildResult
    data class Prompt(val text: String) : PromptBuildResult
}

object PromptBuilder {
    fun build(prefix: String, comments: Collection<ReviewComment>): PromptBuildResult {
        val normalizedComments = comments
            .mapNotNull { comment ->
                val trimmed = comment.commentText.trim()
                if (trimmed.isEmpty()) {
                    null
                } else {
                    comment.copy(commentText = trimmed)
                }
            }
            .sortedWith(compareBy<ReviewComment>({ it.filePath }, { it.lineNumber }, { it.id }))

        if (normalizedComments.isEmpty()) {
            return PromptBuildResult.Empty
        }

        val normalizedPrefix = prefix.trim().ifBlank { PromptSettingsService.DEFAULT_PROMPT_PREFIX }
        val body = normalizedComments.joinToString(separator = "\n") { comment ->
            "[${comment.filePath}:${comment.lineLabel}] - ${comment.commentText}"
        }

        return PromptBuildResult.Prompt(
            buildString {
                append(normalizedPrefix)
                append("\n\n")
                append(body)
            },
        )
    }
}
