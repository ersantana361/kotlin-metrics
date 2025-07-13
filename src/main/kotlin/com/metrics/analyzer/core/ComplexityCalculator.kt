package com.metrics.analyzer.core

import com.metrics.model.analysis.ComplexityAnalysis
import com.metrics.model.analysis.MethodComplexity

/**
 * Interface for calculating cyclomatic complexity metrics.
 */
interface ComplexityCalculator<ClassType, MethodType> {
    /**
     * Calculates complexity analysis for all methods in a class.
     * 
     * @param classNode The AST node representing the class
     * @return ComplexityAnalysis with aggregated complexity metrics
     */
    fun analyzeClassComplexity(classNode: ClassType): ComplexityAnalysis
    
    /**
     * Calculates cyclomatic complexity for a single method.
     * 
     * @param methodNode The AST node representing the method
     * @return MethodComplexity containing complexity and line count
     */
    fun calculateMethodComplexity(methodNode: MethodType): MethodComplexity
}