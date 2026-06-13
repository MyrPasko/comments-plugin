package com.myrpasko.commentsplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JPanel

class PromptSettingsConfigurable : Configurable {
    private val service: PromptSettingsService
        get() = ApplicationManager.getApplication().getService(PromptSettingsService::class.java)

    private var promptArea: JBTextArea? = null
    private var showSuccessPopupCheckBox: JBCheckBox? = null

    override fun getDisplayName(): String = "Comments Plugin"

    override fun createComponent(): JComponent {
        val area = JBTextArea(6, 48)
        area.lineWrap = true
        area.wrapStyleWord = true
        promptArea = area
        val showPopupCheckBox = JBCheckBox("Show success confirmation after prompt insertion")
        showSuccessPopupCheckBox = showPopupCheckBox

        return JPanel(BorderLayout()).apply {
            add(
                JPanel(BorderLayout(0, 8)).apply {
                    add(JBLabel("Prompt prefix"), BorderLayout.NORTH)
                    add(JBScrollPane(area), BorderLayout.CENTER)
                },
                BorderLayout.CENTER,
            )
            add(
                JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                    add(showPopupCheckBox)
                },
                BorderLayout.SOUTH,
            )
        }
    }

    override fun isModified(): Boolean {
        val current = promptArea?.text ?: return false
        val popupSetting = showSuccessPopupCheckBox?.isSelected ?: service.showSuccessPopup
        return current != service.promptPrefix || popupSetting != service.showSuccessPopup
    }

    override fun apply() {
        service.promptPrefix = promptArea?.text ?: PromptSettingsService.DEFAULT_PROMPT_PREFIX
        service.showSuccessPopup = showSuccessPopupCheckBox?.isSelected ?: true
    }

    override fun reset() {
        promptArea?.text = service.promptPrefix
        showSuccessPopupCheckBox?.isSelected = service.showSuccessPopup
    }

    override fun disposeUIResources() {
        promptArea = null
        showSuccessPopupCheckBox = null
    }
}
