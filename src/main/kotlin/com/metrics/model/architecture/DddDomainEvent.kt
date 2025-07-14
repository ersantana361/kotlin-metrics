package com.metrics.model.architecture

data class DddDomainEvent(
    val className: String,
    val fileName: String,
    val isEvent: Boolean,
    val isImmutable: Boolean,
    val hasEventNaming: Boolean,
    val hasTimestamp: Boolean,
    val confidence: Double
)