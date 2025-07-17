package com.metrics.analyzer

import com.metrics.model.analysis.ClassAnalysis
import com.metrics.model.analysis.ProjectReport
import com.metrics.model.common.DependencyType
import com.metrics.util.SourceContext
import com.metrics.util.SourceContextLoader
import java.io.File

/**
 * Analyzes the impact of code changes on the broader codebase.
 * Identifies affected files, dependencies, and potential ripple effects.
 */
class ImpactAnalyzer(
    private val sourceContextLoader: SourceContextLoader,
    private val projectRoot: File
) {
    
    /**
     * Analyzes the impact of changes to specific files.
     * 
     * @param changedFiles List of files that have been changed
     * @param allSourceFiles All source files in the project
     * @return ImpactAnalysis containing impact assessment
     */
    fun analyzeImpact(changedFiles: List<File>, allSourceFiles: List<File>): ImpactAnalysis {
        val changedContexts = sourceContextLoader.loadMultipleContexts(changedFiles)
        val allContexts = sourceContextLoader.loadMultipleContexts(allSourceFiles)
        
        val directlyAffected = changedFiles.map { it.path }.toSet()
        val indirectlyAffected = mutableSetOf<String>()
        val dependencyImpacts = mutableListOf<DependencyImpact>()
        val methodImpacts = mutableListOf<MethodImpact>()
        
        // Analyze direct impacts
        changedContexts.forEach { (filePath, context) ->
            val impacts = analyzeFileImpact(context, allContexts)
            indirectlyAffected.addAll(impacts.affectedFiles)
            dependencyImpacts.addAll(impacts.dependencyImpacts)
            methodImpacts.addAll(impacts.methodImpacts)
        }
        
        // Analyze inheritance impacts
        val inheritanceImpacts = analyzeInheritanceImpact(changedContexts, allContexts)
        indirectlyAffected.addAll(inheritanceImpacts.affectedFiles)
        
        // Calculate impact metrics
        val impactMetrics = calculateImpactMetrics(
            directlyAffected = directlyAffected,
            indirectlyAffected = indirectlyAffected,
            totalFiles = allSourceFiles.size,
            dependencyImpacts = dependencyImpacts
        )
        
        return ImpactAnalysis(
            directlyAffectedFiles = directlyAffected.toList(),
            indirectlyAffectedFiles = indirectlyAffected.toList(),
            dependencyImpacts = dependencyImpacts,
            methodImpacts = methodImpacts,
            inheritanceImpacts = inheritanceImpacts,
            impactMetrics = impactMetrics
        )
    }
    
    /**
     * Analyzes the impact of a single file change.
     */
    private fun analyzeFileImpact(
        changedContext: SourceContext,
        allContexts: Map<String, SourceContext>
    ): FileImpactResult {
        val affectedFiles = mutableSetOf<String>()
        val dependencyImpacts = mutableListOf<DependencyImpact>()
        val methodImpacts = mutableListOf<MethodImpact>()
        
        val changedDependencies = sourceContextLoader.extractDependencies(changedContext)
        val changedMethods = sourceContextLoader.extractMethodSignatures(changedContext)
        
        // Find files that depend on the changed file
        allContexts.forEach { (filePath, context) ->
            if (filePath != changedContext.file.path) {
                val contextDependencies = sourceContextLoader.extractDependencies(context)
                val contextMethods = sourceContextLoader.extractMethodSignatures(context)
                
                // Check if this file depends on the changed file
                val dependsOnChanged = contextDependencies.any { dep ->
                    changedDependencies.any { changedDep ->
                        dep.className == changedDep.className
                    }
                }
                
                if (dependsOnChanged) {
                    affectedFiles.add(filePath)
                    
                    // Analyze specific dependency impacts
                    contextDependencies.forEach { dep ->
                        changedDependencies.forEach { changedDep ->
                            if (dep.className == changedDep.className) {
                                dependencyImpacts.add(DependencyImpact(
                                    affectedFile = filePath,
                                    dependencyType = mapUtilDependencyType(dep.type),
                                    dependencyName = dep.className,
                                    impactSeverity = calculateDependencyImpactSeverity(mapUtilDependencyType(dep.type))
                                ))
                            }
                        }
                    }
                }
                
                // Check for method usage impacts
                contextMethods.forEach { method ->
                    changedMethods.forEach { changedMethod ->
                        if (method.name == changedMethod.name && 
                            method.className == changedMethod.className) {
                            methodImpacts.add(MethodImpact(
                                affectedFile = filePath,
                                methodName = method.name,
                                className = method.className,
                                impactType = MethodImpactType.SIGNATURE_CHANGE,
                                severity = ImpactSeverity.MEDIUM
                            ))
                        }
                    }
                }
            }
        }
        
        return FileImpactResult(
            affectedFiles = affectedFiles,
            dependencyImpacts = dependencyImpacts,
            methodImpacts = methodImpacts
        )
    }
    
    /**
     * Analyzes inheritance-related impacts.
     */
    private fun analyzeInheritanceImpact(
        changedContexts: Map<String, SourceContext>,
        allContexts: Map<String, SourceContext>
    ): InheritanceImpactResult {
        val affectedFiles = mutableSetOf<String>()
        val impacts = mutableListOf<InheritanceImpact>()
        
        // This would need to be implemented with proper inheritance analysis
        // For now, return empty results
        return InheritanceImpactResult(
            affectedFiles = affectedFiles.toList(),
            impacts = impacts
        )
    }
    
    /**
     * Calculates overall impact metrics.
     */
    private fun calculateImpactMetrics(
        directlyAffected: Set<String>,
        indirectlyAffected: Set<String>,
        totalFiles: Int,
        dependencyImpacts: List<DependencyImpact>
    ): ImpactMetrics {
        val totalAffected = directlyAffected.size + indirectlyAffected.size
        val impactPercentage = (totalAffected.toDouble() / totalFiles.toDouble()) * 100
        
        val severityDistribution = dependencyImpacts.groupBy { it.impactSeverity }
            .mapValues { it.value.size }
        
        val riskLevel = when {
            impactPercentage > 50 -> ImpactRiskLevel.HIGH
            impactPercentage > 20 -> ImpactRiskLevel.MEDIUM
            impactPercentage > 5 -> ImpactRiskLevel.LOW
            else -> ImpactRiskLevel.MINIMAL
        }
        
        return ImpactMetrics(
            totalAffectedFiles = totalAffected,
            impactPercentage = impactPercentage,
            riskLevel = riskLevel,
            severityDistribution = severityDistribution
        )
    }
    
    /**
     * Calculates the severity of a dependency impact.
     */
    private fun calculateDependencyImpactSeverity(dependencyType: DependencyType): ImpactSeverity {
        return when (dependencyType) {
            DependencyType.INHERITANCE -> ImpactSeverity.HIGH
            DependencyType.COMPOSITION -> ImpactSeverity.MEDIUM
            DependencyType.USAGE -> ImpactSeverity.LOW
            DependencyType.ASSOCIATION -> ImpactSeverity.MINIMAL
        }
    }
    
    /**
     * Maps util DependencyType to model DependencyType.
     */
    private fun mapUtilDependencyType(utilType: com.metrics.util.DependencyType): DependencyType {
        return when (utilType) {
            com.metrics.util.DependencyType.INHERITANCE -> DependencyType.INHERITANCE
            com.metrics.util.DependencyType.COMPOSITION -> DependencyType.COMPOSITION
            com.metrics.util.DependencyType.USAGE -> DependencyType.USAGE
            com.metrics.util.DependencyType.IMPORT -> DependencyType.ASSOCIATION
        }
    }
}

