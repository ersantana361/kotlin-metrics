package com.metrics.model.architecture

data class LayerDependency(
    val fromLayer: String,
    val toLayer: String,
    val dependencyCount: Int,
    val isValid: Boolean
)