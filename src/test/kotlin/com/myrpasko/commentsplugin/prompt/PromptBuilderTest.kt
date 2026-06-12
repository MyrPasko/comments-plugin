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
                ReviewComment("b:18", "src/hooks/useUser.ts", 18, "Handle loading state."),
                ReviewComment("a:42", "src/components/Button.tsx", 42, "Rename the variable."),
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
            listOf(ReviewComment("a:1", "src/App.kt", 1, "Use a clearer name.")),
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
            listOf(ReviewComment("a:1", "src/App.kt", 1, "   ")),
        )

        assertEquals(PromptBuildResult.Empty, result)
    }
}

