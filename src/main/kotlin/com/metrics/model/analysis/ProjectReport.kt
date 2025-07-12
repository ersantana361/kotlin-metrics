package com.metrics.model.analysis

import com.metrics.model.architecture.ArchitectureAnalysis

data class ProjectReport(
    val timestamp: String,
    val classes: List<ClassAnalysis>,
    val summary: String,
    val architectureAnalysis: ArchitectureAnalysis,
    // Enhanced reporting
    val projectQualityScore: QualityScore,
    val packageMetrics: List<PackageMetrics>,
    val couplingMatrix: List<CouplingRelation>,
    val riskAssessments: List<RiskAssessment>
)