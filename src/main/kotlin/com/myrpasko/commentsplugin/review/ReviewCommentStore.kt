package com.myrpasko.commentsplugin.review

import com.intellij.openapi.components.Service
import com.intellij.util.EventDispatcher
import com.myrpasko.commentsplugin.model.ReviewComment
import java.util.EventListener

@Service(Service.Level.PROJECT)
class ReviewCommentStore {
    interface Listener : EventListener {
        fun commentsChanged()
    }

    private val dispatcher = EventDispatcher.create(Listener::class.java)
    private val commentsById = linkedMapOf<String, ReviewComment>()

    fun addListener(listener: Listener) {
        dispatcher.addListener(listener)
    }

    fun removeListener(listener: Listener) {
        dispatcher.removeListener(listener)
    }

    fun upsert(comment: ReviewComment) {
        commentsById[comment.id] = comment
        dispatcher.multicaster.commentsChanged()
    }

    fun remove(commentId: String) {
        if (commentsById.remove(commentId) != null) {
            dispatcher.multicaster.commentsChanged()
        }
    }

    fun discardAll() {
        if (commentsById.isEmpty()) {
            return
        }
        commentsById.clear()
        dispatcher.multicaster.commentsChanged()
    }

    fun find(commentId: String): ReviewComment? = commentsById[commentId]

    fun getAll(): List<ReviewComment> = commentsById.values.toList()
}
