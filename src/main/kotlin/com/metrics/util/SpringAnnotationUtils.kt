package com.metrics.util

import org.jetbrains.kotlin.psi.KtClassOrObject

/**
 * Utility for detecting Spring annotations.
 * Simplified version for Phase 1.
 */
object SpringAnnotationUtils {
    
    fun hasServiceAnnotation(classText: String): Boolean {
        return classText.contains("@Service")
    }
    
    fun hasRepositoryAnnotation(classText: String): Boolean {
        return classText.contains("@Repository")
    }
    
    fun hasControllerAnnotation(classText: String): Boolean {
        return classText.contains("@Controller") || classText.contains("@RestController")
    }
    
    fun hasComponentAnnotation(classText: String): Boolean {
        return classText.contains("@Component")
    }
    
    /**
     * Generic function to check for any Spring annotation.
     */
    fun hasSpringAnnotation(classOrObject: KtClassOrObject, annotationName: String): Boolean {
        val classText = classOrObject.text
        return classText.contains("@$annotationName")
    }
}