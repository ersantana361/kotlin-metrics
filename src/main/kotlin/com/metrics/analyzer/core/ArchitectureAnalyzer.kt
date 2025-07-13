package com.metrics.analyzer.core

import com.metrics.model.analysis.ClassAnalysis
import com.metrics.model.architecture.ArchitectureAnalysis
import java.io.File

/**
 * Interface for analyzing architecture patterns and dependencies.
 */
interface ArchitectureAnalyzer {
    /**
     * Analyzes the architecture of the codebase including DDD patterns,
     * layered architecture, and dependency graphs.
     * 
     * @param files Source code files to analyze
     * @param classAnalyses Previously computed class analysis results
     * @return ArchitectureAnalysis containing patterns, layers, and dependencies
     */
    fun analyze(files: List<File>, classAnalyses: List<ClassAnalysis>): ArchitectureAnalysis
}