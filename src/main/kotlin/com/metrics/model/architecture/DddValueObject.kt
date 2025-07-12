package com.metrics.model.architecture

data class DddValueObject(
    val className: String,
    val fileName: String,
    val isImmutable: Boolean,
    val hasValueEquality: Boolean,
    val properties: List<String>,
    val confidence: Double
)