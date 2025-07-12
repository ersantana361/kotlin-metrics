package com.metrics.util

import org.jetbrains.kotlin.psi.KtClassOrObject

/**
 * Utility class for detecting various code patterns and class types.
 */
object PatternMatchingUtils {
    
    /**
     * Checks if a class is a test class based on naming conventions.
     */
    fun isTestClass(className: String): Boolean {
        return className.endsWith("Test") || 
               className.endsWith("Tests") || 
               className.endsWith("Spec") ||
               className.startsWith("Test") ||
               className.contains("Test")
    }
    
    /**
     * Checks if a class is a utility class based on naming conventions.
     */
    fun isUtilityClass(className: String): Boolean {
        return className.endsWith("Util") || 
               className.endsWith("Utils") || 
               className.endsWith("Utilities") ||
               className.endsWith("Helper") ||
               className.endsWith("Helpers") ||
               className.endsWith("Tools") ||
               className.endsWith("Constants")
    }
    
    /**
     * Checks if a class is a Data Transfer Object (DTO).
     */
    fun isDtoClass(className: String, classOrObject: KtClassOrObject): Boolean {
        // Check naming patterns
        val hasDataName = className.endsWith("DTO") || 
                         className.endsWith("Dto") || 
                         className.endsWith("Data") ||
                         className.endsWith("Request") ||
                         className.endsWith("Response") ||
                         className.endsWith("Payload")
        
        // Check if it's a data class
        val isDataClass = classOrObject.hasModifier(org.jetbrains.kotlin.lexer.KtTokens.DATA_KEYWORD)
        
        // Check if it has only properties (no methods)
        val hasOnlyProperties = classOrObject.declarations.all { 
            it is org.jetbrains.kotlin.psi.KtProperty
        }
        
        return hasDataName || isDataClass || hasOnlyProperties
    }
    
