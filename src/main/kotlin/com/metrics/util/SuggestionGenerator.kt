package com.metrics.util

import com.metrics.model.analysis.ComplexityAnalysis
import com.metrics.model.analysis.Suggestion

/**
 * Utility class for generating code improvement suggestions based on metrics analysis.
 */
object SuggestionGenerator {
    
    /**
     * Generates actionable improvement suggestions based on LCOM and complexity metrics.
     * 
     * @param lcom The Lack of Cohesion of Methods value
     * @param methodProps Map of method names to their accessed properties
     * @param props List of all class properties
     * @param complexity Complexity analysis results
     * @return List of actionable suggestions
     */
    fun generateSuggestions(
        lcom: Int, 
        methodProps: Map<String, Set<String>>, 
        props: List<String>,
        complexity: ComplexityAnalysis
    ): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        // LCOM-based suggestions
        when (lcom) {
            0 -> {
                suggestions.add(Suggestion(
                    icon = "✅",
                    message = "Perfect cohesion",
                    tooltip = "All methods share common properties. This indicates excellent class design following the Single Responsibility Principle."
                ))
            }
            in 1..2 -> {
                suggestions.add(Suggestion(
                    icon = "👍",
                    message = "Good cohesion",
                    tooltip = "Low LCOM indicates good class design. Methods are well-related and share common data."
                ))
            }
            in 3..5 -> {
                suggestions.add(Suggestion(
                    icon = "⚠️",
                    message = "Consider refactoring",
                    tooltip = "Moderate LCOM suggests potential for improvement. Look for groups of methods that could be extracted into separate classes."
                ))
            }
            in 6..10 -> {
                suggestions.add(Suggestion(
                    icon = "🔧",
                    message = "Refactoring recommended",
                    tooltip = "High LCOM indicates poor cohesion. Consider splitting this class into smaller, more focused classes."
                ))
                
                // Add specific refactoring suggestions for high LCOM
                if (methodProps.isNotEmpty()) {
                    val unusedProperties = findUnusedProperties(methodProps, props)
                    if (unusedProperties.isNotEmpty()) {
                        suggestions.add(Suggestion(
                            icon = "📤",
                            message = "Remove unused properties",
                            tooltip = "Properties not used by any method: ${unusedProperties.joinToString(", ")}"
                        ))
                    }
                    
                    val methodGroups = findMethodGroups(methodProps)
                    if (methodGroups.size > 1) {
                        suggestions.add(Suggestion(
                            icon = "🔀",
                            message = "Split into ${methodGroups.size} classes",
                            tooltip = "Methods can be grouped by shared properties: ${methodGroups.map { it.key }.joinToString(", ")}"
                        ))
                    }
                }
            }
            else -> {
                suggestions.add(Suggestion(
                    icon = "🚨",
                    message = "Critical refactoring needed",
                    tooltip = "Very high LCOM (${lcom}) indicates severe cohesion problems. This class likely violates the Single Responsibility Principle."
                ))
            }
        }
        
        // Complexity-based suggestions
        val avgComplexity = complexity.averageComplexity
        when {
            avgComplexity <= 2 -> {
                suggestions.add(Suggestion(
                    icon = "✨",
                    message = "Excellent simplicity",
                    tooltip = "Low average complexity (${String.format("%.1f", avgComplexity)}) indicates well-structured, maintainable code."
                ))
            }
            avgComplexity <= 5 -> {
                suggestions.add(Suggestion(
                    icon = "👍",
                    message = "Good complexity",
                    tooltip = "Acceptable average complexity (${String.format("%.1f", avgComplexity)}). Code is reasonably maintainable."
                ))
            }
            avgComplexity <= 10 -> {
                suggestions.add(Suggestion(
                    icon = "⚡",
                    message = "Simplify complex methods",
                    tooltip = "Moderate average complexity (${String.format("%.1f", avgComplexity)}). Consider breaking down complex methods."
                ))
            }
            else -> {
                suggestions.add(Suggestion(
                    icon = "🧠",
                    message = "High cognitive load",
                    tooltip = "High average complexity (${String.format("%.1f", avgComplexity)}) makes code hard to understand and maintain."
                ))
            }
        }
        
