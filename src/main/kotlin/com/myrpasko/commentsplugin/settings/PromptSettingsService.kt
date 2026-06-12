package com.myrpasko.commentsplugin.settings

import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service
@State(
    name = "CommentsPluginPromptSettings",
    storages = [Storage(value = "comments-plugin.xml", roamingType = RoamingType.DISABLED)],
)
class PromptSettingsService : SerializablePersistentStateComponent<PromptSettingsService.State>(State()) {
    data class State(
        @JvmField val promptPrefix: String = DEFAULT_PROMPT_PREFIX,
    )

    var promptPrefix: String
        get() = state.promptPrefix
        set(value) {
            updateState { current ->
                current.copy(promptPrefix = value.ifBlank { DEFAULT_PROMPT_PREFIX })
            }
        }

    companion object {
        const val DEFAULT_PROMPT_PREFIX = "Fix these comments:"
    }
}

