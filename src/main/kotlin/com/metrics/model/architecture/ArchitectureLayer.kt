package com.metrics.model.architecture

import com.metrics.model.common.LayerType

data class ArchitectureLayer(
    val name: String,
    val type: LayerType,
    val packages: List<String>,
    val classes: List<String>,
    val level: Int
)