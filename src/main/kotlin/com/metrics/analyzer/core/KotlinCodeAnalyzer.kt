package com.metrics.analyzer.core

import com.metrics.model.analysis.*
import com.metrics.model.architecture.*
import com.metrics.util.*
import org.jetbrains.kotlin.psi.*

/**
 * Analyzer for Kotlin source code.
 * Uses extracted utility classes for analysis.
 */
class KotlinCodeAnalyzer : CodeAnalyzer {
    
    override fun analyze(files: List<java.io.File>): ProjectReport {
        // Legacy compatibility implementation using BackwardCompatibilityHelper
        val emptyArchitecture = ArchitectureAnalysis(
            dddPatterns = DddPatternAnalysis(
                entities = emptyList(),
                valueObjects = emptyList(),
                services = emptyList(),
                repositories = emptyList(),
                aggregates = emptyList(),
                domainEvents = emptyList()
            ),
            layeredArchitecture = LayeredArchitectureAnalysis(
                layers = emptyList(),
                dependencies = emptyList(),
                violations = emptyList(),
                pattern = com.metrics.model.common.ArchitecturePattern.UNKNOWN
            ),
            dependencyGraph = DependencyGraph(
                nodes = emptyList(),
                edges = emptyList(),
                cycles = emptyList(),
                packages = emptyList()
            )
        )
        
        return BackwardCompatibilityHelper.createEnhancedProjectReport(
            timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            classes = emptyList(),
            summary = "Use analyzeFiles method instead",
            architectureAnalysis = emptyArchitecture
        )
    }
    
    /**
     * Analyzes Kotlin files and returns class analysis results.
     * Uses the extracted utility classes for proper analysis.
     */
    fun analyzeFiles(ktFiles: List<KtFile>): List<ClassAnalysis> {
        val analyses = mutableListOf<ClassAnalysis>()
        
        // Collect all classes for cross-class analysis (coupling, inheritance)
        val allClasses = ktFiles.flatMap { it.declarations.filterIsInstance<KtClassOrObject>() }
        
        for (ktFile in ktFiles) {
            val classes = ktFile.declarations.filterIsInstance<KtClassOrObject>()
            
            for (classOrObject in classes) {
                try {
                    val analysis = analyzeClass(classOrObject, ktFile, allClasses)
                    analyses.add(analysis)
                } catch (e: Exception) {
                    // Skip malformed classes but continue analysis
                    println("⚠️  Skipping class ${classOrObject.name}: ${e.message}")
                }
            }
        }
        
        return analyses
    }
    
    /**
     * Analyzes a single Kotlin class using the extracted utility classes.
     */
    private fun analyzeClass(classOrObject: KtClassOrObject, ktFile: KtFile, allClasses: List<KtClassOrObject>): ClassAnalysis {
        val className = classOrObject.name ?: "Unknown"
        val fileName = ktFile.name
        
        // Extract method-property relationships for LCOM calculation
        val methodPropertyMap = extractMethodPropertyRelationships(classOrObject)
        
        // Calculate LCOM using utility class
        val lcom = LcomCalculator.calculateLcom(methodPropertyMap)
        
        // Calculate complexity for all methods using utility class
        val methods = classOrObject.declarations.filterIsInstance<KtNamedFunction>()
        val methodComplexities = methods.map { method ->
            val complexity = ComplexityCalculator.calculateCyclomaticComplexity(method)
            val lineCount = method.text.split('\n').size
            MethodComplexity(method.name ?: "unknown", complexity, lineCount)
        }
        
        val complexityAnalysis = ComplexityAnalysis(
            methods = methodComplexities,
            totalComplexity = methodComplexities.sumOf { it.cyclomaticComplexity },
            averageComplexity = if (methodComplexities.isNotEmpty()) 
                methodComplexities.map { it.cyclomaticComplexity }.average() else 0.0,
            maxComplexity = methodComplexities.maxOfOrNull { it.cyclomaticComplexity } ?: 0,
            complexMethods = methodComplexities.filter { it.cyclomaticComplexity > 10 }
        )
        
        // Generate suggestions using utility class
        val properties = classOrObject.declarations.filterIsInstance<KtProperty>().mapNotNull { it.name }
        val suggestions = SuggestionGenerator.generateSuggestions(lcom, methodPropertyMap, properties, complexityAnalysis)
        
        return BackwardCompatibilityHelper.createEnhancedKotlinClassAnalysis(
            classOrObject = classOrObject,
            allKotlinClasses = allClasses,
            fileName = fileName,
            lcom = lcom,
            methodCount = methods.size,
            propertyCount = classOrObject.declarations.filterIsInstance<KtProperty>().size,
            methodDetails = methodPropertyMap,
            suggestions = suggestions,
            complexity = complexityAnalysis
        )
    }
    
    /**
     * Extracts method-property relationships for LCOM calculation.
     */
    private fun extractMethodPropertyRelationships(classOrObject: KtClassOrObject): Map<String, Set<String>> {
        val methodPropertyMap = mutableMapOf<String, Set<String>>()
        val classProperties = classOrObject.declarations
            .filterIsInstance<KtProperty>()
            .mapNotNull { it.name }
            .toSet()
        
        val methods = classOrObject.declarations.filterIsInstance<KtNamedFunction>()
        
        for (method in methods) {
            val methodName = method.name ?: "unknown"
            val usedProperties = mutableSetOf<String>()
            
            // Simple property usage detection - look for property names in method body
            val methodText = method.text
            for (property in classProperties) {
                if (methodText.contains(property)) {
                    usedProperties.add(property)
                }
            }
            
            methodPropertyMap[methodName] = usedProperties
        }
        
        return methodPropertyMap
    }
}