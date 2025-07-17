package com.metrics.analyzer

import com.metrics.analyzer.kotlin.KotlinCodeAnalyzer
import com.metrics.analyzer.java.JavaCodeAnalyzer
import com.metrics.util.*
import java.io.File
import java.nio.file.Paths

/**
 * Enhanced PR diff analyzer that provides comprehensive analysis with full source context.
 * Integrates all analysis components to provide deep insights into code changes.
 */
class EnhancedPRDiffAnalyzer(
    private val projectRoot: File
) {
    
    private val fileResolver = FileResolver(projectRoot.toPath())
    private val diffParser = DiffParser()
    private val sourceContextLoader = SourceContextLoader()
    private val kotlinAnalyzer = KotlinCodeAnalyzer()
    private val javaAnalyzer = JavaCodeAnalyzer()
    private val impactAnalyzer = ImpactAnalyzer(sourceContextLoader, projectRoot)
    private val semanticChangeAnalyzer = SemanticChangeAnalyzer(sourceContextLoader)
    private val enhancedVersionAnalyzer = EnhancedVersionAnalyzer(
        sourceContextLoader, 
        kotlinAnalyzer, 
        javaAnalyzer, 
        impactAnalyzer
    )
    
    /**
     * Analyzes a PR diff with complete source context and impact assessment.
     * 
     * @param diffFile The diff file to analyze
     * @param options Analysis options
     * @return EnhancedPRDiffResult with comprehensive analysis
     */
    fun analyzePRDiff(
        diffFile: File,
        options: PRDiffAnalysisOptions = PRDiffAnalysisOptions()
    ): EnhancedPRDiffResult {
        
        // Parse the diff
        val parsedDiff = diffParser.parse(diffFile)
        
        // Resolve changed files to actual source files
        val resolvedFiles = resolveChangedFiles(parsedDiff)
        
        // Load all source files for context
        val allSourceFiles = fileResolver.findSourceFiles(listOf("kt", "java"))
        
        // Create before and after versions
        val beforeFiles = createBeforeVersion(resolvedFiles, parsedDiff)
        val afterFiles = resolvedFiles.map { it.afterFile }.filterNotNull()
        
        // Analyze both versions
        val beforeAnalysis = if (beforeFiles.isNotEmpty()) {
            enhancedVersionAnalyzer.analyzeVersion(beforeFiles)
        } else null
        
        val afterAnalysis = enhancedVersionAnalyzer.analyzeVersion(
            afterFiles, 
            changedFiles = afterFiles
        )
        
        // Perform semantic change analysis
        val semanticChanges = semanticChangeAnalyzer.analyzeSemanticChanges(
            beforeFiles, 
            afterFiles
        )
        
        // Generate comparison metrics
        val metricsComparison = generateMetricsComparison(beforeAnalysis, afterAnalysis)
        
        // Create comprehensive result
        return EnhancedPRDiffResult(
            parsedDiff = parsedDiff,
            resolvedFiles = resolvedFiles,
            beforeAnalysis = beforeAnalysis,
            afterAnalysis = afterAnalysis,
            semanticChanges = semanticChanges,
            metricsComparison = metricsComparison,
            options = options
        )
    }
    
    /**
     * Resolves changed files from diff to actual source files.
     */
    private fun resolveChangedFiles(parsedDiff: ParsedDiff): List<ResolvedFileChange> {
        return parsedDiff.fileChanges
            .filter { it.isSupportedFile() }
            .map { fileChange ->
                val beforeFile = if (!fileChange.isNewFile()) {
                    fileResolver.resolveFile(fileChange.originalPath)
                } else null
                
                val afterFile = if (!fileChange.isDeletedFile()) {
                    fileResolver.resolveFile(fileChange.newPath)
                } else null
                
                ResolvedFileChange(
                    originalChange = fileChange,
                    beforeFile = beforeFile,
                    afterFile = afterFile,
                    resolved = beforeFile != null || afterFile != null
                )
            }
            .filter { it.resolved }
    }
    
    /**
     * Creates the before version by reconstructing original files.
     */
    private fun createBeforeVersion(
        resolvedFiles: List<ResolvedFileChange>,
        parsedDiff: ParsedDiff
    ): List<File> {
        return resolvedFiles.mapNotNull { resolved ->
            if (resolved.originalChange.isNewFile()) {
                null // No before version for new files
            } else {
                resolved.beforeFile ?: resolved.afterFile?.let { afterFile ->
                    // Try to reconstruct before version from diff
                    reconstructBeforeVersion(afterFile, resolved.originalChange)
                }
            }
        }
    }
    
    /**
     * Reconstructs the before version of a file from the after version and diff.
     */
    private fun reconstructBeforeVersion(afterFile: File, fileChange: FileChange): File? {
        // This is a simplified implementation
        // In practice, you'd need to reverse-apply the diff
        return afterFile
    }
    
    /**
     * Generates metrics comparison between before and after versions.
     */
    private fun generateMetricsComparison(
        beforeAnalysis: EnhancedVersionAnalysis?,
        afterAnalysis: EnhancedVersionAnalysis
    ): MetricsComparison {
        
        val improvements = mutableListOf<MetricImprovement>()
        val regressions = mutableListOf<MetricRegression>()
        
        if (beforeAnalysis != null) {
            // Compare class-level metrics
            compareClassMetrics(beforeAnalysis, afterAnalysis, improvements, regressions)
            
            // Compare project-level metrics
            compareProjectMetrics(beforeAnalysis, afterAnalysis, improvements, regressions)
        }
        
        return MetricsComparison(
            improvements = improvements,
            regressions = regressions,
            overallImpact = calculateOverallImpact(improvements, regressions)
        )
    }
    
    /**
     * Compares class-level metrics between versions.
     */
    private fun compareClassMetrics(
        beforeAnalysis: EnhancedVersionAnalysis,
        afterAnalysis: EnhancedVersionAnalysis,
        improvements: MutableList<MetricImprovement>,
        regressions: MutableList<MetricRegression>
    ) {
        
        val beforeClasses = beforeAnalysis.classAnalyses.associateBy { it.originalAnalysis.className }
        val afterClasses = afterAnalysis.classAnalyses.associateBy { it.originalAnalysis.className }
        
        afterClasses.forEach { (className, afterClass) ->
            beforeClasses[className]?.let { beforeClass ->
                // Compare LCOM
                val lcomBefore = beforeClass.originalAnalysis.lcom
                val lcomAfter = afterClass.originalAnalysis.lcom
                
                if (lcomBefore != lcomAfter) {
                    val change = lcomAfter - lcomBefore
                    if (change < 0) {
                        improvements.add(MetricImprovement(
                            className = className,
                            metricName = "LCOM",
                            beforeValue = lcomBefore.toDouble(),
                            afterValue = lcomAfter.toDouble(),
                            improvementPercentage = (change.toDouble() / lcomBefore.toDouble()) * 100
                        ))
                    } else {
                        regressions.add(MetricRegression(
                            className = className,
                            metricName = "LCOM",
                            beforeValue = lcomBefore.toDouble(),
                            afterValue = lcomAfter.toDouble(),
                            regressionPercentage = (change.toDouble() / lcomBefore.toDouble()) * 100
                        ))
                    }
                }
                
                // Compare quality scores
                val qualityBefore = beforeClass.originalAnalysis.qualityScore?.overall ?: 0.0
                val qualityAfter = afterClass.originalAnalysis.qualityScore?.overall ?: 0.0
                
                if (qualityBefore != qualityAfter) {
                    val change = qualityAfter - qualityBefore
                    if (change > 0) {
                        improvements.add(MetricImprovement(
                            className = className,
                            metricName = "Quality Score",
                            beforeValue = qualityBefore,
                            afterValue = qualityAfter,
                            improvementPercentage = (change / qualityBefore) * 100
                        ))
                    } else {
                        regressions.add(MetricRegression(
                            className = className,
                            metricName = "Quality Score",
                            beforeValue = qualityBefore,
                            afterValue = qualityAfter,
                            regressionPercentage = (change / qualityBefore) * 100
                        ))
                    }
                }
            }
        }
    }
    
    /**
     * Compares project-level metrics between versions.
     */
    private fun compareProjectMetrics(
        beforeAnalysis: EnhancedVersionAnalysis,
        afterAnalysis: EnhancedVersionAnalysis,
        improvements: MutableList<MetricImprovement>,
        regressions: MutableList<MetricRegression>
    ) {
        
        // Compare total files
        if (beforeAnalysis.totalFiles != afterAnalysis.totalFiles) {
            val change = afterAnalysis.totalFiles - beforeAnalysis.totalFiles
            if (change > 0) {
                improvements.add(MetricImprovement(
                    className = "PROJECT",
                    metricName = "Total Files",
                    beforeValue = beforeAnalysis.totalFiles.toDouble(),
                    afterValue = afterAnalysis.totalFiles.toDouble(),
                    improvementPercentage = (change.toDouble() / beforeAnalysis.totalFiles.toDouble()) * 100
                ))
            }
        }
        
        // Compare impact metrics
        afterAnalysis.impactAnalysis?.let { impact ->
            improvements.add(MetricImprovement(
                className = "PROJECT",
                metricName = "Impact Analysis",
                beforeValue = 0.0,
                afterValue = impact.impactMetrics.impactPercentage,
                improvementPercentage = 0.0
            ))
        }
    }
    
    /**
     * Calculates overall impact of changes.
     */
    private fun calculateOverallImpact(
        improvements: List<MetricImprovement>,
        regressions: List<MetricRegression>
    ): OverallImpact {
        
        val totalImprovements = improvements.size
        val totalRegressions = regressions.size
        val netImpact = totalImprovements - totalRegressions
        
        val impactLevel = when {
            netImpact > 5 -> ImpactLevel.HIGH
            netImpact > 2 -> ImpactLevel.MEDIUM
            netImpact > 0 -> ImpactLevel.LOW
            netImpact == 0 -> ImpactLevel.MINIMAL
            else -> ImpactLevel.HIGH // Negative impact
        }
        
        return OverallImpact(
            totalImprovements = totalImprovements,
            totalRegressions = totalRegressions,
            netImpact = netImpact,
            impactLevel = impactLevel
        )
    }
}

