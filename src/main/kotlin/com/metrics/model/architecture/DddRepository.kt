package com.metrics.model.architecture

data class DddRepository(
    val className: String,
    val fileName: String,
    val isInterface: Boolean,
    val hasDataAccess: Boolean,
    val crudMethods: List<String>,
    val confidence: Double
)