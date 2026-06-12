package com.myrpasko.commentsplugin.diff

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel

class CommentDialog(
    project: Project,
    initialText: String?,
    private val allowRemove: Boolean,
) : DialogWrapper(project, true) {
    sealed interface Result {
        data class Comment(val text: String) : Result
        data object Remove : Result
        data object Cancel : Result
    }

    private val textArea = JBTextArea(6, 48)
    private var result: Result = Result.Cancel

    init {
        title = "Review Comment"
        setOKButtonText("Comment")
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        textArea.text = initialText.orEmpty()
        init()
    }

    override fun createCenterPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            preferredSize = Dimension(420, 180)
            add(JBScrollPane(textArea), BorderLayout.CENTER)
        }
    }

    override fun createActions(): Array<Action> {
        val actions = mutableListOf<Action>()
        if (allowRemove) {
            actions += object : DialogWrapperAction("Remove") {
                override fun doAction(event: java.awt.event.ActionEvent?) {
                    result = Result.Remove
                    close(REMOVE_EXIT_CODE)
                }
            }
        }
        actions += okAction
        actions += cancelAction
        return actions.toTypedArray()
    }

    override fun doValidate(): ValidationInfo? {
        return if (textArea.text.trim().isEmpty()) {
            ValidationInfo("Comment text cannot be blank.", textArea)
        } else {
            null
        }
    }

    override fun doOKAction() {
        result = Result.Comment(textArea.text.trim())
        super.doOKAction()
    }

    override fun doCancelAction() {
        result = Result.Cancel
        super.doCancelAction()
    }

    fun showDialog(): Result {
        show()
        return result
    }

    companion object {
        private const val REMOVE_EXIT_CODE = NEXT_USER_EXIT_CODE
    }
}

