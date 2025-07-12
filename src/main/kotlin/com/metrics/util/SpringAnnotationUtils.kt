package com.metrics.util

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
}