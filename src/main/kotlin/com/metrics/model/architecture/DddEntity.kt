package com.metrics.model.architecture

data class DddEntity(
    val className: String,
    val fileName: String,
    val hasUniqueId: Boolean,
    val isMutable: Boolean,
    val idFields: List<String>,
    val confidence: Double
)