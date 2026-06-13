package com.myrpasko.commentsplugin.prompt

import com.myrpasko.commentsplugin.model.ReviewComment
import com.myrpasko.commentsplugin.settings.PromptSettingsService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PromptBuilderTest {
    @Test
    fun `returns empty when there are no comments`() {
        val result = PromptBuilder.build(PromptSettingsService.DEFAULT_PROMPT_PREFIX, emptyList())

        assertEquals(PromptBuildResult.Empty, result)
    }

    @Test
    fun `formats multiple comments across files in stable order`() {
        val result = PromptBuilder.build(
            "Fix these comments:",
            listOf(
                ReviewComment(id = "b:18", filePath = "src/hooks/useUser.ts", lineNumber = 18, commentText = "Handle loading state."),
                ReviewComment(id = "a:42", filePath = "src/components/Button.tsx", lineNumber = 42, commentText = "Rename the variable."),
            ),
        )

        val prompt = assertIs<PromptBuildResult.Prompt>(result)
        assertEquals(
            """
            Fix these comments:

            [src/components/Button.tsx:42] - Rename the variable.
            [src/hooks/useUser.ts:18] - Handle loading state.
            """.trimIndent(),
            prompt.text,
        )
    }

    @Test
    fun `falls back to default prefix when provided prefix is blank`() {
        val result = PromptBuilder.build(
            "   ",
            listOf(ReviewComment(id = "a:1", filePath = "src/App.kt", lineNumber = 1, commentText = "Use a clearer name.")),
        )

        val prompt = assertIs<PromptBuildResult.Prompt>(result)
        assertEquals(
            """
            ${PromptSettingsService.DEFAULT_PROMPT_PREFIX}

            [src/App.kt:1] - Use a clearer name.
            """.trimIndent(),
            prompt.text,
        )
    }

    @Test
    fun `ignores whitespace only comments`() {
        val result = PromptBuilder.build(
            "Fix these comments:",
            listOf(ReviewComment(id = "a:1", filePath = "src/App.kt", lineNumber = 1, commentText = "   ")),
        )

        assertEquals(PromptBuildResult.Empty, result)
    }

    @Test
    fun `formats deleted line comments with explicit label`() {
        val result = PromptBuilder.build(
            "Fix these comments:",
            listOf(
                ReviewComment(
                    id = "src/App.kt:LEFT:4:3",
                    filePath = "src/App.kt",
                    lineNumber = 4,
                    lineLabel = "4 deleted",
                    commentText = "This removal breaks the retry path.",
                ),
            ),
        )

        val prompt = assertIs<PromptBuildResult.Prompt>(result)
        assertEquals(
            """
            Fix these comments:

            [src/App.kt:4 deleted] - This removal breaks the retry path.
            """.trimIndent(),
            prompt.text,
        )
    }
}
