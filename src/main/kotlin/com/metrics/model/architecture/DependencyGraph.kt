package com.metrics.model.architecture

data class DependencyGraph(
    val nodes: List<DependencyNode>,
    val edges: List<DependencyEdge>,
    val cycles: List<DependencyCycle>,
    val packages: List<PackageAnalysis>
)