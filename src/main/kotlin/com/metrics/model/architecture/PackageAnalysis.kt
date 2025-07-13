package com.metrics.model.architecture

data class PackageAnalysis(
    val packageName: String,
    val classes: List<String>,
    val dependencies: List<String>,
    val layer: String?,
    val cohesion: Double
)