        // Method count suggestions
        val methodCount = methodProps.size
        when {
            methodCount > 20 -> {
                suggestions.add(Suggestion(
                    icon = "📏",
                    message = "Too many methods ($methodCount)",
                    tooltip = "Classes with many methods are hard to maintain. Consider splitting into smaller classes."
                ))
            }
            methodCount == 0 -> {
                suggestions.add(Suggestion(
                    icon = "❌",
                    message = "No methods found",
                    tooltip = "This class has no methods. Consider if it should be a data class or if methods are missing."
                ))
            }
        }
        
        // Complex methods suggestions
        if (complexity.complexMethods.isNotEmpty()) {
            val veryComplexMethods = complexity.methods.filter { it.cyclomaticComplexity > 20 }
            if (veryComplexMethods.isNotEmpty()) {
                suggestions.add(Suggestion(
                    icon = "🚨",
                    message = "${veryComplexMethods.size} very complex methods",
                    tooltip = "Methods with CC > 20: ${veryComplexMethods.map { it.methodName }.joinToString(", ")}"
                ))
            } else {
                suggestions.add(Suggestion(
                    icon = "🔧",
                    message = "${complexity.complexMethods.size} complex methods",
                    tooltip = "Methods with CC > 10: ${complexity.complexMethods.map { it.methodName }.joinToString(", ")}"
                ))
            }
        }
        
        // Combined LCOM + Complexity suggestions
        if (lcom > 5 && avgComplexity > 7) {
            suggestions.add(Suggestion(
                icon = "🎯",
                message = "Priority refactoring target",
                tooltip = "Both high LCOM ($lcom) and complexity (${String.format("%.1f", avgComplexity)}) indicate this class needs immediate attention."
            ))
        }
        
        return suggestions.take(5) // Limit to 5 most important suggestions
    }
    
    /**
     * Generates suggestions specifically for Java classes.
     */
    fun generateJavaSuggestions(
        lcom: Int, 
        methodCount: Int, 
        fieldCount: Int,
        complexity: ComplexityAnalysis
    ): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        // Java-specific LCOM suggestions
        when (lcom) {
            0 -> suggestions.add(Suggestion("✅", "Perfect cohesion", "All methods share common fields"))
            in 1..2 -> suggestions.add(Suggestion("👍", "Good cohesion", "Low LCOM indicates good design"))
            in 3..5 -> suggestions.add(Suggestion("⚠️", "Consider refactoring", "Moderate LCOM suggests potential improvements"))
            else -> suggestions.add(Suggestion("🔧", "Refactoring needed", "High LCOM indicates poor cohesion"))
        }
        
        // Java method count suggestions
        if (methodCount > 15) {
            suggestions.add(Suggestion(
                "📏", 
                "Too many methods ($methodCount)", 
                "Consider splitting this Java class"
            ))
        }
        
        // Java field suggestions
        if (fieldCount > 10) {
            suggestions.add(Suggestion(
                "📊", 
                "Many fields ($fieldCount)", 
                "Consider if this class has too many responsibilities"
            ))
        }
        
        // Java complexity suggestions
        if (complexity.complexMethods.isNotEmpty()) {
            suggestions.add(Suggestion(
                "🧠", 
                "${complexity.complexMethods.size} complex methods", 
                "Consider extracting complex logic into separate methods"
            ))
        }
        
        return suggestions
    }
    
    private fun findUnusedProperties(methodProps: Map<String, Set<String>>, props: List<String>): List<String> {
        val usedProperties = methodProps.values.flatten().toSet()
        return props.filter { it !in usedProperties }
    }
    
    private fun findMethodGroups(methodProps: Map<String, Set<String>>): Map<String, List<String>> {
        val groups = mutableMapOf<String, MutableList<String>>()
        
        for ((method, properties) in methodProps) {
            val groupKey = properties.sorted().joinToString(",")
            groups.computeIfAbsent(groupKey) { mutableListOf() }.add(method)
        }
        
        return groups.filter { it.value.size > 1 } // Only groups with multiple methods
    }
}