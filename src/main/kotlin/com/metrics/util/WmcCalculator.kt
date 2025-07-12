package com.metrics.util

import org.jetbrains.kotlin.psi.*
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration

/**
 * Calculator for WMC (Weighted Methods per Class) metric.
 * WMC is the sum of cyclomatic complexities of all methods in a class.
 */
object WmcCalculator {
    
    /**
     * Calculates WMC for a Kotlin class.
     * Uses existing ComplexityCalculator for method-level complexity.
     */
    fun calculateWmc(classOrObject: KtClassOrObject): Int {
        val methods = classOrObject.declarations.filterIsInstance<KtNamedFunction>()
        
        return methods.sumOf { method ->
            ComplexityCalculator.calculateCyclomaticComplexity(method)
        }
    }
    
    /**
     * Calculates WMC for a Java class.
     */
    fun calculateJavaWmc(classDecl: ClassOrInterfaceDeclaration): Int {
        val methods = classDecl.getMethods()
        
        return methods.sumOf { method ->
            ComplexityCalculator.calculateJavaCyclomaticComplexity(method)
        }
    }
    
    /**
     * Gets WMC quality assessment.
     */
    fun getWmcAssessment(wmc: Int): String {
        return when {
            wmc <= 5 -> "Low complexity - excellent maintainability"
            wmc <= 15 -> "Moderate complexity - acceptable"
            wmc <= 30 -> "High complexity - consider refactoring"
            wmc <= 50 -> "Very high complexity - refactoring recommended"
            else -> "Extremely high complexity - critical refactoring needed"
        }
    }
    
    /**
     * Gets WMC quality level (1-10 scale).
     */
    fun getWmcQualityLevel(wmc: Int): Double {
        return when {
            wmc <= 5 -> 10.0
            wmc <= 15 -> 8.0
            wmc <= 30 -> 6.0
            wmc <= 50 -> 4.0
            wmc <= 75 -> 2.0
            else -> 1.0
        }
    }
}