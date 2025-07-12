package com.metrics.analyzer.core

import com.metrics.model.analysis.*
import com.metrics.model.architecture.*
import com.metrics.util.*
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration

/**
 * Analyzer for Java source code.
 * Uses extracted utility classes for analysis.
 */
class JavaCodeAnalyzer : CodeAnalyzer {
    
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
     * Analyzes Java compilation units and returns class analysis results.
     * Uses the extracted utility classes for proper analysis.
     */
    fun analyzeFiles(javaFiles: List<CompilationUnit>, originalFiles: List<java.io.File>): List<ClassAnalysis> {
        val analyses = mutableListOf<ClassAnalysis>()
        
        // Collect all classes for cross-class analysis (coupling, inheritance)
        val allClasses = javaFiles.flatMap { it.findAll(ClassOrInterfaceDeclaration::class.java) }
        
        for ((index, compilationUnit) in javaFiles.withIndex()) {
            val fileName = if (index < originalFiles.size) originalFiles[index].name else "Unknown.java"
            
            val classes = compilationUnit.findAll(ClassOrInterfaceDeclaration::class.java)
            
            for (classDecl in classes) {
                try {
                    val analysis = analyzeJavaClass(classDecl, fileName, allClasses)
                    analyses.add(analysis)
                } catch (e: Exception) {
                    // Skip malformed classes but continue analysis
                    println("⚠️  Skipping Java class ${classDecl.nameAsString}: ${e.message}")
                }
            }
        }
        
        return analyses
    }
    
    /**
     * Analyzes a single Java class using the extracted utility classes.
     */
    private fun analyzeJavaClass(classDecl: ClassOrInterfaceDeclaration, fileName: String, allClasses: List<ClassOrInterfaceDeclaration>): ClassAnalysis {
        val className = classDecl.nameAsString
        
        // Extract method-property relationships for LCOM calculation
        val methodPropertyMap = extractJavaMethodPropertyRelationships(classDecl)
        
        // Calculate LCOM using utility class
        val lcom = LcomCalculator.calculateLcom(methodPropertyMap)
        
        // Calculate complexity for all methods using utility class
        val methods = classDecl.getMethods()
        val methodComplexities = methods.map { method ->
            val complexity = ComplexityCalculator.calculateJavaCyclomaticComplexity(method)
            val lineCount = method.toString().split('\n').size
            MethodComplexity(method.nameAsString, complexity, lineCount)
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
        val suggestions = SuggestionGenerator.generateJavaSuggestions(lcom, methods.size, classDecl.getFields().size, complexityAnalysis)
        
        return BackwardCompatibilityHelper.createEnhancedJavaClassAnalysis(
            classDecl = classDecl,
            allJavaClasses = allClasses,
            fileName = fileName,
            lcom = lcom,
            methodCount = methods.size,
            propertyCount = classDecl.getFields().size,
            methodDetails = methodPropertyMap,
            suggestions = suggestions,
            complexity = complexityAnalysis
        )
    }
    
    /**
     * Extracts method-property relationships for Java classes.
     */
    private fun extractJavaMethodPropertyRelationships(classDecl: ClassOrInterfaceDeclaration): Map<String, Set<String>> {
        val methodPropertyMap = mutableMapOf<String, Set<String>>()
        val classFields = classDecl.getFields()
            .flatMap { field -> field.getVariables().map { it.nameAsString } }
            .toSet()
        
        val methods = classDecl.getMethods()
        
        for (method in methods) {
            val methodName = method.nameAsString
            val usedFields = mutableSetOf<String>()
            
            // Simple field usage detection - look for field names in method body
            val methodText = method.toString()
            for (field in classFields) {
                if (methodText.contains(field)) {
                    usedFields.add(field)
                }
            }
            
            methodPropertyMap[methodName] = usedFields
        }
        
        return methodPropertyMap
    }
    
}