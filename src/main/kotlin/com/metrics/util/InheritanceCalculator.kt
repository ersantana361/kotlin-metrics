package com.metrics.util

import org.jetbrains.kotlin.psi.*
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration

/**
 * Calculator for inheritance metrics: DIT (Depth of Inheritance Tree) and NOC (Number of Children).
 */
object InheritanceCalculator {
    
    /**
     * Simplified DIT calculation without requiring all classes context.
     */
    fun calculateDit(classOrObject: KtClassOrObject): Int {
        return calculateDit(classOrObject, emptyList())
    }
    
    /**
     * Simplified NOC calculation without requiring all classes context.
     */
    fun calculateNoc(classOrObject: KtClassOrObject): Int {
        return calculateNoc(classOrObject, emptyList())
    }
    
    /**
     * Simplified Java DIT calculation.
     */
    fun calculateJavaDit(classDecl: ClassOrInterfaceDeclaration): Int {
        return calculateJavaDit(classDecl, emptyList())
    }
    
    /**
     * Simplified Java NOC calculation.
     */
    fun calculateJavaNoc(classDecl: ClassOrInterfaceDeclaration): Int {
        return calculateJavaNoc(classDecl, emptyList())
    }
    
    /**
     * Calculates DIT (Depth of Inheritance Tree) for a Kotlin class.
     * DIT is the maximum depth of the inheritance hierarchy for a class.
     * Note: Interface implementation doesn't count as inheritance depth.
     */
    fun calculateDit(classOrObject: KtClassOrObject, allClasses: List<KtClassOrObject>): Int {
        try {
            val className = classOrObject.name ?: return 0
            
            // Check if this class has any superclasses (excluding interfaces)
            val supertypes = classOrObject.superTypeListEntries
            if (supertypes.isEmpty()) {
                return 0 // No inheritance
            }
            
            var maxDepth = 0
            supertypes.forEach { supertype ->
                val supertypeName = supertype.typeAsUserType?.referencedName
                if (supertypeName != null) {
                    val superClass = allClasses.find { it.name == supertypeName }
                    if (superClass != null) {
                        // Only count class inheritance, not interface implementation
                        if (superClass is KtClass && !superClass.isInterface()) {
                            val depth = 1 + calculateKotlinInheritanceDepth(superClass, allClasses, mutableSetOf(className))
                            maxDepth = maxOf(maxDepth, depth)
                        }
                    } else {
                        // External class - only count if it's likely a class (not interface)
                        val estimatedDepth = when {
                            supertypeName in listOf("Any", "Object") -> 1
                            supertypeName.endsWith("Exception") -> 2
                            supertypeName.startsWith("Abstract") -> 2
                            supertypeName.endsWith("Interface") -> 0 // Interface implementation
                            supertypeName.all { it.isLowerCase() || it == '_' } -> 0 // Interface naming pattern
                            else -> 1 // Assume class inheritance
                        }
                        maxDepth = maxOf(maxDepth, estimatedDepth)
                    }
                }
            }
            
            return minOf(maxDepth, 8) // Cap at reasonable depth
            
        } catch (e: Exception) {
            // Fallback: simple estimation
            return 0
        }
    }
    
    /**
     * Calculates NOC (Number of Children) for a Kotlin class.
     * NOC is the number of immediate subclasses of a class.
     */
    fun calculateNoc(classOrObject: KtClassOrObject, allClasses: List<KtClassOrObject>): Int {
        try {
            val className = classOrObject.name ?: return 0
            
            val childCount = allClasses.count { otherClass ->
                otherClass != classOrObject && 
                otherClass.superTypeListEntries.any { 
                    it.typeAsUserType?.referencedName == className 
                }
            }
            
            // Return actual child count only (no estimates)
            return childCount
            
        } catch (e: Exception) {
            // Fallback: just return 0 for NOC if we can't calculate
            return 0
        }
    }
    
    /**
     * Calculates DIT for a Java class.
     */
    fun calculateJavaDit(classDecl: ClassOrInterfaceDeclaration, allClasses: List<ClassOrInterfaceDeclaration>): Int {
        return calculateJavaInheritanceDepth(classDecl, allClasses, mutableSetOf())
    }
    
    /**
     * Calculates NOC for a Java class.
     */
    fun calculateJavaNoc(classDecl: ClassOrInterfaceDeclaration, allClasses: List<ClassOrInterfaceDeclaration>): Int {
        val className = classDecl.nameAsString
        
        return allClasses.count { otherClass ->
            otherClass != classDecl && javaInheritsDirectlyFrom(otherClass, className)
        }
    }
    
