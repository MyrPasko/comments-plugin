package com.myrpasko.commentsplugin.diff

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DoNotAskOption
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
import javax.swing.UIManager

class DiffActionPanel(
    project: Project,
    private val store: ReviewCommentStore,
) : JBPanel<DiffActionPanel>(FlowLayout(FlowLayout.RIGHT, 8, 6)), ReviewCommentStore.Listener, Disposable {
    private val countLabel = JBLabel()
    private val panelFont = UIManager.getFont("Label.font")

    init {
        isOpaque = false
        countLabel.font = panelFont
        add(countLabel)
        add(
            JButton("Discard").apply {
                font = panelFont
                addActionListener {
                    store.discardAll()
                }
            },
        )
        add(
            JButton("Submit").apply {
                font = panelFont
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
                store.discardAll()
                showSuccessConfirmation(project, settings)
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

    private fun showSuccessConfirmation(project: Project, settings: PromptSettingsService) {
        if (!settings.showSuccessPopup) {
            return
        }

        Messages.showDialog(
            project,
            "Prompt inserted into the active terminal session. Review comments were cleared.",
            "Comments Plugin",
            arrayOf(Messages.getOkButton()),
            0,
            Messages.getInformationIcon(),
            object : DoNotAskOption {
                override fun isToBeShown(): Boolean = settings.showSuccessPopup

                override fun setToBeShown(value: Boolean, exitCode: Int) {
                    if (exitCode == Messages.OK) {
                        settings.showSuccessPopup = value
                    }
                }

                override fun canBeHidden(): Boolean = true

                override fun shouldSaveOptionsOnCancel(): Boolean = false

                override fun getDoNotShowMessage(): String = "Never show this info again"
            },
        )
    }
}
