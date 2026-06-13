package com.myrpasko.commentsplugin.review

import com.myrpasko.commentsplugin.model.ReviewComment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReviewCommentStoreTest {
    @Test
    fun `upsert stores and replaces comments by anchor id`() {
        val store = ReviewCommentStore()
        val commentId = "src/App.kt:RIGHT:12:11"

        store.upsert(ReviewComment(id = commentId, filePath = "src/App.kt", lineNumber = 12, commentText = "First"))
        store.upsert(ReviewComment(id = commentId, filePath = "src/App.kt", lineNumber = 12, commentText = "Updated"))

        assertEquals(1, store.getAll().size)
        assertEquals("Updated", store.find(commentId)?.commentText)
    }

    @Test
    fun `discard all clears comments`() {
        val store = ReviewCommentStore()

        store.upsert(ReviewComment(id = "a", filePath = "src/A.kt", lineNumber = 1, commentText = "One"))
        store.upsert(ReviewComment(id = "b", filePath = "src/B.kt", lineNumber = 2, commentText = "Two"))
        store.discardAll()

        assertEquals(emptyList(), store.getAll())
    }

    @Test
    fun `remove deletes specific comment id`() {
        val store = ReviewCommentStore()
        val commentId = "src/App.kt:LEFT:7:6"

        store.upsert(ReviewComment(id = commentId, filePath = "src/App.kt", lineNumber = 7, commentText = "Remove me"))
        store.remove(commentId)

        assertNull(store.find(commentId))
    }
}
