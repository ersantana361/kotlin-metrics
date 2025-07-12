package com.metrics.model.analysis

import com.metrics.model.architecture.ArchitectureAnalysis

data class ProjectReport(
    val timestamp: String,
    val classes: List<ClassAnalysis>,
    val summary: String,
    val architectureAnalysis: ArchitectureAnalysis
)