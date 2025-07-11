package com.metrics.model.architecture

import com.metrics.model.common.DependencyType

data class DependencyEdge(
    val fromId: String,
    val toId: String,
    val dependencyType: DependencyType,
    val strength: Int
)