/**
 * Result of impact analysis.
 */
data class ImpactAnalysis(
    val directlyAffectedFiles: List<String>,
    val indirectlyAffectedFiles: List<String>,
    val dependencyImpacts: List<DependencyImpact>,
    val methodImpacts: List<MethodImpact>,
    val inheritanceImpacts: InheritanceImpactResult,
    val impactMetrics: ImpactMetrics
)

/**
 * Impact analysis result for a single file.
 */
data class FileImpactResult(
    val affectedFiles: Set<String>,
    val dependencyImpacts: List<DependencyImpact>,
    val methodImpacts: List<MethodImpact>
)

/**
 * Impact on class dependencies.
 */
data class DependencyImpact(
    val affectedFile: String,
    val dependencyType: DependencyType,
    val dependencyName: String,
    val impactSeverity: ImpactSeverity
)

/**
 * Impact on method usage.
 */
data class MethodImpact(
    val affectedFile: String,
    val methodName: String,
    val className: String,
    val impactType: MethodImpactType,
    val severity: ImpactSeverity
)

/**
 * Impact on inheritance hierarchy.
 */
data class InheritanceImpactResult(
    val affectedFiles: List<String>,
    val impacts: List<InheritanceImpact>
)

/**
 * Specific inheritance impact.
 */
data class InheritanceImpact(
    val affectedFile: String,
    val impactType: InheritanceImpactType,
    val className: String,
    val severity: ImpactSeverity
)

/**
 * Overall impact metrics.
 */
data class ImpactMetrics(
    val totalAffectedFiles: Int,
    val impactPercentage: Double,
    val riskLevel: ImpactRiskLevel,
    val severityDistribution: Map<ImpactSeverity, Int>
)

/**
 * Severity levels for impact assessment.
 */
enum class ImpactSeverity {
    HIGH,
    MEDIUM,
    LOW,
    MINIMAL
}

/**
 * Risk levels for overall impact.
 */
enum class ImpactRiskLevel {
    HIGH,
    MEDIUM,
    LOW,
    MINIMAL
}

/**
 * Types of method impacts.
 */
enum class MethodImpactType {
    SIGNATURE_CHANGE,
    BEHAVIOR_CHANGE,
    VISIBILITY_CHANGE,
    REMOVAL
}

/**
 * Types of inheritance impacts.
 */
enum class InheritanceImpactType {
    PARENT_CHANGE,
    CHILD_IMPACT,
    INTERFACE_CHANGE
}