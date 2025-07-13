package com.metrics.parser

import java.io.File

/**
 * Interface for parsing source code files.
 */
interface SourceCodeParser<T> {
    /**
     * Parses a source code file and returns the AST representation.
     * 
     * @param file The source code file to parse
     * @return Parsed AST representation, or null if parsing fails
     */
    fun parse(file: File): T?
    
    /**
     * Checks if this parser can handle the given file type.
     * 
     * @param file The file to check
     * @return true if this parser can handle the file, false otherwise
     */
    fun canParse(file: File): Boolean
}