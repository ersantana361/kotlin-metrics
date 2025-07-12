package com.metrics.model.architecture

import com.metrics.model.common.CycleSeverity

data class DependencyCycle(
    val nodes: List<String>,
    val severity: CycleSeverity
)