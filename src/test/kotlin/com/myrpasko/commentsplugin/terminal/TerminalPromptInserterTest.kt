@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package com.myrpasko.commentsplugin.terminal

import com.intellij.terminal.ui.TtyConnectorAccessor
import com.jediterm.terminal.Questioner
import com.jediterm.terminal.TtyConnector
import org.jetbrains.plugins.terminal.view.TerminalSendTextBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TerminalPromptInserterTest {
    @Test
    fun `writePromptToCandidates prefers selected terminal view when it sends successfully`() {
        val builder = RecordingSendTextBuilder()
        val accessor = TtyConnectorAccessor().apply {
            ttyConnector = RecordingTtyConnector()
        }
        var widgetWriteCalls = 0

        val inserted = TerminalPromptInserter.writePromptToCandidates(
            terminalViewBuilder = { builder },
            ttyConnectorAccessor = accessor,
            prompt = "Fix these comments:",
            writePrompt = { _, _ ->
                widgetWriteCalls += 1
                true
            },
        )

        assertTrue(inserted)
        assertEquals("Fix these comments:", builder.sentText)
        assertEquals(0, widgetWriteCalls)
    }

    @Test
    fun `writePromptToCandidates falls back to tty accessor when terminal view send returns false`() {
        val accessor = TtyConnectorAccessor().apply {
            ttyConnector = RecordingTtyConnector()
        }
        var widgetWriteCalls = 0

        val inserted = TerminalPromptInserter.writePromptToCandidates(
            terminalViewBuilder = { RecordingSendTextBuilder() },
            ttyConnectorAccessor = accessor,
            prompt = "Fix these comments:",
            sendPrompt = { _, _ -> false },
            writePrompt = { resolvedAccessor, prompt ->
                widgetWriteCalls += 1
                TerminalPromptInserter.writePrompt(resolvedAccessor, prompt)
            },
        )

        assertTrue(inserted)
        assertEquals(1, widgetWriteCalls)
    }

    @Test
    fun `writePromptToCandidates falls back to tty accessor when terminal view builder throws`() {
        val connector = RecordingTtyConnector()
        val accessor = TtyConnectorAccessor().apply {
            ttyConnector = connector
        }

        val inserted = TerminalPromptInserter.writePromptToCandidates(
            terminalViewBuilder = { error("frontend terminal unavailable") },
            ttyConnectorAccessor = accessor,
            prompt = "Fix these comments:",
        )

        assertTrue(inserted)
        assertEquals(listOf("Fix these comments:"), connector.writes)
    }

    @Test
    fun `writePromptToCandidates returns false when no terminal candidates are available`() {
        val inserted = TerminalPromptInserter.writePromptToCandidates(
            terminalViewBuilder = null,
            ttyConnectorAccessor = null,
            prompt = "Fix these comments:",
        )

        assertFalse(inserted)
    }

    @Test
    fun `pickPreferred returns selected value first`() {
        assertEquals(
            "selected",
            TerminalPromptInserter.pickPreferred(
                selectedValue = "selected",
                fallbackValues = listOf("fallback"),
            ),
        )
    }

    @Test
    fun `pickPreferred falls back to first available value`() {
        assertEquals(
            "fallback",
            TerminalPromptInserter.pickPreferred(
                selectedValue = null,
                fallbackValues = listOf("fallback", "other"),
            ),
        )
    }

    @Test
    fun `writePrompt writes exact prompt through tty connector accessor`() {
        val accessor = TtyConnectorAccessor()
        val connector = RecordingTtyConnector()
        accessor.ttyConnector = connector
        val prompt = "Fix these comments:\n[src/App.tsx:8] - Rename this variable."

        val wrotePrompt = TerminalPromptInserter.writePrompt(accessor, prompt)

        assertTrue(wrotePrompt)
        assertEquals(listOf(prompt), connector.writes)
    }

    @Test
    fun `writePrompt returns false when tty connector is unavailable`() {
        val accessor = TtyConnectorAccessor()

        val wrotePrompt = TerminalPromptInserter.writePrompt(accessor, "ignored")

        assertFalse(wrotePrompt)
    }

    @Test
    fun `sendPrompt uses bracketed paste mode before sending`() {
        val builder = RecordingSendTextBuilder()

        val sent = TerminalPromptInserter.sendPrompt(builder, "Fix these comments:")

        assertTrue(sent)
        assertTrue(builder.usedBracketedPasteMode)
        assertEquals("Fix these comments:", builder.sentText)
    }

    private class RecordingTtyConnector : TtyConnector {
        val writes = mutableListOf<String>()

        override fun read(buf: CharArray?, offset: Int, length: Int): Int = -1

        override fun write(bytes: ByteArray?) = Unit

        override fun write(string: String?) {
            if (string != null) {
                writes += string
            }
        }

        override fun isConnected(): Boolean = true

        override fun waitFor(): Int = 0

        override fun ready(): Boolean = true

        override fun getName(): String = "recording"

        override fun close() = Unit

        override fun init(questioner: Questioner?): Boolean = true
    }

    private class RecordingSendTextBuilder : TerminalSendTextBuilder {
        var usedBracketedPasteMode = false
        var sentText: String? = null

        override fun shouldExecute(): TerminalSendTextBuilder = this

        override fun useBracketedPasteMode(): TerminalSendTextBuilder {
            usedBracketedPasteMode = true
            return this
        }

        override fun send(text: String) {
            sentText = text
        }
    }
}
