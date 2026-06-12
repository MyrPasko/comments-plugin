package com.myrpasko.commentsplugin.terminal

import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.awt.datatransfer.StringSelection

sealed interface TerminalInsertionResult {
    data object Inserted : TerminalInsertionResult
    data class CopiedToClipboard(val reason: String) : TerminalInsertionResult
    data class Failed(val reason: String) : TerminalInsertionResult
}

object TerminalPromptInserter {
    fun insert(project: Project, prompt: String): TerminalInsertionResult {
        val widget = findActiveTerminalWidget(project)
            ?: return copyToClipboard("No active compatible terminal session was found. Prompt copied to clipboard.", prompt)

        return try {
            widget.executeWithTtyConnector { connector ->
                connector.write(prompt)
            }
            TerminalInsertionResult.Inserted
        } catch (error: Exception) {
            copyToClipboard(error.message ?: "Terminal insertion failed. Prompt copied to clipboard.", prompt)
        }
    }

    private fun findActiveTerminalWidget(project: Project): ShellTerminalWidget? {
        val manager = TerminalToolWindowManager.getInstance(project)
        val selectedWidget = manager.toolWindow
            ?.contentManager
            ?.selectedContent
            ?.let(TerminalToolWindowManager::getWidgetByContent)
            ?.let { widget -> asShellWidget(widget) }

        if (selectedWidget != null) {
            return selectedWidget
        }

        return manager.terminalWidgets
            .asSequence()
            .mapNotNull { widget -> asShellWidget(widget) }
            .firstOrNull()
    }

    private fun asShellWidget(widget: JBTerminalWidget): ShellTerminalWidget? {
        return widget as? ShellTerminalWidget
    }

    private fun asShellWidget(widget: TerminalWidget): ShellTerminalWidget? {
        return widget as? ShellTerminalWidget ?: ShellTerminalWidget.asShellJediTermWidget(widget)
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
