package com.metrics.util

import com.metrics.model.architecture.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtNamedFunction
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration

/**
 * Utility class for detecting Domain-Driven Design (DDD) patterns in code.
 */
object DddPatternAnalyzer {
    
    /**
     * Analyzes a Kotlin class to determine if it matches the Entity pattern.
     */
    fun analyzeEntity(classOrObject: KtClassOrObject, fileName: String): DddEntity {
        val className = classOrObject.name ?: "Unknown"
        var confidence = 0.0
        
        // Check for ID fields
        val idFields = findIdFields(classOrObject)
        val hasUniqueId = idFields.isNotEmpty()
        if (hasUniqueId) confidence += 0.3
        
        // Check for mutability
        val isMutable = hasMutableProperties(classOrObject)
        if (isMutable) confidence += 0.2
        
        // Check for equals/hashCode methods
        if (hasEqualsHashCode(classOrObject)) confidence += 0.3
        
        // Check naming patterns
        if (className.endsWith("Entity") || className.endsWith("Aggregate")) {
            confidence += 0.2
        }
        
        // Check for entity annotations
        if (SpringAnnotationUtils.hasSpringAnnotation(classOrObject, "Entity")) {
            confidence += 0.4
        }
        
        // Check if in domain package
        if (ArchitectureUtils.isInDomainPackage(fileName)) {
            confidence += 0.1
        }
        
        // Check for business logic methods
        if (PatternMatchingUtils.hasBusinessLogic(classOrObject)) {
            confidence += 0.15
        }
        
        return DddEntity(
            className = className,
            fileName = fileName,
            hasUniqueId = hasUniqueId,
            isMutable = isMutable,
            idFields = idFields,
            confidence = confidence.coerceAtMost(1.0)
        )
    }
    
    /**
     * Analyzes a Kotlin class to determine if it matches the Value Object pattern.
     */
    fun analyzeValueObject(classOrObject: KtClassOrObject, fileName: String): DddValueObject {
        val className = classOrObject.name ?: "Unknown"
        var confidence = 0.0
        
        // Check for immutability
        val isImmutable = isImmutableClass(classOrObject)
        if (isImmutable) confidence += 0.4
        
        // Check for value equality (equals/hashCode)
        val hasValueEquality = hasEqualsHashCode(classOrObject)
        if (hasValueEquality) confidence += 0.3
        
        // Check if it's a data class
        if (classOrObject.hasModifier(org.jetbrains.kotlin.lexer.KtTokens.DATA_KEYWORD)) {
            confidence += 0.3
        }
        
        // Check naming patterns
        if (className.endsWith("Value") || className.endsWith("VO") || className.endsWith("ValueObject")) {
            confidence += 0.2
        }
        
        // Value objects should not have identity
        val hasNoIdFields = findIdFields(classOrObject).isEmpty()
        if (hasNoIdFields) confidence += 0.1
        
        // Should have no or minimal business logic
        val hasMinimalLogic = !PatternMatchingUtils.hasBusinessLogic(classOrObject)
        if (hasMinimalLogic) confidence += 0.1
        
        val properties = classOrObject.declarations
            .filterIsInstance<KtProperty>()
            .mapNotNull { it.name }
        
        return DddValueObject(
            className = className,
            fileName = fileName,
            isImmutable = isImmutable,
            hasValueEquality = hasValueEquality,
            properties = properties,
            confidence = confidence.coerceAtMost(1.0)
        )
    }
    
    /**
     * Analyzes a Kotlin class to determine if it matches the Service pattern.
     */
    fun analyzeService(classOrObject: KtClassOrObject, fileName: String): DddService {
        val className = classOrObject.name ?: "Unknown"
        var confidence = 0.0
        
        // Check for statelessness (no mutable fields)
        val isStateless = isStatelessClass(classOrObject)
        if (isStateless) confidence += 0.3
        
        // Check for domain logic
        val hasDomainLogic = PatternMatchingUtils.hasBusinessLogic(classOrObject)
        if (hasDomainLogic) confidence += 0.4
        
        // Check naming patterns
        if (className.endsWith("Service") || className.endsWith("DomainService")) {
            confidence += 0.2
        }
        
        // Check for service annotations
        if (SpringAnnotationUtils.hasSpringAnnotation(classOrObject, "Service")) {
            confidence += 0.3
        }
        
        // Should not be an entity or value object
        if (!className.endsWith("Entity") && !className.endsWith("Value")) {
            confidence += 0.1
        }
        
        val methods = classOrObject.declarations
            .filterIsInstance<KtNamedFunction>()
            .mapNotNull { it.name }
        
        return DddService(
            className = className,
            fileName = fileName,
            isStateless = isStateless,
            hasDomainLogic = hasDomainLogic,
            methods = methods,
            confidence = confidence.coerceAtMost(1.0)
        )
    }
    
