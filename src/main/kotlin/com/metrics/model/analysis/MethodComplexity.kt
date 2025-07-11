package com.metrics.model.analysis

data class MethodComplexity(
    val methodName: String,
    val cyclomaticComplexity: Int,
    val lineCount: Int
)