/**
 * Options for PR diff analysis.
 */
data class PRDiffAnalysisOptions(
    val includeTests: Boolean = false,
    val contextLines: Int = 3,
    val ignoreWhitespace: Boolean = true,
    val focusOnComplexity: Boolean = false,
    val focusOnCoupling: Boolean = false,
    val minImprovementThreshold: Double = 5.0
)

/**
 * Result of enhanced PR diff analysis.
 */
data class EnhancedPRDiffResult(
    val parsedDiff: ParsedDiff,
    val resolvedFiles: List<ResolvedFileChange>,
    val beforeAnalysis: EnhancedVersionAnalysis?,
    val afterAnalysis: EnhancedVersionAnalysis,
    val semanticChanges: SemanticChangeAnalysis,
    val metricsComparison: MetricsComparison,
    val options: PRDiffAnalysisOptions
)

/**
 * Resolved file change with actual file references.
 */
data class ResolvedFileChange(
    val originalChange: FileChange,
    val beforeFile: File?,
    val afterFile: File?,
    val resolved: Boolean
)

/**
 * Comparison of metrics between versions.
 */
data class MetricsComparison(
    val improvements: List<MetricImprovement>,
    val regressions: List<MetricRegression>,
    val overallImpact: OverallImpact
)

/**
 * Metric improvement between versions.
 */
data class MetricImprovement(
    val className: String,
    val metricName: String,
    val beforeValue: Double,
    val afterValue: Double,
    val improvementPercentage: Double
)

/**
 * Metric regression between versions.
 */
data class MetricRegression(
    val className: String,
    val metricName: String,
    val beforeValue: Double,
    val afterValue: Double,
    val regressionPercentage: Double
)

/**
 * Overall impact assessment.
 */
data class OverallImpact(
    val totalImprovements: Int,
    val totalRegressions: Int,
    val netImpact: Int,
    val impactLevel: ImpactLevel
)