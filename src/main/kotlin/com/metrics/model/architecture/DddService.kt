package com.metrics.model.architecture

data class DddService(
    val className: String,
    val fileName: String,
    val isStateless: Boolean,
    val hasDomainLogic: Boolean,
    val methods: List<String>,
    val confidence: Double
)