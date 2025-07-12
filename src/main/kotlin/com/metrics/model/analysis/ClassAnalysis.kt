package com.metrics.model.analysis

data class ClassAnalysis(
    val className: String,
    val fileName: String,
    val lcom: Int,
    val methodCount: Int,
    val propertyCount: Int,
    val methodDetails: Map<String, Set<String>>,
    val suggestions: List<Suggestion>,
    val complexity: ComplexityAnalysis,
    // Enhanced CK metrics
    val ckMetrics: CkMetrics,
    val qualityScore: QualityScore,
    val riskAssessment: RiskAssessment
)