    /**
     * Gets DIT quality assessment.
     */
    fun getDitAssessment(dit: Int): String {
        return when {
            dit <= 2 -> "Shallow inheritance - good design"
            dit <= 4 -> "Moderate inheritance depth"
            dit <= 6 -> "Deep inheritance - may affect maintainability"
            else -> "Very deep inheritance - consider composition over inheritance"
        }
    }
    
    /**
     * Gets NOC quality assessment.
     */
    fun getNocAssessment(noc: Int): String {
        return when {
            noc == 0 -> "Leaf class - no inheritance complexity"
            noc <= 5 -> "Reasonable number of children"
            noc <= 10 -> "Many children - ensure proper abstraction"
            else -> "Too many children - consider refactoring hierarchy"
        }
    }
    
    /**
     * Gets DIT quality level (1-10 scale).
     */
    fun getDitQualityLevel(dit: Int): Double {
        return when {
            dit <= 2 -> 10.0
            dit <= 4 -> 8.0
            dit <= 6 -> 6.0
            dit <= 8 -> 4.0
            dit <= 10 -> 2.0
            else -> 1.0
        }
    }
    
    /**
     * Gets NOC quality level (1-10 scale).
     */
    fun getNocQualityLevel(noc: Int): Double {
        return when {
            noc <= 3 -> 10.0
            noc <= 5 -> 8.0
            noc <= 8 -> 6.0
            noc <= 12 -> 4.0
            noc <= 20 -> 2.0
            else -> 1.0
        }
    }
    
    private fun calculateKotlinInheritanceDepth(
        classOrObject: KtClassOrObject, 
        allClasses: List<KtClassOrObject>,
        visited: MutableSet<String>
    ): Int {
        val className = classOrObject.name ?: return 0
        
        if (visited.contains(className)) {
            return 0 // Circular inheritance detection
        }
        visited.add(className)
        
        // Get superclass names
        val superTypes = classOrObject.getSuperTypeList()?.entries?.mapNotNull { entry ->
            entry.typeAsUserType?.referencedName
        } ?: emptyList()
        
        if (superTypes.isEmpty()) {
            return 0 // Base case: no inheritance
        }
        
        // Find the maximum depth among all super types
        val maxSuperDepth = superTypes.maxOfOrNull { superTypeName ->
            val superClass = allClasses.find { it.name == superTypeName }
            if (superClass != null) {
                calculateKotlinInheritanceDepth(superClass, allClasses, visited.toMutableSet())
            } else {
                0 // External class, assume depth 0
            }
        } ?: 0
        
        return maxSuperDepth + 1
    }
    
    private fun calculateJavaInheritanceDepth(
        classDecl: ClassOrInterfaceDeclaration,
        allClasses: List<ClassOrInterfaceDeclaration>,
        visited: MutableSet<String>
    ): Int {
        val className = classDecl.nameAsString
        
        if (visited.contains(className)) {
            return 0 // Circular inheritance detection
        }
        visited.add(className)
        
        // Get extended classes
        val extendedTypes = classDecl.extendedTypes.map { it.nameAsString }
        
        if (extendedTypes.isEmpty()) {
            return 1 // Base case: no inheritance (except Object)
        }
        
        // Find the maximum depth among extended types
        val maxSuperDepth = extendedTypes.maxOfOrNull { superTypeName ->
            val superClass = allClasses.find { it.nameAsString == superTypeName }
            if (superClass != null) {
                calculateJavaInheritanceDepth(superClass, allClasses, visited.toMutableSet())
            } else {
                1 // External class, assume depth 1
            }
        } ?: 0
        
        return maxSuperDepth + 1
    }
    
    private fun inheritsDirectlyFrom(childClass: KtClassOrObject, parentClassName: String): Boolean {
        val superTypes = childClass.getSuperTypeList()?.entries?.mapNotNull { entry ->
            entry.typeAsUserType?.referencedName
        } ?: emptyList()
        
        return superTypes.contains(parentClassName)
    }
    
    private fun javaInheritsDirectlyFrom(childClass: ClassOrInterfaceDeclaration, parentClassName: String): Boolean {
        return childClass.extendedTypes.any { it.nameAsString == parentClassName }
    }
}