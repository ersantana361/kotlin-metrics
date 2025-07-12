package com.metrics.util

import org.jetbrains.kotlin.psi.KtFile

/**
 * Utility class for type resolution and reference handling.
 */
object TypeResolutionUtils {
    
    /**
     * Resolves a type reference within the context of the given Kotlin files.
     * Handles generic types, nullable markers, and package resolution.
     * 
     * @param typeRef The type reference string to resolve
     * @param ktFiles All Kotlin files in the project for context
     * @param currentPackage The package of the current file
     * @return The fully qualified type name or null if cannot be resolved
     */
    fun resolveTypeReference(typeRef: String, ktFiles: List<KtFile>, currentPackage: String): String? {
        // Clean the type reference
        var cleanType = typeRef
            .replace("?", "") // Remove nullable marker
            .replace(Regex("<.*>"), "") // Remove generics
            .trim()
        
        // Handle primitive types
        if (isPrimitiveType(cleanType)) {
            return cleanType
        }
        
        // Handle built-in Kotlin types
        if (isKotlinBuiltinType(cleanType)) {
            return "kotlin.$cleanType"
        }
        
        // Handle java.lang types
        if (isJavaLangType(cleanType)) {
            return "java.lang.$cleanType"
        }
        
        // Try to find the type in imports of all files
        for (ktFile in ktFiles) {
            val importList = ktFile.importList?.imports ?: continue
            
            for (import in importList) {
                val importPath = import.importPath?.pathStr ?: continue
                
                // Check if import matches the type
                if (importPath.endsWith(".$cleanType") || importPath == cleanType) {
                    return importPath
                }
                
                // Check wildcard imports
                if (importPath.endsWith("*")) {
                    val packagePath = importPath.dropLast(1) // Remove the *
                    // Check if type exists in this package
                    val potentialType = "$packagePath$cleanType"
                    if (typeExistsInFiles(potentialType, ktFiles)) {
                        return potentialType
                    }
                }
            }
        }
        
        // Try to find in the same package
        val samePackageType = if (currentPackage.isNotEmpty()) {
            "$currentPackage.$cleanType"
        } else {
            cleanType
        }
        
        if (typeExistsInFiles(samePackageType, ktFiles)) {
            return samePackageType
        }
        
        // Try to find in project files without package qualification
        for (ktFile in ktFiles) {
            val packageName = ktFile.packageFqName.asString()
            val fullyQualifiedType = if (packageName.isNotEmpty()) {
                "$packageName.$cleanType"
            } else {
                cleanType
            }
            
            if (typeExistsInFiles(fullyQualifiedType, ktFiles)) {
                return fullyQualifiedType
            }
        }
        
        // Return as-is if we can't resolve it
        return cleanType
    }
    
    /**
     * Checks if a type is a primitive type.
     */
    private fun isPrimitiveType(type: String): Boolean {
        return type in setOf(
            "Boolean", "Byte", "Short", "Int", "Long", "Float", "Double", "Char",
            "boolean", "byte", "short", "int", "long", "float", "double", "char"
        )
    }
    
    /**
     * Checks if a type is a Kotlin built-in type.
     */
    private fun isKotlinBuiltinType(type: String): Boolean {
        return type in setOf(
            "String", "Any", "Unit", "Nothing", "Array", "List", "Set", "Map",
            "Collection", "Iterable", "Sequence", "Pair", "Triple"
        )
    }
    
    /**
     * Checks if a type is a java.lang type.
     */
    private fun isJavaLangType(type: String): Boolean {
        return type in setOf(
            "Object", "Class", "Thread", "Runnable", "Exception", "RuntimeException",
            "Error", "Throwable", "System", "Math", "StringBuilder", "StringBuffer"
        )
    }
    
    /**
     * Checks if a type exists in the given Kotlin files.
     */
    private fun typeExistsInFiles(fullyQualifiedType: String, ktFiles: List<KtFile>): Boolean {
        val typeName = fullyQualifiedType.substringAfterLast(".")
        val expectedPackage = fullyQualifiedType.substringBeforeLast(".")
        
        return ktFiles.any { file ->
            val filePackage = file.packageFqName.asString()
            val hasMatchingPackage = filePackage == expectedPackage || expectedPackage.isEmpty()
            
            if (hasMatchingPackage) {
                // Check if file contains a class/interface/object with this name
                file.declarations.any { declaration ->
                    declaration.name == typeName
                }
            } else {
                false
            }
        }
    }
    
    /**
     * Extracts the simple class name from a fully qualified name.
     */
    fun getSimpleClassName(fullyQualifiedName: String): String {
        return fullyQualifiedName.substringAfterLast(".")
    }
    
    /**
     * Extracts the package name from a fully qualified name.
     */
    fun getPackageName(fullyQualifiedName: String): String {
        return fullyQualifiedName.substringBeforeLast(".")
    }
    
    /**
     * Cleans a type reference by removing generics and nullable markers.
     */
    fun cleanTypeReference(typeRef: String): String {
        return typeRef
            .replace("?", "")
            .replace(Regex("<.*>"), "")
            .trim()
    }
}