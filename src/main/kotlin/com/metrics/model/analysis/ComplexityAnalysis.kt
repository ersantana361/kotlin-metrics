package com.metrics.model.analysis

data class ComplexityAnalysis(
    val methods: List<MethodComplexity>,
    val totalComplexity: Int,
    val averageComplexity: Double,
    val maxComplexity: Int,
    val complexMethods: List<MethodComplexity> // CC > 10
)