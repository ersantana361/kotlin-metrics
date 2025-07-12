package com.metrics.analyzer.core

import com.metrics.model.analysis.ProjectReport
import java.io.File

/**
 * Main interface for analyzing code and generating reports.
 * This is the primary entry point for code analysis.
 */
interface CodeAnalyzer {
    /**
     * Analyzes the given files and generates a comprehensive project report.
     * 
     * @param files List of source code files to analyze (can be Kotlin or Java)
     * @return ProjectReport containing all analysis results
     */
    fun analyze(files: List<File>): ProjectReport
}