    /**
     * Checks if a class is a controller based on naming and annotations.
     */
    fun isControllerClass(className: String, classOrObject: KtClassOrObject? = null): Boolean {
        // Check naming patterns
        val hasControllerName = className.endsWith("Controller") || 
                               className.endsWith("Resource") ||
                               className.endsWith("Endpoint") ||
                               className.endsWith("Api")
        
        // Check for Spring MVC annotations if class is provided
        val hasControllerAnnotation = classOrObject?.let { clazz ->
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "Controller") ||
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "RestController") ||
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "RequestMapping")
        } ?: false
        
        return hasControllerName || hasControllerAnnotation
    }
    
    /**
     * Checks if a class is a service based on naming and annotations.
     */
    fun isServiceClass(className: String, classOrObject: KtClassOrObject? = null): Boolean {
        // Check naming patterns
        val hasServiceName = className.endsWith("Service") || 
                           className.endsWith("Manager") ||
                           className.endsWith("Handler") ||
                           className.endsWith("Processor")
        
        // Check for Spring service annotations if class is provided
        val hasServiceAnnotation = classOrObject?.let { clazz ->
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "Service") ||
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "Component") ||
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "Transactional")
        } ?: false
        
        return hasServiceName || hasServiceAnnotation
    }
    
    /**
     * Checks if a class is a repository based on naming and annotations.
     */
    fun isRepositoryClass(className: String, classOrObject: KtClassOrObject? = null): Boolean {
        // Check naming patterns
        val hasRepositoryName = className.endsWith("Repository") || 
                              className.endsWith("Dao") ||
                              className.endsWith("DAO") ||
                              className.endsWith("Store")
        
        // Check for Spring repository annotations if class is provided
        val hasRepositoryAnnotation = classOrObject?.let { clazz ->
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "Repository") ||
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "Component")
        } ?: false
        
        return hasRepositoryName || hasRepositoryAnnotation
    }
    
    /**
     * Checks if a class is an entity based on naming and annotations.
     */
    fun isEntityClass(className: String, classOrObject: KtClassOrObject? = null): Boolean {
        // Check naming patterns
        val hasEntityName = className.endsWith("Entity") || 
                          className.endsWith("Model") ||
                          className.endsWith("Domain") ||
                          className.endsWith("Aggregate")
        
        // Check for JPA/persistence annotations if class is provided
        val hasEntityAnnotation = classOrObject?.let { clazz ->
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "Entity") ||
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "Table") ||
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "Document")
        } ?: false
        
        return hasEntityName || hasEntityAnnotation
    }
    
    /**
     * Checks if a class contains business logic based on its structure.
     */
    fun hasBusinessLogic(classOrObject: KtClassOrObject): Boolean {
        val methods = classOrObject.declarations.filterIsInstance<org.jetbrains.kotlin.psi.KtNamedFunction>()
        
        // Look for methods that suggest business logic
        val businessLogicIndicators = methods.any { method ->
            val methodName = method.name?.lowercase() ?: ""
            
            // Common business logic method patterns
            methodName.startsWith("calculate") ||
            methodName.startsWith("compute") ||
            methodName.startsWith("process") ||
            methodName.startsWith("validate") ||
            methodName.startsWith("transform") ||
            methodName.startsWith("handle") ||
            methodName.startsWith("execute") ||
            methodName.contains("business") ||
            methodName.contains("rule") ||
            methodName.contains("policy")
        }
        
        // Check for complex method bodies (methods with multiple statements)
        val hasComplexMethods = methods.any { method ->
            method.bodyExpression?.children?.size ?: 0 > 3
        }
        
        return businessLogicIndicators || hasComplexMethods
    }
    
    /**
     * Checks if a class is a configuration class.
     */
    fun isConfigurationClass(className: String, classOrObject: KtClassOrObject? = null): Boolean {
        // Check naming patterns
        val hasConfigName = className.endsWith("Config") || 
                          className.endsWith("Configuration") ||
                          className.endsWith("Properties") ||
                          className.endsWith("Settings")
        
        // Check for Spring configuration annotations if class is provided
        val hasConfigAnnotation = classOrObject?.let { clazz ->
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "Configuration") ||
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "ConfigurationProperties") ||
            SpringAnnotationUtils.hasSpringAnnotation(clazz, "EnableAutoConfiguration")
        } ?: false
        
        return hasConfigName || hasConfigAnnotation
    }
    
    /**
     * Checks if a class is an exception/error class.
     */
    fun isExceptionClass(className: String): Boolean {
        return className.endsWith("Exception") || 
               className.endsWith("Error") ||
               className.endsWith("Fault") ||
               className.endsWith("Failure")
    }
    
    /**
     * Checks if a class is an interface implementation.
     */
    fun isInterfaceImplementation(className: String): Boolean {
        return className.endsWith("Impl") || 
               className.endsWith("Implementation") ||
               className.contains("Impl")
    }
    
    /**
     * Determines the primary purpose/role of a class based on multiple patterns.
     */
    fun determineClassRole(className: String, classOrObject: KtClassOrObject? = null): String {
        return when {
            isTestClass(className) -> "Test"
            isControllerClass(className, classOrObject) -> "Controller"
            isServiceClass(className, classOrObject) -> "Service"
            isRepositoryClass(className, classOrObject) -> "Repository"
            isEntityClass(className, classOrObject) -> "Entity"
            isConfigurationClass(className, classOrObject) -> "Configuration"
            isDtoClass(className, classOrObject ?: createMockClassOrObject()) -> "DTO"
            isUtilityClass(className) -> "Utility"
            isExceptionClass(className) -> "Exception"
            isInterfaceImplementation(className) -> "Implementation"
            else -> "Unknown"
        }
    }
    
    private fun createMockClassOrObject(): KtClassOrObject {
        // Return a mock implementation - in practice this should use a proper mock framework
        // For now, we'll use null-safe operations where classOrObject is optional
        // This is a temporary solution for compilation
        throw UnsupportedOperationException("Mock implementation not available")
    }
}