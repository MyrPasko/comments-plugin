package com.myrpasko.commentsplugin.util

import com.intellij.diff.contents.DiffContent
import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.contents.FileContent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil

object ProjectPathResolver {
    fun resolveRelativePath(project: Project, content: DiffContent): String? {
        val path = when (content) {
            is DocumentContent -> content.highlightFile?.path
            is FileContent -> content.file.path
            else -> null
        } ?: return null

        val basePath = project.basePath ?: return path.substringAfterLast('/')
        return FileUtil.getRelativePath(basePath, path, '/') ?: path.substringAfterLast('/')
    }
}

