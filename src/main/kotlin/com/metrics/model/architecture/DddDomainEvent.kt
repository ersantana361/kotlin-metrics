package com.metrics.model.architecture

data class DddDomainEvent(
    val className: String,
    val fileName: String,
    val isEvent: Boolean,
    val confidence: Double
)