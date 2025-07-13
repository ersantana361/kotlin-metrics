package com.metrics.model.architecture

data class DddAggregate(
    val rootEntity: String,
    val relatedEntities: List<String>,
    val confidence: Double
)