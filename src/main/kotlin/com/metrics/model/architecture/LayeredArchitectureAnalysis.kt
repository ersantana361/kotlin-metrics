package com.metrics.model.architecture

import com.metrics.model.common.ArchitecturePattern

data class LayeredArchitectureAnalysis(
    val layers: List<ArchitectureLayer>,
    val dependencies: List<LayerDependency>,
    val violations: List<ArchitectureViolation>,
    val pattern: ArchitecturePattern
)