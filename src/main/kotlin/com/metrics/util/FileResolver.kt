package com.metrics.util

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Resolves file paths from diff headers to actual source files in the project.
 * Handles various diff formats and file path resolution strategies.
 */
class FileResolver(private val projectRoot: Path) {
    
    /**
     * Resolves a file path from a diff header to an actual file in the project.
     * 
     * @param diffPath The file path as it appears in the diff (e.g., "src/main/kotlin/com/example/Service.kt")
     * @return The resolved File object, or null if the file doesn't exist
     */
    fun resolveFile(diffPath: String): File? {
        val cleanPath = cleanDiffPath(diffPath)
        
        // Try different resolution strategies
        val candidates = listOf(
            projectRoot.resolve(cleanPath),
            projectRoot.resolve(cleanPath.removePrefix("a/")),
            projectRoot.resolve(cleanPath.removePrefix("b/")),
            projectRoot.resolve(cleanPath.substringAfter("/")),
            findFileByName(cleanPath.substringAfterLast("/"))
        ).filterNotNull()
        
        return candidates.firstOrNull { it.toFile().exists() }?.toFile()
    }
    
    /**
     * Finds all source files in the project that match the given extensions.
     * 
     * @param extensions File extensions to search for (e.g., listOf("kt", "java"))
     * @return List of source files matching the extensions
     */
    fun findSourceFiles(extensions: List<String>): List<File> {
        val sourceFiles = mutableListOf<File>()
        val extensionSet = extensions.toSet()
        
        projectRoot.toFile().walkTopDown()
            .filter { it.isFile }
            .filter { it.extension in extensionSet }
            .filter { !it.path.contains("/test/") } // Exclude test files
            .filter { !it.path.contains("/build/") } // Exclude build files
            .forEach { sourceFiles.add(it) }
        
        return sourceFiles
    }
    
    /**
     * Validates that the given file exists and is readable.
     * 
     * @param file The file to validate
     * @return ValidationResult containing success status and error message if any
     */
    fun validateFile(file: File): ValidationResult {
        return when {
            !file.exists() -> ValidationResult(false, "File does not exist: ${file.path}")
            !file.isFile -> ValidationResult(false, "Path is not a file: ${file.path}")
            !file.canRead() -> ValidationResult(false, "File is not readable: ${file.path}")
            else -> ValidationResult(true, null)
        }
    }
    
    /**
     * Extracts the relative path from project root for a given file.
     * 
     * @param file The file to get relative path for
     * @return The relative path from project root, or absolute path if not under project root
     */
    fun getRelativePath(file: File): String {
        val projectRootFile = projectRoot.toFile()
        return if (file.path.startsWith(projectRootFile.path)) {
            file.relativeTo(projectRootFile).path
        } else {
            file.path
        }
    }
    
    private fun cleanDiffPath(diffPath: String): String {
        return diffPath
            .trim()
            .removePrefix("a/")
            .removePrefix("b/")
            .removePrefix("./")
            .replace("\\", "/")
    }
    
    private fun findFileByName(fileName: String): Path? {
        if (fileName.isBlank()) return null
        
        return projectRoot.toFile().walkTopDown()
            .filter { it.isFile }
            .filter { it.name == fileName }
            .filter { !it.path.contains("/test/") }
            .filter { !it.path.contains("/build/") }
            .firstOrNull()?.toPath()
    }
    
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String?
    )
}