package com.metrics.util

import java.io.File

/**
 * Parses Git diff files to extract file changes and code snippets.
 * Supports unified diff format and handles both Kotlin and Java files.
 */
class DiffParser {
    
    /**
     * Parses a diff file and returns structured diff information.
     * 
     * @param diffFile The diff file to parse
     * @return ParsedDiff containing all file changes
     */
    fun parse(diffFile: File): ParsedDiff {
        val content = diffFile.readText()
        return parse(content)
    }
    
    /**
     * Parses diff content string and returns structured diff information.
     * 
     * @param diffContent The diff content as string
     * @return ParsedDiff containing all file changes
     */
    fun parse(diffContent: String): ParsedDiff {
        val lines = diffContent.lines()
        val fileChanges = mutableListOf<FileChange>()
        
        var currentFileChange: FileChange? = null
        var currentHunk: DiffHunk? = null
        
        for (line in lines) {
            when {
                line.startsWith("diff --git") -> {
                    // Save previous file change
                    currentFileChange?.let { fileChanges.add(it) }
                    
                    // Start new file change
                    val paths = extractGitPaths(line)
                    currentFileChange = FileChange(
                        originalPath = paths.first,
                        newPath = paths.second,
                        hunks = mutableListOf()
                    )
                    currentHunk = null
                }
                
                line.startsWith("---") -> {
                    currentFileChange?.let { fc ->
                        fc.originalPath = extractFilePath(line)
                    }
                }
                
                line.startsWith("+++") -> {
                    currentFileChange?.let { fc ->
                        fc.newPath = extractFilePath(line)
                    }
                }
                
                line.startsWith("@@") -> {
                    // Start new hunk
                    currentHunk = parseHunkHeader(line)
                    currentFileChange?.hunks?.add(currentHunk)
                }
                
                line.startsWith(" ") || line.startsWith("+") || line.startsWith("-") -> {
                    // Add line to current hunk
                    currentHunk?.let { hunk ->
                        val changeType = when {
                            line.startsWith("+") -> ChangeType.ADDED
                            line.startsWith("-") -> ChangeType.REMOVED
                            else -> ChangeType.CONTEXT
                        }
                        
                        hunk.lines.add(DiffLine(
                            changeType = changeType,
                            content = line.substring(1), // Remove the +/- prefix
                            lineNumber = calculateLineNumber(hunk, changeType)
                        ))
                    }
                }
            }
        }
        
        // Add the last file change
        currentFileChange?.let { fileChanges.add(it) }
        
        return ParsedDiff(fileChanges)
    }
    
    private fun extractGitPaths(line: String): Pair<String, String> {
        // Extract from "diff --git a/path/to/file b/path/to/file"
        val parts = line.split(" ")
        val originalPath = parts.getOrNull(2)?.removePrefix("a/") ?: ""
        val newPath = parts.getOrNull(3)?.removePrefix("b/") ?: ""
        return Pair(originalPath, newPath)
    }
    
    private fun extractFilePath(line: String): String {
        // Extract from "--- a/path/to/file" or "+++ b/path/to/file"
        val parts = line.split("\t")[0].split(" ")
        return parts.getOrNull(1)?.removePrefix("a/")?.removePrefix("b/") ?: ""
    }
    
    private fun parseHunkHeader(line: String): DiffHunk {
        // Parse "@@ -oldStart,oldCount +newStart,newCount @@"
        val regex = Regex("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@")
        val match = regex.find(line)
        
        return if (match != null) {
            val oldStart = match.groupValues[1].toIntOrNull() ?: 0
            val oldCount = match.groupValues[2].toIntOrNull() ?: 1
            val newStart = match.groupValues[3].toIntOrNull() ?: 0
            val newCount = match.groupValues[4].toIntOrNull() ?: 1
            
            DiffHunk(
                oldStart = oldStart,
                oldCount = oldCount,
                newStart = newStart,
                newCount = newCount,
                lines = mutableListOf()
            )
        } else {
            DiffHunk(0, 0, 0, 0, mutableListOf())
        }
    }
    
    private fun calculateLineNumber(hunk: DiffHunk, changeType: ChangeType): Int {
        val addedLines = hunk.lines.count { it.changeType == ChangeType.ADDED }
        val removedLines = hunk.lines.count { it.changeType == ChangeType.REMOVED }
        val contextLines = hunk.lines.count { it.changeType == ChangeType.CONTEXT }
        
        return when (changeType) {
            ChangeType.ADDED -> hunk.newStart + addedLines + contextLines
            ChangeType.REMOVED -> hunk.oldStart + removedLines + contextLines
            ChangeType.CONTEXT -> hunk.oldStart + contextLines
        }
    }
}

/**
 * Represents a parsed diff with all file changes.
 */
data class ParsedDiff(
    val fileChanges: List<FileChange>
)

/**
 * Represents changes to a single file.
 */
data class FileChange(
    var originalPath: String,
    var newPath: String,
    val hunks: MutableList<DiffHunk>
) {
    /**
     * Checks if this is a file rename (paths are different).
     */
    fun isRenamed(): Boolean = originalPath != newPath
    
    /**
     * Checks if this is a new file (original path is /dev/null).
     */
    fun isNewFile(): Boolean = originalPath == "/dev/null"
    
    /**
     * Checks if this is a deleted file (new path is /dev/null).
     */
    fun isDeletedFile(): Boolean = newPath == "/dev/null"
    
    /**
     * Gets the effective file path (new path for renames, original path otherwise).
     */
    fun getEffectivePath(): String = if (isDeletedFile()) originalPath else newPath
    
    /**
     * Checks if the file is a Kotlin source file.
     */
    fun isKotlinFile(): Boolean = getEffectivePath().endsWith(".kt")
    
    /**
     * Checks if the file is a Java source file.
     */
    fun isJavaFile(): Boolean = getEffectivePath().endsWith(".java")
    
    /**
     * Checks if the file is a supported source file.
     */
    fun isSupportedFile(): Boolean = isKotlinFile() || isJavaFile()
}

/**
 * Represents a hunk (continuous block of changes) in a diff.
 */
data class DiffHunk(
    val oldStart: Int,
    val oldCount: Int,
    val newStart: Int,
    val newCount: Int,
    val lines: MutableList<DiffLine>
)

/**
 * Represents a single line in a diff.
 */
data class DiffLine(
    val changeType: ChangeType,
    val content: String,
    val lineNumber: Int
)

/**
 * Type of change for a diff line.
 */
enum class ChangeType {
    ADDED,    // Lines starting with +
    REMOVED,  // Lines starting with -
    CONTEXT   // Lines starting with space (unchanged)
}