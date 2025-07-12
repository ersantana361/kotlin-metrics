package com.metrics.util

/**
 * Utility for pattern matching in class and method analysis.
 * Simplified version for Phase 1.
 */
object PatternMatchingUtils {
    
    fun isTest(className: String): Boolean {
        return className.endsWith("Test") || className.contains("Test")
    }
    
    fun isDto(className: String): Boolean {
        return className.endsWith("DTO") || className.endsWith("Dto")
    }
    
    fun isService(className: String): Boolean {
        return className.endsWith("Service")
    }
    
    fun isRepository(className: String): Boolean {
        return className.endsWith("Repository")
    }
    
    fun isController(className: String): Boolean {
        return className.endsWith("Controller")
    }
}