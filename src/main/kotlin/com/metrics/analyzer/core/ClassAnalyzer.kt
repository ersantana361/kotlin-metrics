package com.metrics.analyzer.core

import com.metrics.model.analysis.ClassAnalysis

/**
 * Interface for analyzing individual classes.
 * Implementations should handle language-specific AST nodes.
 */
interface ClassAnalyzer<T> {
    /**
     * Analyzes a single class and returns detailed metrics.
     * 
     * @param classNode The AST node representing the class (e.g., KtClassOrObject for Kotlin)
     * @param fileName The name of the file containing the class
     * @return ClassAnalysis containing LCOM, complexity, and other metrics
     */
    fun analyze(classNode: T, fileName: String): ClassAnalysis
}