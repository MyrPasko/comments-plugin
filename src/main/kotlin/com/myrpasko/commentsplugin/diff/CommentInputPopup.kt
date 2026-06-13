package com.myrpasko.commentsplugin.diff

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Point
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

object CommentInputPopup {
    fun show(
        project: Project,
        editor: Editor,
        line: Int,
        initialText: String?,
        allowRemove: Boolean,
        onSubmit: (String) -> Unit,
        onRemove: () -> Unit,
    ): JBPopup {
        val textArea = JBTextArea(4, 42).apply {
            lineWrap = true
            wrapStyleWord = true
            text = initialText.orEmpty()
        }

        val popupRef = arrayOfNulls<JBPopup>(1)
        val content = createContent(
            textArea = textArea,
            allowRemove = allowRemove,
            onCancel = { popupRef[0]?.cancel() },
            onSubmit = {
                val text = textArea.text.trim()
                if (text.isEmpty()) {
                    return@createContent
                }
                onSubmit(text)
                popupRef[0]?.cancel()
            },
            onRemove = {
                onRemove()
                popupRef[0]?.cancel()
            },
        )

        val popup = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(content, textArea)
            .setProject(project)
            .setRequestFocus(true)
            .setFocusable(true)
            .setResizable(false)
            .setMovable(false)
            .setCancelOnClickOutside(true)
            .setCancelKeyEnabled(true)
            .createPopup()

        popupRef[0] = popup

        val anchor = editor.logicalPositionToXY(LogicalPosition(line, 0))
        popup.show(
            RelativePoint(
                editor.contentComponent,
                Point(48, anchor.y + editor.lineHeight),
            ),
        )
        textArea.requestFocusInWindow()
        return popup
    }

    private fun createContent(
        textArea: JBTextArea,
        allowRemove: Boolean,
        onCancel: () -> Unit,
        onSubmit: () -> Unit,
        onRemove: () -> Unit,
    ): JComponent {
        val actions = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
            if (allowRemove) {
                add(
                    JButton("Remove").apply {
                        addActionListener { onRemove() }
                    },
                )
            }
            add(
                JButton("Cancel").apply {
                    addActionListener { onCancel() }
                },
            )
            add(
                JButton(if (allowRemove) "Update" else "Comment").apply {
                    addActionListener { onSubmit() }
                },
            )
        }

        return JPanel(BorderLayout(0, 8)).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
                JBUI.Borders.empty(10),
            )
            preferredSize = Dimension(420, 160)
            add(JBScrollPane(textArea), BorderLayout.CENTER)
            add(actions, BorderLayout.SOUTH)
        }
    }
}
