package com.myrpasko.commentsplugin.terminal

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager
import com.intellij.terminal.frontend.view.TerminalView
import com.intellij.terminal.ui.TtyConnectorAccessor
import com.intellij.terminal.ui.TerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import org.jetbrains.plugins.terminal.view.TerminalSendTextBuilder
import java.awt.datatransfer.StringSelection

sealed interface TerminalInsertionResult {
    data object Inserted : TerminalInsertionResult
    data class CopiedToClipboard(val reason: String) : TerminalInsertionResult
    data class Failed(val reason: String) : TerminalInsertionResult
}

object TerminalPromptInserter {
    fun insert(project: Project, prompt: String): TerminalInsertionResult {
        return try {
            if (writePromptToSelectedTerminal(project, prompt)) {
                TerminalInsertionResult.Inserted
            } else {
                copyToClipboard("No active compatible terminal session was found. Prompt copied to clipboard.", prompt)
            }
        } catch (error: Exception) {
            copyToClipboard(error.message ?: "Terminal insertion failed. Prompt copied to clipboard.", prompt)
        }
    }

    private fun writePromptToSelectedTerminal(project: Project, prompt: String): Boolean {
        return writePromptToCandidates(
            terminalViewBuilder = findSelectedTerminalView(project)?.let { terminalView ->
                { terminalView.createSendTextBuilder() }
            },
            ttyConnectorAccessor = findActiveTerminalWidget(project)?.ttyConnectorAccessor,
            prompt = prompt,
        )
    }

    private fun findSelectedTerminalView(project: Project): TerminalView? {
        val selectedContent = TerminalToolWindowManager.getInstance(project)
            .toolWindow
            ?.contentManager
            ?.selectedContent
            ?: return null

        return TerminalToolWindowTabsManager.getInstance(project)
            .tabs
            .firstOrNull { tab -> tab.content == selectedContent }
            ?.view
    }

    private fun findActiveTerminalWidget(project: Project): TerminalWidget? {
        val manager = TerminalToolWindowManager.getInstance(project)
        val selectedWidget = manager.toolWindow
            ?.contentManager
            ?.selectedContent
            ?.let(TerminalToolWindowManager::findWidgetByContent)
            ?: manager.toolWindow
                ?.contentManager
                ?.selectedContent
                ?.let(TerminalToolWindowManager::getWidgetByContent)
                ?.asNewWidget()

        return pickPreferred(selectedWidget, manager.terminalWidgets)
    }

    internal fun <T> pickPreferred(
        selectedValue: T?,
        fallbackValues: Collection<T>,
    ): T? {
        return selectedValue ?: fallbackValues.firstOrNull()
    }

    internal fun sendPrompt(
        builder: TerminalSendTextBuilder,
        prompt: String,
    ): Boolean {
        builder
            .useBracketedPasteMode()
            .send(prompt)
        return true
    }

    internal fun writePromptToCandidates(
        terminalViewBuilder: (() -> TerminalSendTextBuilder)?,
        ttyConnectorAccessor: TtyConnectorAccessor?,
        prompt: String,
        sendPrompt: (TerminalSendTextBuilder, String) -> Boolean = ::sendPrompt,
        writePrompt: (TtyConnectorAccessor, String) -> Boolean = ::writePrompt,
    ): Boolean {
        val sentFromTerminalView = runCatching {
            terminalViewBuilder?.let { sendPrompt(it(), prompt) } == true
        }.getOrDefault(false)
        if (sentFromTerminalView) {
            return true
        }

        val connectorAccessor = ttyConnectorAccessor ?: return false
        return writePrompt(connectorAccessor, prompt)
    }

    internal fun writePrompt(
        ttyConnectorAccessor: TtyConnectorAccessor,
        prompt: String,
    ): Boolean {
        var wrotePrompt = false
        ttyConnectorAccessor.executeWithTtyConnector { connector ->
            connector.write(prompt)
            wrotePrompt = true
        }
        return wrotePrompt
    }

    private fun copyToClipboard(reason: String, prompt: String): TerminalInsertionResult {
        return try {
            CopyPasteManager.getInstance().setContents(StringSelection(prompt))
            TerminalInsertionResult.CopiedToClipboard(reason)
        } catch (_: Exception) {
            TerminalInsertionResult.Failed(reason)
        }
    }
}
