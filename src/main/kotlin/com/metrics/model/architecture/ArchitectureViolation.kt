package com.metrics.model.architecture

import com.metrics.model.common.ViolationType

data class ArchitectureViolation(
    val fromClass: String,
    val toClass: String,
    val violationType: ViolationType,
    val suggestion: String
)