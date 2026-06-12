package com.myrpasko.commentsplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class PromptSettingsConfigurable : Configurable {
    private val service: PromptSettingsService
        get() = ApplicationManager.getApplication().getService(PromptSettingsService::class.java)

    private var promptArea: JBTextArea? = null

    override fun getDisplayName(): String = "Comments Plugin"

    override fun createComponent(): JComponent {
        val area = JBTextArea(6, 48)
        area.lineWrap = true
        area.wrapStyleWord = true
        promptArea = area

        return JPanel(BorderLayout()).apply {
            add(JBScrollPane(area), BorderLayout.CENTER)
        }
    }

    override fun isModified(): Boolean {
        val current = promptArea?.text ?: return false
        return current != service.promptPrefix
    }

    override fun apply() {
        service.promptPrefix = promptArea?.text ?: PromptSettingsService.DEFAULT_PROMPT_PREFIX
    }

    override fun reset() {
        promptArea?.text = service.promptPrefix
    }

    override fun disposeUIResources() {
        promptArea = null
    }
}

