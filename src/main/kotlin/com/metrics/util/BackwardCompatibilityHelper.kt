package com.metrics.util

import com.metrics.model.analysis.*
import com.metrics.model.architecture.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration

/**
 * Helper for maintaining backward compatibility during Phase 1 transition.
 */
object BackwardCompatibilityHelper {
    
    /**
     * Creates enhanced ClassAnalysis from legacy parameters.
     */
    fun createEnhancedClassAnalysis(
        className: String,
        fileName: String,
        lcom: Int,
        methodCount: Int,
        propertyCount: Int,
        methodDetails: Map<String, Set<String>>,
        suggestions: List<Suggestion>,
        complexity: ComplexityAnalysis
    ): ClassAnalysis {
        // Create default CK metrics
        val ckMetrics = QualityScoreCalculator.createDefaultCkMetrics(lcom, complexity.totalComplexity)
        
        // Calculate quality score
        val qualityScore = QualityScoreCalculator.calculateQualityScore(ckMetrics)
        
        // Assess risk
        val riskAssessment = QualityScoreCalculator.assessRisk(ckMetrics, qualityScore)
        
        return ClassAnalysis(
            className = className,
            fileName = fileName,
            lcom = lcom,
            methodCount = methodCount,
            propertyCount = propertyCount,
            methodDetails = methodDetails,
            suggestions = suggestions,
            complexity = complexity,
            ckMetrics = ckMetrics,
            qualityScore = qualityScore,
            riskAssessment = riskAssessment
        )
    }
    
    /**
     * Creates enhanced ClassAnalysis with complete CK metrics for Kotlin classes.
     */
    fun createEnhancedKotlinClassAnalysis(
        classOrObject: KtClassOrObject,
        allKotlinClasses: List<KtClassOrObject>,
        fileName: String,
        lcom: Int,
        methodCount: Int,
        propertyCount: Int,
        methodDetails: Map<String, Set<String>>,
        suggestions: List<Suggestion>,
        complexity: ComplexityAnalysis
    ): ClassAnalysis {
        val className = classOrObject.name ?: "Unknown"
        
        // Create complete CK metrics
        val ckMetrics = QualityScoreCalculator.createCompleteCkMetrics(
            classOrObject, allKotlinClasses, lcom, complexity.totalComplexity
        )
        
        // Calculate quality score
        val qualityScore = QualityScoreCalculator.calculateQualityScore(ckMetrics)
        
        // Assess risk
        val riskAssessment = QualityScoreCalculator.assessRisk(ckMetrics, qualityScore)
        
        return ClassAnalysis(
            className = className,
            fileName = fileName,
            lcom = lcom,
            methodCount = methodCount,
            propertyCount = propertyCount,
            methodDetails = methodDetails,
            suggestions = suggestions,
            complexity = complexity,
            ckMetrics = ckMetrics,
            qualityScore = qualityScore,
            riskAssessment = riskAssessment
        )
    }
    
    /**
     * Creates enhanced ClassAnalysis with complete CK metrics for Java classes.
     */
    fun createEnhancedJavaClassAnalysis(
        classDecl: ClassOrInterfaceDeclaration,
        allJavaClasses: List<ClassOrInterfaceDeclaration>,
        fileName: String,
        lcom: Int,
        methodCount: Int,
        propertyCount: Int,
        methodDetails: Map<String, Set<String>>,
        suggestions: List<Suggestion>,
        complexity: ComplexityAnalysis
    ): ClassAnalysis {
        val className = classDecl.nameAsString
        
        // Create complete CK metrics
        val ckMetrics = QualityScoreCalculator.createCompleteJavaCkMetrics(
            classDecl, allJavaClasses, lcom, complexity.totalComplexity
        )
        
        // Calculate quality score
        val qualityScore = QualityScoreCalculator.calculateQualityScore(ckMetrics)
        
        // Assess risk
        val riskAssessment = QualityScoreCalculator.assessRisk(ckMetrics, qualityScore)
        
        return ClassAnalysis(
            className = className,
            fileName = fileName,
            lcom = lcom,
            methodCount = methodCount,
            propertyCount = propertyCount,
            methodDetails = methodDetails,
            suggestions = suggestions,
            complexity = complexity,
            ckMetrics = ckMetrics,
            qualityScore = qualityScore,
            riskAssessment = riskAssessment
        )
    }
    
    /**
     * Creates enhanced ProjectReport from legacy parameters.
     */
    fun createEnhancedProjectReport(
        timestamp: String,
        classes: List<ClassAnalysis>,
        summary: String,
        architectureAnalysis: ArchitectureAnalysis
    ): ProjectReport {
        // Calculate project-level quality score
        val projectQualityScore = calculateProjectQualityScore(classes)
        
        // Create package metrics (simplified for now)
        val packageMetrics = emptyList<PackageMetrics>()
        
        // Create coupling matrix (simplified for now)
        val couplingMatrix = emptyList<CouplingRelation>()
        
        // Collect risk assessments
        val riskAssessments = classes.map { it.riskAssessment }
        
        return ProjectReport(
            timestamp = timestamp,
            classes = classes,
            summary = summary,
            architectureAnalysis = architectureAnalysis,
            projectQualityScore = projectQualityScore,
            packageMetrics = packageMetrics,
            couplingMatrix = couplingMatrix,
            riskAssessments = riskAssessments
        )
    }
    
    private fun calculateProjectQualityScore(classes: List<ClassAnalysis>): QualityScore {
        if (classes.isEmpty()) {
            return QualityScore(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
        
        val avgCohesion = classes.map { it.qualityScore.cohesion }.average()
        val avgComplexity = classes.map { it.qualityScore.complexity }.average()
        val avgCoupling = classes.map { it.qualityScore.coupling }.average()
        val avgInheritance = classes.map { it.qualityScore.inheritance }.average()
        val avgArchitecture = classes.map { it.qualityScore.architecture }.average()
        val avgOverall = classes.map { it.qualityScore.overall }.average()
        
        return QualityScore(
            cohesion = avgCohesion,
            complexity = avgComplexity,
            coupling = avgCoupling,
            inheritance = avgInheritance,
            architecture = avgArchitecture,
            overall = avgOverall
        )
    }
}