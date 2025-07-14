package com.metrics.util

import com.metrics.model.architecture.*
import com.metrics.model.common.ArchitecturePattern

/**
 * Utility class for architecture-related operations including layer inference,
 * validation, and pattern detection.
 */
object ArchitectureUtils {
    
    /**
     * Infers the architectural layer from package name and class name.
     * 
     * @param packageName The package name of the class
     * @param className The class name
     * @return The inferred layer name or null if cannot be determined
     */
    fun inferLayer(packageName: String, className: String): String? {
        return when {
            // Package-based inference
            packageName.contains("presentation") || packageName.contains("controller") || 
            packageName.contains("api") || packageName.contains("web") -> "presentation"
            packageName.contains("application") || packageName.contains("service") -> "application"
            packageName.contains("domain") || packageName.contains("model") -> "domain"
            packageName.contains("repository") || packageName.contains("data") -> "data"
            packageName.contains("infrastructure") || packageName.contains("config") -> "infrastructure"
            
            // Class name-based inference
            className.endsWith("Controller") || className.endsWith("Api") -> "presentation"
            className.endsWith("Service") || className.endsWith("Manager") -> "application"
            className.endsWith("Repository") || className.endsWith("Dao") -> "data"
            className.endsWith("Entity") || className.endsWith("Model") -> "domain"
            className.endsWith("Config") || className.endsWith("Configuration") -> "infrastructure"
            else -> null
        }
    }
    
    /**
     * Validates whether a dependency between architectural layers is valid.
     * Enforces standard layered architecture dependency rules.
     * 
     * @param fromLayer The source layer
     * @param toLayer The target layer
     * @return true if the dependency is valid, false otherwise
     */
    fun isValidLayerDependency(fromLayer: String, toLayer: String): Boolean {
        val layerHierarchy = mapOf(
            "presentation" to 1,
            "application" to 2,
            "domain" to 3,
            "data" to 4,
            "infrastructure" to 4
        )
        
        val fromLevel = layerHierarchy[fromLayer] ?: return true
        val toLevel = layerHierarchy[toLayer] ?: return true
        
        // Presentation can depend on application and domain
        // Application can depend on domain and data
        // Domain should not depend on higher layers
        // Data and infrastructure are at the same level
        
        return when (fromLayer) {
            "presentation" -> toLayer in listOf("application", "domain", "infrastructure")
            "application" -> toLayer in listOf("domain", "data", "infrastructure")
            "domain" -> toLayer == "infrastructure" // Domain can only depend on infrastructure
            "data" -> toLayer in listOf("domain", "infrastructure")
            "infrastructure" -> true // Infrastructure can depend on anything (typically external libs)
            else -> true
        }
    }
    
    /**
     * Determines the overall architecture pattern based on layers and dependencies.
     * 
     * @param layers List of architecture layers
     * @param dependencies List of layer dependencies
     * @return The detected architecture pattern
     */
    fun determineArchitecturePattern(
        layers: List<ArchitectureLayer>, 
        dependencies: List<LayerDependency>
    ): ArchitecturePattern {
        val layerNames = layers.map { it.name.lowercase() }.toSet()
        
        // Check for Hexagonal (Ports and Adapters) pattern
        if (hasHexagonalCharacteristics(layerNames, dependencies)) {
            return ArchitecturePattern.HEXAGONAL
        }
        
        // Check for Clean Architecture pattern
        if (hasCleanArchitectureCharacteristics(layerNames, dependencies)) {
            return ArchitecturePattern.CLEAN
        }
        
        // Check for Onion Architecture pattern
        if (hasOnionCharacteristics(layerNames, dependencies)) {
            return ArchitecturePattern.ONION
        }
        
        // Default to Layered architecture
        return ArchitecturePattern.LAYERED
    }
    
    private fun hasHexagonalCharacteristics(
        layerNames: Set<String>, 
        dependencies: List<LayerDependency>
    ): Boolean {
        // Hexagonal architecture typically has ports and adapters
        val hasPortsAndAdapters = layerNames.any { it.contains("port") } ||
                                  layerNames.any { it.contains("adapter") } ||
                                  layerNames.any { it.contains("api") && it.contains("impl") }
        
        // Domain should be at the center with minimal dependencies
        val domainLayer = layerNames.find { it.contains("domain") }
        if (domainLayer != null) {
            val domainDependencies = dependencies.count { it.fromLayer == domainLayer }
            return hasPortsAndAdapters && domainDependencies <= 2
        }
        
        return hasPortsAndAdapters
    }
    
    private fun hasCleanArchitectureCharacteristics(
        layerNames: Set<String>, 
        dependencies: List<LayerDependency>
    ): Boolean {
        // Clean architecture typically has use cases/interactors
        val hasUseCases = layerNames.any { it.contains("usecase") || it.contains("interactor") }
        
        // Should have clear separation between entities, use cases, and frameworks
        val hasEntities = layerNames.any { it.contains("entity") || it.contains("domain") }
        val hasFrameworks = layerNames.any { it.contains("framework") || it.contains("infrastructure") }
        
        return hasUseCases && hasEntities && hasFrameworks
    }
    
    private fun hasOnionCharacteristics(
        layerNames: Set<String>, 
        dependencies: List<LayerDependency>
    ): Boolean {
        // Onion architecture has concentric layers with domain at core
        val hasDomainCore = layerNames.any { it.contains("domain") || it.contains("core") }
        val hasApplicationServices = layerNames.any { it.contains("application") || it.contains("service") }
        val hasInfrastructure = layerNames.any { it.contains("infrastructure") || it.contains("adapter") }
        
        // Dependencies should flow inward
        val inwardDependencies = dependencies.count { dependency ->
            val fromPriority = getOnionLayerPriority(dependency.fromLayer)
            val toPriority = getOnionLayerPriority(dependency.toLayer)
            fromPriority > toPriority // Outer layers depend on inner layers
        }
        
        val totalDependencies = dependencies.size
        val inwardRatio = if (totalDependencies > 0) inwardDependencies.toDouble() / totalDependencies else 0.0
        
        return hasDomainCore && hasApplicationServices && hasInfrastructure && inwardRatio > 0.7
    }
    
    private fun getOnionLayerPriority(layerName: String): Int {
        return when {
            layerName.contains("domain") || layerName.contains("core") -> 1
            layerName.contains("application") || layerName.contains("service") -> 2
            layerName.contains("infrastructure") || layerName.contains("adapter") -> 3
            layerName.contains("presentation") || layerName.contains("api") -> 4
            else -> 5
        }
    }
    
    /**
     * Checks if a file is in a domain package based on its file path.
     * 
     * @param fileName The file path/name
     * @return true if the file is in a domain package
     */
    fun isInDomainPackage(fileName: String): Boolean {
        return fileName.contains("/domain/") || 
               fileName.contains("/model/") ||
               fileName.contains("\\domain\\") || 
               fileName.contains("\\model\\")
    }
}