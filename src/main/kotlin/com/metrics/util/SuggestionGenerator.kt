package com.metrics.util

import com.metrics.model.analysis.ComplexityAnalysis
import com.metrics.model.analysis.Suggestion

/**
 * Utility class for generating code improvement suggestions based on metrics analysis.
 */
object SuggestionGenerator {
    
    /**
     * Generates actionable improvement suggestions based on LCOM and complexity metrics.
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
            0 -> suggestions.add(Suggestion("✅", "Perfect cohesion", "All methods share common properties"))
            in 1..2 -> suggestions.add(Suggestion("👍", "Good cohesion", "Low LCOM indicates good design"))
            in 3..5 -> suggestions.add(Suggestion("⚠️", "Consider refactoring", "Moderate LCOM suggests potential improvements"))
            else -> suggestions.add(Suggestion("🔧", "Refactoring needed", "High LCOM indicates poor cohesion"))
        }
        
        // Complexity suggestions
        if (complexity.complexMethods.isNotEmpty()) {
            suggestions.add(Suggestion(
                "🧠", 
                "${complexity.complexMethods.size} complex methods", 
                "Consider extracting complex logic into separate methods"
            ))
        }
        
        return suggestions.take(3) // Limit to top 3 suggestions
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
        
        // LCOM suggestions
        when (lcom) {
            0 -> suggestions.add(Suggestion("✅", "Perfect cohesion", "All methods share common fields"))
            in 1..2 -> suggestions.add(Suggestion("👍", "Good cohesion", "Low LCOM indicates good design"))
            in 3..5 -> suggestions.add(Suggestion("⚠️", "Consider refactoring", "Moderate LCOM suggests potential improvements"))
            else -> suggestions.add(Suggestion("🔧", "Refactoring needed", "High LCOM indicates poor cohesion"))
        }
        
        // Method count suggestions
        if (methodCount > 15) {
            suggestions.add(Suggestion(
                "📏", 
                "Too many methods ($methodCount)", 
                "Consider splitting this Java class"
            ))
        }
        
        // Complexity suggestions
        if (complexity.complexMethods.isNotEmpty()) {
            suggestions.add(Suggestion(
                "🧠", 
                "${complexity.complexMethods.size} complex methods", 
                "Consider extracting complex logic into separate methods"
            ))
        }
        
        return suggestions.take(3) // Limit to top 3 suggestions
    }
}