package com.metrics.util

import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction

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
    
    /**
     * Checks if a class contains business logic methods.
     * Looks for non-trivial methods that aren't just getters/setters.
     */
    fun hasBusinessLogic(classOrObject: KtClassOrObject): Boolean {
        val methods = classOrObject.declarations.filterIsInstance<KtNamedFunction>()
        
        return methods.any { method ->
            val methodText = method.text
            val isNotAccessor = !methodText.contains("get") && !methodText.contains("set")
            val hasControlFlow = methodText.contains("if") || methodText.contains("when") || 
                               methodText.contains("for") || methodText.contains("while")
            val hasBusinessKeywords = methodText.contains("calculate") || methodText.contains("process") ||
                                    methodText.contains("validate") || methodText.contains("execute") ||
                                    methodText.contains("handle") || methodText.contains("transform")
            
            (isNotAccessor && hasControlFlow) || hasBusinessKeywords
        }
    }
    
    // Additional pattern matching functions for tests
    fun isTestClass(className: String): Boolean {
        return className.endsWith("Test") || className.endsWith("Spec") || className.contains("Test")
    }
    
    fun isUtilityClass(className: String): Boolean {
        return className.endsWith("Utils") || className.endsWith("Helper") || className.endsWith("Util")
    }
    
    fun isExceptionClass(className: String): Boolean {
        return className.endsWith("Exception") || className.endsWith("Error")
    }
    
    fun isControllerClass(className: String): Boolean {
        return className.endsWith("Controller")
    }
    
    fun isServiceClass(className: String): Boolean {
        return className.endsWith("Service")
    }
    
    fun isRepositoryClass(className: String): Boolean {
        return className.endsWith("Repository")
    }
    
    fun isEntityClass(className: String): Boolean {
        return className.endsWith("Entity")
    }
}