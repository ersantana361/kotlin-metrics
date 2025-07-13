package com.metrics.analyzer.core

import com.metrics.model.analysis.ClassAnalysis
import com.metrics.model.analysis.Suggestion
import com.metrics.analyzer.lcom.JavaLcomCalculator
import com.metrics.analyzer.complexity.JavaComplexityCalculator
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration

/**
 * Analyzes Java classes to calculate LCOM, complexity, and generate suggestions.
 */
class JavaClassAnalyzer : ClassAnalyzer<ClassOrInterfaceDeclaration> {
    
    private val lcomCalculator = JavaLcomCalculator()
    private val complexityCalculator = JavaComplexityCalculator()
    
    override fun analyze(classNode: ClassOrInterfaceDeclaration, fileName: String): ClassAnalysis {
        val className = classNode.nameAsString
        val fields = classNode.fields
        val methods = classNode.methods
        
        // Calculate LCOM
        val lcom = lcomCalculator.calculateLcom(classNode)
        val methodDetails = lcomCalculator.analyzeMethodPropertyRelationships(classNode)
        
        // Calculate complexity
        val complexity = complexityCalculator.analyzeClassComplexity(classNode)
        
        // Generate suggestions
        val suggestions = generateSuggestions(lcom, methodDetails, fields.map { it.variables.first().nameAsString }, complexity)
        
        return ClassAnalysis(
            className = className,
            fileName = fileName,
            lcom = lcom,
            methodCount = methods.size,
            propertyCount = fields.size,
            methodDetails = methodDetails,
            suggestions = suggestions,
            complexity = complexity
        )
    }
    
    private fun generateSuggestions(
        lcom: Int,
        methodFields: Map<String, Set<String>>,
        fields: List<String>,
        complexity: com.metrics.model.analysis.ComplexityAnalysis
    ): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        // LCOM-based suggestions
        when {
            lcom == 0 -> suggestions.add(
                Suggestion("✅", "Excellent cohesion", "This class has perfect cohesion - all methods share fields")
            )
            lcom in 1..2 -> suggestions.add(
                Suggestion("👍", "Good cohesion", "Class has good cohesion with minimal field isolation")
            )
            lcom in 3..5 -> suggestions.add(
                Suggestion("⚠️", "Consider refactoring", "Moderate cohesion - some methods might belong in separate classes")
            )
            lcom > 5 -> suggestions.add(
                Suggestion("❌", "Poor cohesion", "Consider splitting this class - many methods don't share fields")
            )
        }
        
        // Complexity-based suggestions
        if (complexity.maxComplexity > 15) {
            suggestions.add(
                Suggestion("🔥", "High complexity detected", "Some methods are very complex - consider breaking them down")
            )
        } else if (complexity.maxComplexity > 10) {
            suggestions.add(
                Suggestion("⚠️", "Complex methods found", "Review methods with high cyclomatic complexity")
            )
        }
        
        // Size-based suggestions
        if (methodFields.size > 20) {
            suggestions.add(
                Suggestion("📏", "Large class", "Class has many methods - consider using composition or inheritance")
            )
        }
        
        if (fields.size > 15) {
            suggestions.add(
                Suggestion("📊", "Many fields", "Class has many fields - consider grouping related data")
            )
        }
        
        return suggestions
    }
}