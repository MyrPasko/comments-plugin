package com.myrpasko.commentsplugin.diff

import java.awt.Font
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommentInputPopupTest {
    @Test
    fun `submit shortcut uses command enter on mac`() {
        val keyStroke = CommentInputPopup.submitShortcutKeyStroke(isMac = true)

        assertEquals(KeyEvent.VK_ENTER, keyStroke.keyCode)
        assertTrue(keyStroke.modifiers and InputEvent.META_DOWN_MASK != 0)
    }

    @Test
    fun `submit shortcut uses control enter off mac`() {
        val keyStroke = CommentInputPopup.submitShortcutKeyStroke(isMac = false)

        assertEquals(KeyEvent.VK_ENTER, keyStroke.keyCode)
        assertTrue(keyStroke.modifiers and InputEvent.CTRL_DOWN_MASK != 0)
    }

    @Test
    fun `shortcut hint reflects comment action on mac`() {
        assertEquals(
            "Press Cmd+Enter to comment.",
            CommentInputPopup.submitShortcutHintText(submitLabel = "Comment", isMac = true),
        )
    }

    @Test
    fun `shortcut hint reflects update action off mac`() {
        assertEquals(
            "Press Ctrl+Enter to update.",
            CommentInputPopup.submitShortcutHintText(submitLabel = "Update", isMac = false),
        )
    }
}

class DiffActionPanelTest {
    @Test
    fun `compact panel font shrinks by one point`() {
        val compact = DiffActionPanel.compactPanelFont(Font("Dialog", Font.PLAIN, 14))

        assertEquals(13f, compact.size2D)
    }

    @Test
    fun `compact panel font keeps minimum readable size`() {
        val compact = DiffActionPanel.compactPanelFont(Font("Dialog", Font.PLAIN, 11))

        assertEquals(11f, compact.size2D)
    }
}
