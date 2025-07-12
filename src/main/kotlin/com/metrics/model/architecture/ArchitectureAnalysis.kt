package com.metrics.model.architecture

data class ArchitectureAnalysis(
    val dddPatterns: DddPatternAnalysis,
    val layeredArchitecture: LayeredArchitectureAnalysis,
    val dependencyGraph: DependencyGraph
)