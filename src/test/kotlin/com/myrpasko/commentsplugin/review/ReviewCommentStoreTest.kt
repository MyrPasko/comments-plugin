package com.myrpasko.commentsplugin.review

import com.myrpasko.commentsplugin.model.CommentLocation
import com.myrpasko.commentsplugin.model.ReviewComment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReviewCommentStoreTest {
    @Test
    fun `upsert stores and replaces comments by location id`() {
        val store = ReviewCommentStore()
        val location = CommentLocation("src/App.kt", 12)

        store.upsert(ReviewComment(location.id(), location.filePath, location.lineNumber, "First"))
        store.upsert(ReviewComment(location.id(), location.filePath, location.lineNumber, "Updated"))

        assertEquals(1, store.getAll().size)
        assertEquals("Updated", store.find(location)?.commentText)
    }

    @Test
    fun `discard all clears comments`() {
        val store = ReviewCommentStore()

        store.upsert(ReviewComment("a", "src/A.kt", 1, "One"))
        store.upsert(ReviewComment("b", "src/B.kt", 2, "Two"))
        store.discardAll()

        assertEquals(emptyList(), store.getAll())
    }

    @Test
    fun `remove deletes specific location`() {
        val store = ReviewCommentStore()
        val location = CommentLocation("src/App.kt", 7)

        store.upsert(ReviewComment(location.id(), location.filePath, location.lineNumber, "Remove me"))
        store.remove(location)

        assertNull(store.find(location))
    }
}

