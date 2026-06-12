package com.myrpasko.commentsplugin.diff

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.myrpasko.commentsplugin.prompt.PromptBuildResult
import com.myrpasko.commentsplugin.prompt.PromptBuilder
import com.myrpasko.commentsplugin.review.ReviewCommentStore
import com.myrpasko.commentsplugin.settings.PromptSettingsService
import com.myrpasko.commentsplugin.terminal.TerminalInsertionResult
import com.myrpasko.commentsplugin.terminal.TerminalPromptInserter
import java.awt.FlowLayout
import javax.swing.JButton

class DiffActionPanel(
    project: Project,
    private val store: ReviewCommentStore,
) : JBPanel<DiffActionPanel>(FlowLayout(FlowLayout.RIGHT, 8, 6)), ReviewCommentStore.Listener, Disposable {
    private val countLabel = JBLabel()

    init {
        add(countLabel)
        add(
            JButton("Discard").apply {
                addActionListener {
                    store.discardAll()
                }
            },
        )
        add(
            JButton("Submit").apply {
                addActionListener {
                    submit(project)
                }
            },
        )
        store.addListener(this)
        updateCount()
    }

    override fun commentsChanged() {
        updateCount()
    }

    override fun dispose() {
        store.removeListener(this)
    }

    private fun updateCount() {
        val count = store.getAll().size
        countLabel.text = if (count == 1) "1 comment" else "$count comments"
    }

    private fun submit(project: Project) {
        val settings = ApplicationManager.getApplication().getService(PromptSettingsService::class.java)
        val prompt = when (val buildResult = PromptBuilder.build(settings.promptPrefix, store.getAll())) {
            PromptBuildResult.Empty -> {
                Messages.showInfoMessage(project, "There are no comments to submit.", "Comments Plugin")
                return
            }

            is PromptBuildResult.Prompt -> buildResult.text
        }

        when (val insertionResult = TerminalPromptInserter.insert(project, prompt)) {
            TerminalInsertionResult.Inserted -> {
                Messages.showInfoMessage(
                    project,
                    "Prompt inserted into the active terminal session.",
                    "Comments Plugin",
                )
            }

            is TerminalInsertionResult.CopiedToClipboard -> {
                Messages.showWarningDialog(
                    project,
                    insertionResult.reason,
                    "Comments Plugin",
                )
            }

            is TerminalInsertionResult.Failed -> {
                Messages.showWarningDialog(
                    project,
                    insertionResult.reason,
                    "Comments Plugin",
                )
            }
        }
    }
}
