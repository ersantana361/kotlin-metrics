package com.metrics.util

import com.metrics.model.analysis.*
import org.jetbrains.kotlin.psi.KtClassOrObject
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration

/**
 * Utility for calculating composite quality scores and risk assessments.
 */
object QualityScoreCalculator {
    
    /**
     * Calculates quality score based on CK metrics.
     */
    fun calculateQualityScore(ckMetrics: CkMetrics): QualityScore {
        val cohesionScore = calculateCohesionScore(ckMetrics.lcom)
        val complexityScore = calculateComplexityScore(ckMetrics.wmc, ckMetrics.cyclomaticComplexity)
        val couplingScore = calculateCouplingScore(ckMetrics.cbo, ckMetrics.rfc, ckMetrics.ca, ckMetrics.ce)
        val inheritanceScore = calculateInheritanceScore(ckMetrics.dit, ckMetrics.noc)
        val architectureScore = 7.0 // Default until architecture analysis is enhanced
        
        val overall = (cohesionScore * 0.25 + complexityScore * 0.25 + 
                      couplingScore * 0.25 + inheritanceScore * 0.15 + 
                      architectureScore * 0.10)
        
        return QualityScore(
            cohesion = cohesionScore,
            complexity = complexityScore,
            coupling = couplingScore,
            inheritance = inheritanceScore,
            architecture = architectureScore,
            overall = overall
        )
    }
    
    /**
     * Assesses risk level based on quality scores and metrics.
     */
    fun assessRisk(ckMetrics: CkMetrics, qualityScore: QualityScore): RiskAssessment {
        val issues = mutableListOf<String>()
        var priority = 1
        
        // Check for critical issues
        if (ckMetrics.lcom > 10) {
            issues.add("Very poor cohesion (LCOM: ${ckMetrics.lcom})")
            priority = maxOf(priority, 4)
        }
        if (ckMetrics.wmc > 50) {
            issues.add("Extremely high complexity (WMC: ${ckMetrics.wmc})")
            priority = maxOf(priority, 4)
        }
        if (ckMetrics.cbo > 20) {
            issues.add("Excessive coupling (CBO: ${ckMetrics.cbo})")
            priority = maxOf(priority, 3)
        }
        if (ckMetrics.dit > 6) {
            issues.add("Deep inheritance (DIT: ${ckMetrics.dit})")
            priority = maxOf(priority, 2)
        }
        
        val level = when {
            qualityScore.overall < 3.0 -> RiskLevel.CRITICAL
            qualityScore.overall < 5.0 -> RiskLevel.HIGH
            qualityScore.overall < 7.0 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        
        val impact = when (level) {
            RiskLevel.CRITICAL -> "Severe impact on maintainability and reliability"
            RiskLevel.HIGH -> "High impact on code quality and development velocity"
            RiskLevel.MEDIUM -> "Moderate impact on maintainability"
            RiskLevel.LOW -> "Minimal impact on code quality"
        }
        
        return RiskAssessment(
            level = level,
            reasons = issues,
            impact = impact,
            priority = priority
        )
    }
    
    /**
     * Creates default CK metrics for backward compatibility.
     */
    fun createDefaultCkMetrics(lcom: Int, totalComplexity: Int): CkMetrics {
        return CkMetrics(
            wmc = totalComplexity,
            cyclomaticComplexity = totalComplexity,
            cbo = 0, // Will be calculated in Phase 2
            rfc = 0, // Will be calculated in Phase 2
            ca = 0,  // Will be calculated in Phase 2
            ce = 0,  // Will be calculated in Phase 2
            dit = 0, // Will be calculated in Phase 2
            noc = 0, // Will be calculated in Phase 2
            lcom = lcom
        )
    }
    
    /**
     * Creates complete CK metrics using all calculators.
     */
    fun createCompleteCkMetrics(
        classOrObject: KtClassOrObject,
        allKotlinClasses: List<KtClassOrObject>,
        lcom: Int,
        totalComplexity: Int
    ): CkMetrics {
        return CkMetrics(
            wmc = WmcCalculator.calculateWmc(classOrObject),
            cyclomaticComplexity = totalComplexity,
            cbo = CouplingCalculator.calculateCbo(classOrObject, allKotlinClasses),
            rfc = CouplingCalculator.calculateRfc(classOrObject),
            ca = CouplingCalculator.calculateCa(classOrObject, allKotlinClasses),
            ce = CouplingCalculator.calculateCe(classOrObject, allKotlinClasses),
            dit = InheritanceCalculator.calculateDit(classOrObject, allKotlinClasses),
            noc = InheritanceCalculator.calculateNoc(classOrObject, allKotlinClasses),
            lcom = lcom
        )
    }
    
    /**
     * Creates complete CK metrics for Java classes.
     */
    fun createCompleteJavaCkMetrics(
        classDecl: ClassOrInterfaceDeclaration,
        allJavaClasses: List<ClassOrInterfaceDeclaration>,
        lcom: Int,
        totalComplexity: Int
    ): CkMetrics {
        return CkMetrics(
            wmc = WmcCalculator.calculateJavaWmc(classDecl),
            cyclomaticComplexity = totalComplexity,
            cbo = CouplingCalculator.calculateJavaCbo(classDecl, allJavaClasses),
            rfc = CouplingCalculator.calculateJavaRfc(classDecl),
            ca = CouplingCalculator.calculateJavaCa(classDecl, allJavaClasses),
            ce = CouplingCalculator.calculateJavaCe(classDecl, allJavaClasses),
            dit = InheritanceCalculator.calculateJavaDit(classDecl, allJavaClasses),
            noc = InheritanceCalculator.calculateJavaNoc(classDecl, allJavaClasses),
            lcom = lcom
        )
    }
    
    private fun calculateCohesionScore(lcom: Int): Double {
        return when {
            lcom == 0 -> 10.0
            lcom <= 2 -> 8.0
            lcom <= 5 -> 6.0
            lcom <= 10 -> 4.0
            lcom <= 20 -> 2.0
            else -> 1.0
        }
    }
    
    private fun calculateComplexityScore(wmc: Int, cc: Int): Double {
        val avgComplexity = if (wmc > 0) cc.toDouble() / wmc else 0.0
        return when {
            avgComplexity <= 2 -> 10.0
            avgComplexity <= 5 -> 8.0
            avgComplexity <= 10 -> 6.0
            avgComplexity <= 15 -> 4.0
            avgComplexity <= 25 -> 2.0
            else -> 1.0
        }
    }
    
    private fun calculateCouplingScore(cbo: Int, rfc: Int, ca: Int, ce: Int): Double {
        val totalCoupling = cbo + (rfc / 5) + ca + ce
        return when {
            totalCoupling <= 5 -> 10.0
            totalCoupling <= 10 -> 8.0
            totalCoupling <= 20 -> 6.0
            totalCoupling <= 35 -> 4.0
            totalCoupling <= 50 -> 2.0
            else -> 1.0
        }
    }
    
    private fun calculateInheritanceScore(dit: Int, noc: Int): Double {
        return when {
            dit <= 2 && noc <= 5 -> 10.0
            dit <= 4 && noc <= 10 -> 8.0
            dit <= 6 && noc <= 15 -> 6.0
            dit <= 8 && noc <= 20 -> 4.0
            dit <= 10 && noc <= 30 -> 2.0
            else -> 1.0
        }
    }
}