package com.metrics.model.architecture

// Simplified DDD classes for Phase 1 - these will be properly implemented in Phase 2

data class DddValueObject(
    val className: String,
    val fileName: String,
    val isImmutable: Boolean,
    val hasValueEquality: Boolean,
    val properties: List<String>,
    val confidence: Double
)

data class DddService(
    val className: String,
    val fileName: String,
    val isStateless: Boolean,
    val hasDomainLogic: Boolean,
    val methods: List<String>,
    val confidence: Double
)

data class DddRepository(
    val className: String,
    val fileName: String,
    val isInterface: Boolean,
    val hasDataAccess: Boolean,
    val crudMethods: List<String>,
    val confidence: Double
)

data class DddAggregate(
    val className: String,
    val fileName: String,
    val confidence: Double
)

data class DependencyEdge(
    val fromId: String,
    val toId: String,
    val dependencyType: DependencyType,
    val strength: Int
)

data class DependencyCycle(
    val nodes: List<String>,
    val severity: String
)

data class PackageAnalysis(
    val packageName: String,
    val classCount: Int
)

enum class DependencyType {
    INHERITANCE, COMPOSITION, USAGE, INTERFACE_IMPLEMENTATION
}