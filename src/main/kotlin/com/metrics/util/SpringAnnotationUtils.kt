package com.metrics.util

import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtAnnotationEntry

/**
 * Utility class for detecting Spring framework annotations and related patterns.
 */
object SpringAnnotationUtils {
    
    /**
     * Checks if a class has a specific Spring annotation.
     * 
     * @param classOrObject The class to check
     * @param annotationName The annotation name (without @)
     * @return true if the annotation is present
     */
    fun hasSpringAnnotation(classOrObject: KtClassOrObject, annotationName: String): Boolean {
        val annotations = classOrObject.annotationEntries
        
        return annotations.any { annotation ->
            val annotationText = annotation.shortName?.asString() ?: ""
            
            // Check for exact match
            if (annotationText == annotationName) {
                return@any true
            }
            
            // Check for Spring-specific patterns
            when (annotationName.lowercase()) {
                "controller" -> {
                    annotationText in setOf("Controller", "RestController", "RequestMapping")
                }
                "service" -> {
                    annotationText in setOf("Service", "Component", "Transactional")
                }
                "repository" -> {
                    annotationText in setOf("Repository", "Component")
                }
                "entity" -> {
                    annotationText in setOf("Entity", "Table", "Document", "Embeddable")
                }
                "configuration" -> {
                    annotationText in setOf("Configuration", "ConfigurationProperties", "EnableAutoConfiguration")
                }
                else -> false
            }
        }
    }
    
    /**
     * Gets all Spring annotations present on a class.
     */
    fun getSpringAnnotations(classOrObject: KtClassOrObject): List<String> {
        val springAnnotations = setOf(
            // Core Spring annotations
            "Component", "Service", "Repository", "Controller", "RestController",
            "Configuration", "Bean", "Autowired", "Qualifier", "Value",
            
            // Spring MVC annotations
            "RequestMapping", "GetMapping", "PostMapping", "PutMapping", "DeleteMapping",
            "RequestParam", "PathVariable", "RequestBody", "ResponseBody",
            
            // Spring Data annotations
            "Entity", "Table", "Column", "Id", "GeneratedValue", "Transactional",
            "Query", "Modifying", "Document", "Field",
            
            // Spring Boot annotations
            "SpringBootApplication", "EnableAutoConfiguration", "ComponentScan",
            "ConfigurationProperties", "Profile", "Conditional",
            
            // Spring Security annotations
            "Secured", "PreAuthorize", "PostAuthorize", "RolesAllowed",
            
            // Spring Test annotations
            "SpringBootTest", "MockBean", "TestConfiguration", "ActiveProfiles"
        )
        
        return classOrObject.annotationEntries
            .mapNotNull { it.shortName?.asString() }
            .filter { it in springAnnotations }
    }
    
    /**
     * Checks if a class is a Spring-managed component.
     */
    fun isSpringComponent(classOrObject: KtClassOrObject): Boolean {
        val componentAnnotations = setOf(
            "Component", "Service", "Repository", "Controller", "RestController", "Configuration"
        )
        
        return classOrObject.annotationEntries.any { annotation ->
            annotation.shortName?.asString() in componentAnnotations
        }
    }
    
    /**
     * Checks if a class is a Spring MVC controller.
     */
    fun isSpringController(classOrObject: KtClassOrObject): Boolean {
        return hasSpringAnnotation(classOrObject, "Controller") ||
               hasSpringAnnotation(classOrObject, "RestController")
    }
    
    /**
     * Checks if a class is a Spring service.
     */
    fun isSpringService(classOrObject: KtClassOrObject): Boolean {
        return hasSpringAnnotation(classOrObject, "Service")
    }
    
    /**
     * Checks if a class is a Spring repository.
     */
    fun isSpringRepository(classOrObject: KtClassOrObject): Boolean {
        return hasSpringAnnotation(classOrObject, "Repository")
    }
    
    /**
     * Checks if a class is a JPA entity.
     */
    fun isJpaEntity(classOrObject: KtClassOrObject): Boolean {
        return hasSpringAnnotation(classOrObject, "Entity") ||
               hasSpringAnnotation(classOrObject, "Table")
    }
    
    /**
     * Checks if a class is a Spring configuration class.
     */
    fun isSpringConfiguration(classOrObject: KtClassOrObject): Boolean {
        return hasSpringAnnotation(classOrObject, "Configuration")
    }
    
    /**
     * Determines the Spring stereotype of a class.
     */
    fun getSpringStereotype(classOrObject: KtClassOrObject): String? {
        return when {
            isSpringController(classOrObject) -> "Controller"
            isSpringService(classOrObject) -> "Service"
            isSpringRepository(classOrObject) -> "Repository"
            isSpringConfiguration(classOrObject) -> "Configuration"
            isJpaEntity(classOrObject) -> "Entity"
            isSpringComponent(classOrObject) -> "Component"
            else -> null
        }
    }
    
    /**
     * Checks if a class has dependency injection annotations.
     */
    fun hasDependencyInjection(classOrObject: KtClassOrObject): Boolean {
        val diAnnotations = setOf("Autowired", "Inject", "Value", "Resource")
        
        // Check class-level annotations
        val hasClassDI = classOrObject.annotationEntries.any { annotation ->
            annotation.shortName?.asString() in diAnnotations
        }
        
        // Check constructor parameters, properties, and methods
        // This is a simplified check - in practice, you'd need to examine:
        // - Constructor parameters
        // - Property declarations
        // - Method parameters
        
        return hasClassDI
    }
    
    /**
     * Gets the request mapping path from a controller class.
     */
    fun getRequestMappingPath(classOrObject: KtClassOrObject): String? {
        val requestMappingAnnotation = classOrObject.annotationEntries.find { annotation ->
            val name = annotation.shortName?.asString()
            name == "RequestMapping" || name == "RestController"
        }
        
        // Extract path from annotation parameters
        // This is a simplified implementation
        return requestMappingAnnotation?.valueArguments?.firstOrNull()?.getArgumentExpression()?.text
    }
    
    /**
     * Checks if a class uses Spring Boot auto-configuration.
     */
    fun usesAutoConfiguration(classOrObject: KtClassOrObject): Boolean {
        val autoConfigAnnotations = setOf(
            "SpringBootApplication", "EnableAutoConfiguration", "EnableJpaRepositories",
            "EnableWebMvc", "EnableScheduling", "EnableAsync", "EnableCaching"
        )
        
        return classOrObject.annotationEntries.any { annotation ->
            annotation.shortName?.asString() in autoConfigAnnotations
        }
    }
}