package com.metrics.model.architecture

import com.metrics.model.common.NodeType

data class DependencyNode(
    val id: String,
    val className: String,
    val fileName: String,
    val packageName: String,
    val nodeType: NodeType,
    val layer: String?,
    val language: String = "Kotlin"
)