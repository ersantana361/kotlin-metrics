package com.metrics.analyzer.core

import com.metrics.model.analysis.ClassAnalysis
import com.metrics.model.analysis.Suggestion
import com.metrics.analyzer.lcom.KotlinLcomCalculator
import com.metrics.analyzer.complexity.KotlinComplexityCalculator
import org.jetbrains.kotlin.psi.KtClassOrObject

/**
 * Analyzes Kotlin classes to calculate LCOM, complexity, and generate suggestions.
 */
class KotlinClassAnalyzer : ClassAnalyzer<KtClassOrObject> {
    
    private val lcomCalculator = KotlinLcomCalculator()
    private val complexityCalculator = KotlinComplexityCalculator()
    
    override fun analyze(classNode: KtClassOrObject, fileName: String): ClassAnalysis {
        val className = classNode.name ?: "Anonymous"
        val properties = classNode.declarations.filterIsInstance<org.jetbrains.kotlin.psi.KtProperty>()
        val methods = classNode.body?.functions ?: emptyList()
        
        // Calculate LCOM
        val lcom = lcomCalculator.calculateLcom(classNode)
        val methodDetails = lcomCalculator.analyzeMethodPropertyRelationships(classNode)
        
        // Calculate complexity
        val complexity = complexityCalculator.analyzeClassComplexity(classNode)
        
        // Calculate CK metrics
        val ckMetrics = com.metrics.util.CouplingCalculator.calculateCkMetrics(classNode, lcom, complexity)
        
        // Calculate quality score
        val qualityScore = com.metrics.util.QualityScoreCalculator.calculateQualityScore(ckMetrics)
        
        // Generate risk assessment
        val riskAssessment = com.metrics.util.QualityScoreCalculator.generateRiskAssessment(ckMetrics, qualityScore)
        
        // Generate suggestions
        val suggestions = generateSuggestions(lcom, methodDetails, properties.map { it.name!! }, complexity)
        
        return ClassAnalysis(
            className = className,
            fileName = fileName,
            lcom = lcom,
            methodCount = methods.size,
            propertyCount = properties.size,
            methodDetails = methodDetails,
            suggestions = suggestions,
            complexity = complexity,
            ckMetrics = ckMetrics,
            qualityScore = qualityScore,
            riskAssessment = riskAssessment
        )
    }
    
    private fun generateSuggestions(
        lcom: Int,
        methodProps: Map<String, Set<String>>,
        props: List<String>,
        complexity: com.metrics.model.analysis.ComplexityAnalysis
    ): List<Suggestion> {
        val suggestions = mutableListOf<Suggestion>()
        
        // LCOM-based suggestions
        when {
            lcom == 0 -> suggestions.add(
                Suggestion("✅", "Excellent cohesion", "This class has perfect cohesion - all methods share properties")
            )
            lcom in 1..2 -> suggestions.add(
                Suggestion("👍", "Good cohesion", "Class has good cohesion with minimal property isolation")
            )
            lcom in 3..5 -> suggestions.add(
                Suggestion("⚠️", "Consider refactoring", "Moderate cohesion - some methods might belong in separate classes")
            )
            lcom > 5 -> suggestions.add(
                Suggestion("❌", "Poor cohesion", "Consider splitting this class - many methods don't share properties")
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
        if (methodProps.size > 20) {
            suggestions.add(
                Suggestion("📏", "Large class", "Class has many methods - consider using composition or inheritance")
            )
        }
        
        if (props.size > 15) {
            suggestions.add(
                Suggestion("📊", "Many properties", "Class has many properties - consider grouping related data")
            )
        }
        
        return suggestions
    }
}