    /**
     * Analyzes a Kotlin class to determine if it matches the Repository pattern.
     */
    fun analyzeRepository(classOrObject: KtClassOrObject, fileName: String): DddRepository {
        val className = classOrObject.name ?: "Unknown"
        var confidence = 0.0
        
        // Check naming patterns
        if (className.endsWith("Repository") || className.endsWith("Repo")) {
            confidence += 0.4
        }
        
        // Check for repository annotations
        if (SpringAnnotationUtils.hasSpringAnnotation(classOrObject, "Repository")) {
            confidence += 0.4
        }
        
        // Check if it's an interface
        val isInterface = classOrObject.isInterface()
        if (isInterface) confidence += 0.2
        
        // Check for CRUD operations
        val crudMethods = findCrudMethods(classOrObject)
        val hasDataAccess = crudMethods.isNotEmpty()
        if (hasDataAccess) confidence += 0.3
        
        // Should be in data or infrastructure layer
        val layer = ArchitectureUtils.inferLayer(fileName, className)
        if (layer == "data" || layer == "infrastructure") {
            confidence += 0.1
        }
        
        return DddRepository(
            className = className,
            fileName = fileName,
            isInterface = isInterface,
            hasDataAccess = hasDataAccess,
            crudMethods = crudMethods,
            confidence = confidence.coerceAtMost(1.0)
        )
    }
    
    /**
     * Analyzes a Kotlin class to determine if it matches the Domain Event pattern.
     */
    fun analyzeDomainEvent(classOrObject: KtClassOrObject, fileName: String): DddDomainEvent {
        val className = classOrObject.name ?: "Unknown"
        var confidence = 0.0
        
        // Check naming patterns
        if (className.endsWith("Event") || className.endsWith("DomainEvent") || 
            className.contains("Event") || className.endsWith("Happened")) {
            confidence += 0.4
        }
        
        // Should be immutable
        if (isImmutableClass(classOrObject)) {
            confidence += 0.3
        }
        
        // Should be in domain package
        if (ArchitectureUtils.isInDomainPackage(fileName)) {
            confidence += 0.2
        }
        
        // Should have timestamp or similar temporal properties
        val hasTemporalProperties = hasTemporalProperties(classOrObject)
        if (hasTemporalProperties) confidence += 0.2
        
        // Should not have business logic
        val hasMinimalLogic = !PatternMatchingUtils.hasBusinessLogic(classOrObject)
        if (hasMinimalLogic) confidence += 0.1
        
        val isEvent = confidence > 0.5
        
        return DddDomainEvent(
            className = className,
            fileName = fileName,
            isEvent = isEvent,
            confidence = confidence.coerceAtMost(1.0)
        )
    }
    
    /**
     * Finds related entities for aggregate analysis.
     */
    fun findRelatedEntities(
        entity: DddEntity, 
        allEntities: List<DddEntity>, 
        ktFiles: List<org.jetbrains.kotlin.psi.KtFile>
    ): List<String> {
        val relatedEntities = mutableListOf<String>()
        
        // Find entities that reference this entity
        for (otherEntity in allEntities) {
            if (otherEntity.className != entity.className) {
                // Check if other entity references this one
                if (referencesEntity(otherEntity.className, entity.className, ktFiles)) {
                    relatedEntities.add(otherEntity.className)
                }
            }
        }
        
        return relatedEntities
    }
    
    // Helper methods
    
    private fun findIdFields(classOrObject: KtClassOrObject): List<String> {
        return classOrObject.declarations
            .filterIsInstance<KtProperty>()
            .filter { property ->
                val name = property.name?.lowercase() ?: ""
                name == "id" || name.endsWith("id") || name == "uuid" ||
                SpringAnnotationUtils.hasSpringAnnotation(classOrObject, "Id")
            }
            .mapNotNull { it.name }
    }
    
    private fun hasMutableProperties(classOrObject: KtClassOrObject): Boolean {
        return classOrObject.declarations
            .filterIsInstance<KtProperty>()
            .any { it.isVar }
    }
    
    private fun hasEqualsHashCode(classOrObject: KtClassOrObject): Boolean {
        val methods = classOrObject.declarations
            .filterIsInstance<KtNamedFunction>()
            .mapNotNull { it.name }
        
        return methods.contains("equals") && methods.contains("hashCode")
    }
    
    private fun isImmutableClass(classOrObject: KtClassOrObject): Boolean {
        // Check if all properties are val (not var)
        val properties = classOrObject.declarations.filterIsInstance<KtProperty>()
        return properties.isNotEmpty() && properties.all { !it.isVar }
    }
    
    private fun isStatelessClass(classOrObject: KtClassOrObject): Boolean {
        // No mutable fields
        return !hasMutableProperties(classOrObject)
    }
    
    private fun findCrudMethods(classOrObject: KtClassOrObject): List<String> {
        val crudPatterns = setOf("save", "find", "delete", "update", "create", "get", "put", "post")
        
        return classOrObject.declarations
            .filterIsInstance<KtNamedFunction>()
            .mapNotNull { it.name }
            .filter { methodName ->
                val lowerName = methodName.lowercase()
                crudPatterns.any { pattern -> lowerName.contains(pattern) }
            }
    }
    
    private fun hasTemporalProperties(classOrObject: KtClassOrObject): Boolean {
        val temporalNames = setOf("timestamp", "time", "date", "created", "updated", "occurred", "when")
        
        return classOrObject.declarations
            .filterIsInstance<KtProperty>()
            .any { property ->
                val name = property.name?.lowercase() ?: ""
                temporalNames.any { pattern -> name.contains(pattern) }
            }
    }
    
    private fun referencesEntity(
        fromClassName: String, 
        toClassName: String, 
        ktFiles: List<org.jetbrains.kotlin.psi.KtFile>
    ): Boolean {
        // Simplified implementation - in reality would need deeper AST analysis
        return ktFiles.any { file ->
            val content = file.text
            content.contains(fromClassName) && content.contains(toClassName)
        }
    }
}