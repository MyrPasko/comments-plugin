package com.myrpasko.commentsplugin.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class PromptSettingsServiceTest {
    @Test
    fun `default prefix is set`() {
        val service = PromptSettingsService()

        assertEquals(PromptSettingsService.DEFAULT_PROMPT_PREFIX, service.promptPrefix)
    }

    @Test
    fun `custom prefix is stored`() {
        val service = PromptSettingsService()

        service.promptPrefix = "Review these:"

        assertEquals("Review these:", service.promptPrefix)
    }

    @Test
    fun `blank prefix resets to default`() {
        val service = PromptSettingsService()

        service.promptPrefix = "   "

        assertEquals(PromptSettingsService.DEFAULT_PROMPT_PREFIX, service.promptPrefix)
    }
}
