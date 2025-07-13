package com.metrics.parser

import com.metrics.parser.kotlin.KotlinFileParser
import com.metrics.parser.java.JavaFileParser
import org.jetbrains.kotlin.psi.KtFile
import com.github.javaparser.ast.CompilationUnit
import java.io.File

/**
 * Unified parser that can handle multiple programming languages.
 * Automatically delegates to the appropriate language-specific parser.
 */
class MultiLanguageParser {
    
    private val kotlinParser = KotlinFileParser()
    private val javaParser = JavaFileParser()
    
    /**
     * Parses files and separates them by language.
     * 
     * @param files List of source code files to parse
     * @return ParsedFiles containing separate lists for Kotlin and Java files
     */
    fun parseFiles(files: List<File>): ParsedFiles {
        val kotlinFiles = mutableListOf<KtFile>()
        val javaFiles = mutableListOf<CompilationUnit>()
        val originalJavaFiles = mutableListOf<File>()
        
        for (file in files) {
            when {
                kotlinParser.canParse(file) -> {
                    kotlinParser.parse(file)?.let { kotlinFiles.add(it) }
                }
                javaParser.canParse(file) -> {
                    javaParser.parse(file)?.let { 
                        javaFiles.add(it)
                        originalJavaFiles.add(file)
                    }
                }
                else -> {
                    println("Unsupported file type: ${file.name}")
                }
            }
        }
        
        return ParsedFiles(kotlinFiles, javaFiles, originalJavaFiles)
    }
    
    /**
     * Filters files by supported languages.
     * 
     * @param files List of files to filter
     * @return List of files that can be parsed by this parser
     */
    fun getSupportedFiles(files: List<File>): List<File> {
        return files.filter { file ->
            kotlinParser.canParse(file) || javaParser.canParse(file)
        }
    }
    
    /**
     * Cleanup resources when parser is no longer needed.
     */
    fun dispose() {
        kotlinParser.dispose()
    }
}

/**
 * Container for parsed files separated by language.
 */
data class ParsedFiles(
    val kotlinFiles: List<KtFile>,
    val javaCompilationUnits: List<CompilationUnit>,
    val originalJavaFiles: List<File>
)