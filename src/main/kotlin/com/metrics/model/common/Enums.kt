package com.metrics.model.common

enum class ArchitecturePattern {
    LAYERED, HEXAGONAL, CLEAN, ONION, UNKNOWN
}

enum class LayerType {
    PRESENTATION, APPLICATION, DOMAIN, DATA, INFRASTRUCTURE
}

enum class ViolationType {
    LAYER_VIOLATION, DEPENDENCY_INVERSION, CIRCULAR_DEPENDENCY
}

enum class NodeType {
    CLASS, INTERFACE, ENUM, OBJECT
}