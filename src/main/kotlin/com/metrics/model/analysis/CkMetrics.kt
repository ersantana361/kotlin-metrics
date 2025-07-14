package com.metrics.model.analysis

/**
 * Comprehensive Chidamber and Kemerer (CK) metrics for a class.
 */
data class CkMetrics(
    // Complexity Metrics
    val wmc: Int,                    // Weighted Methods per Class
    val cyclomaticComplexity: Int,   // Sum of method complexities (already have CC per method)
    
    // Coupling Metrics  
    val cbo: Int,                    // Coupling Between Objects
    val rfc: Int,                    // Response for a Class
    val ca: Int,                     // Afferent Coupling (incoming dependencies)
    val ce: Int,                     // Efferent Coupling (outgoing dependencies)
    
    // Inheritance Metrics
    val dit: Int,                    // Depth of Inheritance Tree
    val noc: Int,                    // Number of Children
    
    // Cohesion Metrics (using existing LCOM)
    val lcom: Int                    // Lack of Cohesion of Methods
)

/**
 * Quality score breakdown for different metric categories.
 */
data class QualityScore(
    val cohesion: Double,      // LCOM-based score (0-10)
    val complexity: Double,    // CC + WMC-based score (0-10)  
    val coupling: Double,      // CBO + RFC + CA + CE-based score (0-10)
    val inheritance: Double,   // DIT + NOC-based score (0-10)
    val architecture: Double,  // DDD + Layer compliance score (0-10)
    val overall: Double        // Weighted average (0-10)
) {
    /**
     * Gets quality level description for overall score.
     */
    fun getQualityLevel(): String = when {
        overall >= 8.0 -> "Excellent"
        overall >= 6.0 -> "Good" 
        overall >= 4.0 -> "Moderate"
        overall >= 2.0 -> "Poor"
        else -> "Critical"
    }
    
    /**
     * Gets quality emoji for overall score.
     */
    fun getQualityEmoji(): String = when {
        overall >= 8.0 -> "🟢"
        overall >= 6.0 -> "🟡" 
        overall >= 4.0 -> "🟠"
        overall >= 2.0 -> "🔴"
        else -> "🚨"
    }
}

/**
 * Risk assessment for a class based on multiple metrics.
 */
data class RiskAssessment(
    val level: RiskLevel,
    val reasons: List<String>,
    val impact: String,
    val priority: Int
)

enum class RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Package-level metrics aggregation.
 */
data class PackageMetrics(
    val packageName: String,
    val classCount: Int,
    val averageCkMetrics: CkMetrics,
    val qualityScore: QualityScore,
    val issues: List<String>
)

/**
 * Coupling matrix entry showing dependencies between classes.
 */
data class CouplingRelation(
    val fromClass: String,
    val toClass: String,
    val strength: Int,
    val type: CouplingType
)

enum class CouplingType {
    INHERITANCE, COMPOSITION, USAGE, INTERFACE_IMPLEMENTATION
}