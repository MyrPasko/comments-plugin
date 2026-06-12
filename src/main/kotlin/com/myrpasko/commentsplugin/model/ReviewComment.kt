package com.myrpasko.commentsplugin.model

data class ReviewComment(
    val id: String,
    val filePath: String,
    val lineNumber: Int,
    val commentText: String,
    val lineTextPreview: String? = null,
    val viewerKind: String? = null,
)

data class CommentLocation(
    val filePath: String,
    val lineNumber: Int,
) {
    fun id(): String = "$filePath:$lineNumber"
}

