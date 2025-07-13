package com.metrics.parser.java

import com.metrics.parser.SourceCodeParser
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import java.io.File

/**
 * Parser for Java source code files.
 * Uses JavaParser library for parsing.
 */
class JavaFileParser : SourceCodeParser<CompilationUnit> {
    
    override fun parse(file: File): CompilationUnit? {
        return try {
            StaticJavaParser.parse(file)
        } catch (e: Exception) {
            println("Error parsing Java file ${file.name}: ${e.message}")
            null
        }
    }
    
    override fun canParse(file: File): Boolean {
        return file.extension.equals("java", ignoreCase = true)
    }
}