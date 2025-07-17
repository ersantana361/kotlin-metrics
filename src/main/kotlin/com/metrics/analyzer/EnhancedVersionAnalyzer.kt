package com.metrics.analyzer

import com.metrics.analyzer.kotlin.KotlinCodeAnalyzer
import com.metrics.analyzer.java.JavaCodeAnalyzer
import com.metrics.model.analysis.ClassAnalysis
import com.metrics.model.analysis.ProjectReport
import com.metrics.model.architecture.ArchitectureAnalysis
import com.metrics.util.SourceContext
import com.metrics.util.SourceContextLoader
import com.metrics.util.SourceLanguage
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Enhanced version analyzer that provides full-context analysis of code versions.
 * Extends the existing analyzers with complete source file context and impact analysis.
 */
class EnhancedVersionAnalyzer(
    private val sourceContextLoader: SourceContextLoader,
    private val kotlinAnalyzer: KotlinCodeAnalyzer,
    private val javaAnalyzer: JavaCodeAnalyzer,
    private val impactAnalyzer: ImpactAnalyzer
) {
    
    /**
     * Analyzes a complete version of the codebase with full context.
     * 
     * @param sourceFiles All source files in the version
     * @param changedFiles Files that were changed (for impact analysis)
     * @return EnhancedVersionAnalysis with complete metrics and impact assessment
     */
    fun analyzeVersion(
        sourceFiles: List<File>,
        changedFiles: List<File> = emptyList()
    ): EnhancedVersionAnalysis {
        
        // Load source contexts for all files
        val sourceContexts = sourceContextLoader.loadMultipleContexts(sourceFiles)
        
        // Separate by language
        val kotlinFiles = mutableListOf<File>()
        val javaFiles = mutableListOf<File>()
        
        sourceContexts.forEach { (filePath, context) ->
            when (context.language) {
                SourceLanguage.KOTLIN -> kotlinFiles.add(context.file)
                SourceLanguage.JAVA -> javaFiles.add(context.file)
            }
        }
        
        // Perform traditional analysis
        val kotlinAnalysis = if (kotlinFiles.isNotEmpty()) {
            kotlinAnalyzer.analyzeFiles(kotlinFiles)
        } else {
            emptyList()
        }
        
        val javaAnalysis = if (javaFiles.isNotEmpty()) {
            javaAnalyzer.analyzeFiles(javaFiles)
        } else {
            emptyList()
        }
        
        val allClassAnalyses = kotlinAnalysis + javaAnalysis
        
        // Enhanced context analysis
        val enhancedAnalyses = enhanceClassAnalyses(allClassAnalyses, sourceContexts)
        
        // Impact analysis (if changed files provided)
        val impactAnalysis = if (changedFiles.isNotEmpty()) {
            impactAnalyzer.analyzeImpact(changedFiles, sourceFiles)
        } else {
            null
        }
        
        // Generate project report
        val projectReport = generateProjectReport(enhancedAnalyses, impactAnalysis)
        
        return EnhancedVersionAnalysis(
            classAnalyses = enhancedAnalyses,
            projectReport = projectReport,
            impactAnalysis = impactAnalysis,
            sourceContexts = sourceContexts,
            totalFiles = sourceFiles.size,
            kotlinFiles = kotlinFiles.size,
            javaFiles = javaFiles.size
        )
    }
    
    /**
     * Enhances class analyses with full source context information.
     */
    private fun enhanceClassAnalyses(
        classAnalyses: List<ClassAnalysis>,
        sourceContexts: Map<String, SourceContext>
    ): List<EnhancedClassAnalysis> {
        
        return classAnalyses.map { classAnalysis ->
            val sourceContext = sourceContexts[classAnalysis.fileName]
            
            EnhancedClassAnalysis(
                originalAnalysis = classAnalysis,
                sourceContext = sourceContext,
                methodSignatures = sourceContext?.let { 
                    sourceContextLoader.extractMethodSignatures(it) 
                } ?: emptyList(),
                dependencies = sourceContext?.let { 
                    sourceContextLoader.extractDependencies(it) 
                } ?: emptyList(),
                fullSourceContent = sourceContext?.content ?: "",
                contextMetrics = calculateContextMetrics(classAnalysis, sourceContext)
            )
        }
    }
    
    /**
     * Calculates additional metrics based on full source context.
     */
    private fun calculateContextMetrics(
        classAnalysis: ClassAnalysis,
        sourceContext: SourceContext?
    ): ContextMetrics {
        
        if (sourceContext == null) {
            return ContextMetrics(
                linesOfCode = 0,
                commentLines = 0,
                blankLines = 0,
                commentRatio = 0.0,
                averageMethodLength = 0.0,
                publicMethodCount = 0,
                privateMethodCount = 0
            )
        }
        
        val lines = sourceContext.content.lines()
        val linesOfCode = lines.size
        val commentLines = lines.count { line ->
            val trimmed = line.trim()
            trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")
        }
        val blankLines = lines.count { it.trim().isEmpty() }
        val commentRatio = if (linesOfCode > 0) commentLines.toDouble() / linesOfCode else 0.0
        
        val methodSignatures = sourceContextLoader.extractMethodSignatures(sourceContext)
        val averageMethodLength = if (methodSignatures.isNotEmpty()) {
            // This is a simplified calculation - would need more detailed analysis
            (linesOfCode - commentLines - blankLines).toDouble() / methodSignatures.size
        } else {
            0.0
        }
        
        val publicMethodCount = methodSignatures.count { it.visibility == "public" }
        val privateMethodCount = methodSignatures.count { it.visibility == "private" }
        
        return ContextMetrics(
            linesOfCode = linesOfCode,
            commentLines = commentLines,
            blankLines = blankLines,
            commentRatio = commentRatio,
            averageMethodLength = averageMethodLength,
            publicMethodCount = publicMethodCount,
            privateMethodCount = privateMethodCount
        )
    }
    
    /**
     * Generates a comprehensive project report.
     */
    private fun generateProjectReport(
        enhancedAnalyses: List<EnhancedClassAnalysis>,
        impactAnalysis: ImpactAnalysis?
    ): ProjectReport {
        
        val classAnalyses = enhancedAnalyses.map { it.originalAnalysis }
        
        // Generate summary
        val summary = generateSummary(enhancedAnalyses, impactAnalysis)
        
        // This would need to be implemented with proper architecture analysis
        // For now, create a basic architecture analysis
        val architectureAnalysis = ArchitectureAnalysis(
            dddPatterns = com.metrics.model.architecture.DddPatternAnalysis(
                entities = emptyList(),
                valueObjects = emptyList(),
                services = emptyList(),
                repositories = emptyList(),
                aggregates = emptyList(),
                domainEvents = emptyList()
            ),
            layeredArchitecture = com.metrics.model.architecture.LayeredArchitectureAnalysis(
                layers = emptyList(),
                violations = emptyList()
            ),
            dependencyGraph = com.metrics.model.architecture.DependencyGraph(
                nodes = emptyList(),
                edges = emptyList(),
                cycles = emptyList()
            )
        )
        
        return ProjectReport(
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            classes = classAnalyses,
            summary = summary,
            architectureAnalysis = architectureAnalysis
        )
    }
    
    /**
     * Generates a summary of the analysis including impact information.
     */
    private fun generateSummary(
        enhancedAnalyses: List<EnhancedClassAnalysis>,
        impactAnalysis: ImpactAnalysis?
    ): String {
        val totalClasses = enhancedAnalyses.size
        val avgQualityScore = enhancedAnalyses.map { it.originalAnalysis.qualityScore?.overall ?: 0.0 }.average()
        val highRiskClasses = enhancedAnalyses.count { 
            it.originalAnalysis.riskAssessment?.riskLevel == com.metrics.model.analysis.RiskLevel.HIGH 
        }
        
        val summary = StringBuilder()
        summary.append("Enhanced Analysis Summary:\n")
        summary.append("Total Classes: $totalClasses\n")
        summary.append("Average Quality Score: ${"%.2f".format(avgQualityScore)}\n")
        summary.append("High Risk Classes: $highRiskClasses\n")
        
        impactAnalysis?.let { impact ->
            summary.append("\nImpact Analysis:\n")
            summary.append("Directly Affected Files: ${impact.directlyAffectedFiles.size}\n")
            summary.append("Indirectly Affected Files: ${impact.indirectlyAffectedFiles.size}\n")
            summary.append("Impact Percentage: ${"%.1f".format(impact.impactMetrics.impactPercentage)}%\n")
            summary.append("Risk Level: ${impact.impactMetrics.riskLevel}\n")
        }
        
        return summary.toString()
    }
}

/**
 * Enhanced version analysis result with full context.
 */
data class EnhancedVersionAnalysis(
    val classAnalyses: List<EnhancedClassAnalysis>,
    val projectReport: ProjectReport,
    val impactAnalysis: ImpactAnalysis?,
    val sourceContexts: Map<String, SourceContext>,
    val totalFiles: Int,
    val kotlinFiles: Int,
    val javaFiles: Int
)

/**
 * Enhanced class analysis with full source context.
 */
data class EnhancedClassAnalysis(
    val originalAnalysis: ClassAnalysis,
    val sourceContext: SourceContext?,
    val methodSignatures: List<com.metrics.util.MethodSignature>,
    val dependencies: List<com.metrics.util.ClassDependency>,
    val fullSourceContent: String,
    val contextMetrics: ContextMetrics
)

/**
 * Additional metrics calculated from full source context.
 */
data class ContextMetrics(
    val linesOfCode: Int,
    val commentLines: Int,
    val blankLines: Int,
    val commentRatio: Double,
    val averageMethodLength: Double,
    val publicMethodCount: Int,
    val privateMethodCount: Int
)