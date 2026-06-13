package com.myrpasko.commentsplugin.model

data class ReviewComment(
    val id: String,
    val filePath: String,
    val lineNumber: Int,
    val lineLabel: String = lineNumber.toString(),
    val commentText: String,
    val lineTextPreview: String? = null,
    val viewerKind: String? = null,
)
