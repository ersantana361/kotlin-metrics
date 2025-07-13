package com.metrics.analyzer.core

/**
 * Interface for calculating LCOM (Lack of Cohesion of Methods) metrics.
 */
interface LcomCalculator<T> {
    /**
     * Calculates the LCOM value for a given class.
     * 
     * @param classNode The AST node representing the class
     * @return LCOM value (0 or greater, where 0 indicates perfect cohesion)
     */
    fun calculateLcom(classNode: T): Int
    
    /**
     * Analyzes method-property relationships within a class.
     * 
     * @param classNode The AST node representing the class
     * @return Map of method names to sets of properties they access
     */
    fun analyzeMethodPropertyRelationships(classNode: T): Map<String, Set<String>>
}