package com.myrpasko.commentsplugin.diff

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
import java.awt.Point
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.UIManager

object CommentInputPopup {
    internal const val SUBMIT_SHORTCUT_ACTION_KEY = "commentsPlugin.submitFromShortcut"

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
        val submitLabel = if (allowRemove) "Update" else "Comment"

        val popupRef = arrayOfNulls<JBPopup>(1)
        val submitAction = submit@{
            val text = textArea.text.trim()
            if (text.isEmpty()) {
                return@submit
            }
            onSubmit(text)
            popupRef[0]?.cancel()
        }
        val content = createContent(
            textArea = textArea,
            allowRemove = allowRemove,
            submitLabel = submitLabel,
            onCancel = { popupRef[0]?.cancel() },
            onSubmit = submitAction,
            onRemove = {
                onRemove()
                popupRef[0]?.cancel()
            },
        )
        bindSubmitShortcut(content, submitAction)

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
        submitLabel: String,
        onCancel: () -> Unit,
        onSubmit: () -> Unit,
        onRemove: () -> Unit,
    ): JComponent {
        val hintLabel = JBLabel(submitShortcutHintText(submitLabel)).apply {
            font = font.deriveFont((font.size2D - 1f).coerceAtLeast(11f))
            foreground = shortcutHintColor()
            border = JBUI.Borders.empty(2, 0, 0, 0)
        }
        val actions = JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
            isOpaque = false
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
                JButton(submitLabel).apply {
                    addActionListener { onSubmit() }
                },
            )
        }
        val footer = JPanel(BorderLayout(12, 0)).apply {
            isOpaque = false
            add(hintLabel, BorderLayout.WEST)
            add(actions, BorderLayout.EAST)
        }

        return JPanel(BorderLayout(0, 8)).apply {
            border = JBUI.Borders.compound(
                JBUI.Borders.customLine(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
                JBUI.Borders.empty(10),
            )
            preferredSize = Dimension(420, 170)
            add(JBScrollPane(textArea), BorderLayout.CENTER)
            add(footer, BorderLayout.SOUTH)
        }
    }

    private fun bindSubmitShortcut(
        component: JComponent,
        onSubmit: () -> Unit,
    ) {
        val keyStroke = submitShortcutKeyStroke()
        component.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(keyStroke, SUBMIT_SHORTCUT_ACTION_KEY)
        component.actionMap.put(
            SUBMIT_SHORTCUT_ACTION_KEY,
            object : AbstractAction() {
                override fun actionPerformed(event: ActionEvent?) {
                    onSubmit()
                }
            },
        )
    }

    internal fun submitShortcutKeyStroke(isMac: Boolean = SystemInfo.isMac): KeyStroke {
        val modifiers = if (isMac) InputEvent.META_DOWN_MASK else InputEvent.CTRL_DOWN_MASK
        return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, modifiers)
    }

    internal fun submitShortcutHintText(
        submitLabel: String,
        isMac: Boolean = SystemInfo.isMac,
    ): String {
        val modifier = if (isMac) "Cmd" else "Ctrl"
        return "Press $modifier+Enter to ${submitLabel.lowercase()}."
    }

    private fun shortcutHintColor(): Color {
        return UIManager.getColor("Label.disabledForeground")
            ?: JBUI.CurrentTheme.ContextHelp.FOREGROUND
    }
}
