package com.metrics.report.html

import com.metrics.model.analysis.ClassAnalysis
import com.metrics.model.analysis.ProjectReport
import com.metrics.model.architecture.ArchitectureAnalysis
import com.metrics.report.ReportGenerator
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Generates interactive HTML reports for code analysis results.
 * Creates a comprehensive web-based dashboard with charts, tables, and interactive visualizations.
 */
class HtmlReportGenerator : ReportGenerator {
    
    override fun generate(report: ProjectReport) {
        generateHtmlReport(report.classes, report.architectureAnalysis)
    }
    
    /**
     * Main entry point for HTML report generation.
     * Creates a complete HTML report file with all analysis results.
     */
    private fun generateHtmlReport(analyses: List<ClassAnalysis>, architectureAnalysis: ArchitectureAnalysis) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val reportFile = File("kotlin-metrics-report.html")
        
        val html = buildString {
            append(generateHtmlHeader())
            append(generateHtmlBody(analyses, architectureAnalysis, timestamp))
            append(generateHtmlFooter())
        }
        
        reportFile.writeText(html)
        println("HTML report saved to: ${reportFile.absolutePath}")
    }
    
    /**
     * Generates the HTML document header with CSS dependencies and styling.
     */
    private fun generateHtmlHeader(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kotlin Metrics Report</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script src="https://d3js.org/d3.v7.min.js"></script>
    <style>
        body { background-color: #f8f9fa; }
        .metric-card { transition: transform 0.2s; }
        .metric-card:hover { transform: translateY(-2px); }
        .cohesion-excellent { border-left: 4px solid #28a745; }
        .cohesion-good { border-left: 4px solid #17a2b8; }
        .cohesion-moderate { border-left: 4px solid #ffc107; }
        .cohesion-poor { border-left: 4px solid #dc3545; }
        .chart-container { height: 400px; }
        .sortable { cursor: pointer; user-select: none; }
        .sortable:hover { background-color: #f8f9fa; }
        .sort-indicator { margin-left: 5px; opacity: 0.5; }
        .sort-indicator.active { opacity: 1; }
        .filter-buttons { margin-bottom: 20px; }
        .filter-btn { margin-right: 10px; }
        .filter-btn.active { box-shadow: 0 0 0 2px rgba(0,123,255,.5); }
        .table-row { transition: opacity 0.3s; }
        .table-row.filtered { display: none; }
        .suggestion-item { cursor: help; }
        .suggestion-item:hover { opacity: 0.8; }
        
        /* D3.js Dependency Graph Styles */
        #dependencyGraph { height: 600px; border: 1px solid #dee2e6; border-radius: 8px; background: white; }
        .node { cursor: pointer; }
        .node circle { stroke: #333; stroke-width: 2px; }
        .node text { font: 12px sans-serif; pointer-events: none; text-anchor: middle; }
        .link { stroke: #999; stroke-opacity: 0.6; }
        .link.cycle { stroke: #dc3545; stroke-width: 3px; stroke-opacity: 0.8; }
        .tooltip { position: absolute; text-align: center; padding: 8px; font: 12px sans-serif; background: rgba(0,0,0,0.8); color: white; border-radius: 4px; pointer-events: none; opacity: 0; }
        
        .layer-presentation { fill: #e3f2fd; }
        .layer-application { fill: #f3e5f5; }
        .layer-domain { fill: #e8f5e8; }
        .layer-data { fill: #fff3e0; }
        .layer-infrastructure { fill: #fce4ec; }
        .layer-unknown { fill: #f5f5f5; }
    </style>
</head>
<body>
"""
    
    /**
     * Generates the HTML document footer with JavaScript dependencies.
     */
    private fun generateHtmlFooter(): String = """
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
"""
    
    /**
     * Generates the complete HTML body content with all interactive components.
     * This is the main content generation method containing all charts, tables, and visualizations.
     */
    private fun generateHtmlBody(analyses: List<ClassAnalysis>, architectureAnalysis: ArchitectureAnalysis, timestamp: String): String {
        val cohesionDistribution = analyses.groupBy { 
            when (it.lcom) {
                0 -> "Excellent"
                in 1..2 -> "Good"
                in 3..5 -> "Moderate"
                else -> "Poor"
            }
        }
        
        val complexityDistribution = analyses.flatMap { it.complexity.methods }.groupBy {
            when (it.cyclomaticComplexity) {
                1 -> "Simple (1)"
                in 2..5 -> "Low (2-5)"
                in 6..10 -> "Moderate (6-10)"
                in 11..20 -> "High (11-20)"
                else -> "Very High (21+)"
            }
        }
        
        return """
<div class="container-fluid py-4">
    <div class="row">
        <div class="col-12">
            <h1 class="text-center mb-4">
                <i class="fas fa-chart-line"></i> Kotlin Metrics Analysis Report
            </h1>
            <p class="text-center text-muted">Generated: $timestamp</p>
        </div>
    </div>
    
    <!-- Summary Cards -->
    <div class="row mb-4">
        <div class="col-md-3">
            <div class="card metric-card">
                <div class="card-body text-center">
                    <h5 class="card-title">Total Classes</h5>
                    <h2 class="text-primary">${analyses.size}</h2>
                </div>
            </div>
        </div>
        <div class="col-md-2">
            <div class="card metric-card">
                <div class="card-body text-center">
                    <h5 class="card-title">Average LCOM</h5>
                    <h2 class="text-info">${if (analyses.isNotEmpty()) "%.2f".format(analyses.map { it.lcom }.average()) else "0"}</h2>
                </div>
            </div>
        </div>
        <div class="col-md-2">
            <div class="card metric-card">
                <div class="card-body text-center">
                    <h5 class="card-title">Average CC</h5>
                    <h2 class="text-info">${if (analyses.isNotEmpty()) "%.1f".format(analyses.map { it.complexity.averageComplexity }.average()) else "0"}</h2>
                </div>
            </div>
        </div>
        <div class="col-md-2">
            <div class="card metric-card">
                <div class="card-body text-center">
                    <h5 class="card-title">Complex Methods</h5>
                    <h2 class="text-warning">${analyses.sumOf { it.complexity.complexMethods.size }}</h2>
                </div>
            </div>
        </div>
        <div class="col-md-2">
            <div class="card metric-card">
                <div class="card-body text-center">
                    <h5 class="card-title">Excellent Classes</h5>
                    <h2 class="text-success">${cohesionDistribution["Excellent"]?.size ?: 0}</h2>
                </div>
            </div>
        </div>
        <div class="col-md-2">
            <div class="card metric-card">
                <div class="card-body text-center">
                    <h5 class="card-title">Needs Attention</h5>
                    <h2 class="text-danger">${cohesionDistribution["Poor"]?.size ?: 0}</h2>
                </div>
            </div>
        </div>
    </div>
    
    <!-- Tabs Navigation -->
    <ul class="nav nav-tabs" id="metricsTab" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link active" id="lcom-tab" data-bs-toggle="tab" data-bs-target="#lcom" type="button" role="tab">
                LCOM Analysis
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" id="complexity-tab" data-bs-toggle="tab" data-bs-target="#complexity" type="button" role="tab">
                Cyclomatic Complexity
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" id="architecture-tab" data-bs-toggle="tab" data-bs-target="#architecture" type="button" role="tab">
                Architecture
            </button>
        </li>
    </ul>
    
    <!-- Tab Content -->
    <div class="tab-content" id="metricsTabContent">
        <div class="tab-pane fade show active" id="lcom" role="tabpanel">
            <div class="row mt-4">
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-header">
                            <h5>LCOM Distribution</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="lcomChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-header">
                            <h5>Cohesion Quality</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="cohesionChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Class Details -->
            <div class="row mt-4">
                <div class="col-12">
                    <div class="card">
                        <div class="card-header">
                            <h5>Class Details</h5>
                        </div>
                        <div class="card-body">
                            <!-- Filter Buttons -->
                            <div class="filter-buttons">
                                <button class="btn btn-outline-primary filter-btn active" data-filter="all">
                                    All Classes
                                </button>
                                <button class="btn btn-outline-success filter-btn" data-filter="excellent">
                                    ✅ Excellent
                                </button>
                                <button class="btn btn-outline-info filter-btn" data-filter="good">
                                    👍 Good
                                </button>
                                <button class="btn btn-outline-warning filter-btn" data-filter="moderate">
                                    ⚠️ Moderate
                                </button>
                                <button class="btn btn-outline-danger filter-btn" data-filter="poor">
                                    ❌ Poor
                                </button>
                            </div>
                            
                            <div class="table-responsive">
                                <table class="table table-striped" id="classTable">
                                    <thead>
                                        <tr>
                                            <th class="sortable" data-column="class">
                                                Class <span class="sort-indicator">↕️</span>
                                            </th>
                                            <th class="sortable" data-column="file">
                                                File <span class="sort-indicator">↕️</span>
                                            </th>
                                            <th class="sortable" data-column="lcom">
                                                LCOM <span class="sort-indicator">↕️</span>
                                            </th>
                                            <th class="sortable" data-column="methods">
                                                Methods <span class="sort-indicator">↕️</span>
                                            </th>
                                            <th class="sortable" data-column="properties">
                                                Properties <span class="sort-indicator">↕️</span>
                                            </th>
                                            <th class="sortable" data-column="quality">
                                                Quality <span class="sort-indicator">↕️</span>
                                            </th>
                                            <th>Suggestions</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        ${generateClassTableRows(analyses)}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Cyclomatic Complexity Tab -->
        <div class="tab-pane fade" id="complexity" role="tabpanel">
            <div class="row mt-4">
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-header">
                            <h5>Complexity Distribution</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="complexityChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-header">
                            <h5>Method Complexity vs Size</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="complexityScatterChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Method Details -->
            <div class="row mt-4">
                <div class="col-12">
                    <div class="card">
                        <div class="card-header">
                            <h5>Method Complexity Details</h5>
                        </div>
                        <div class="card-body">
                            <!-- Complexity Filter Buttons -->
                            <div class="filter-buttons">
                                <button class="btn btn-outline-primary filter-btn active" data-filter="all">
                                    All Methods
                                </button>
                                <button class="btn btn-outline-success filter-btn" data-filter="simple">
                                    Simple (1-5)
                                </button>
                                <button class="btn btn-outline-info filter-btn" data-filter="moderate">
                                    Moderate (6-10)
                                </button>
                                <button class="btn btn-outline-warning filter-btn" data-filter="complex">
                                    Complex (11-20)
                                </button>
                                <button class="btn btn-outline-danger filter-btn" data-filter="very-complex">
                                    Very Complex (21+)
                                </button>
                            </div>
                            
                            <div class="table-responsive">
                                <table class="table table-striped" id="methodTable">
                                    <thead>
                                        <tr>
                                            <th class="sortable" data-column="class">
                                                Class <span class="sort-indicator">↕️</span>
                                            </th>
                                            <th class="sortable" data-column="method">
                                                Method <span class="sort-indicator">↕️</span>
                                            </th>
                                            <th class="sortable" data-column="complexity">
                                                CC <span class="sort-indicator">↕️</span>
                                            </th>
                                            <th class="sortable" data-column="lines">
                                                Lines <span class="sort-indicator">↕️</span>
                                            </th>
                                            <th class="sortable" data-column="complexity-level">
                                                Level <span class="sort-indicator">↕️</span>
                                            </th>
                                            <th>Recommendations</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        ${generateMethodTableRows(analyses)}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- Architecture Analysis Tab -->
        <div class="tab-pane fade" id="architecture" role="tabpanel">
            <div class="row mt-4">
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-header">
                            <h5>Architecture Pattern: ${architectureAnalysis.layeredArchitecture.pattern}</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="layerChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-header">
                            <h5>DDD Patterns Distribution</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="dddChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Dependency Graph Visualization -->
            <div class="row mt-4">
                <div class="col-12">
                    <div class="card">
                        <div class="card-header">
                            <h5>Dependency Graph</h5>
                        </div>
                        <div class="card-body">
                            <div id="dependencyGraph" style="height: 500px; border: 1px solid #ddd;"></div>
                        </div>
                    </div>
                </div>
            </div>
            
            ${generateArchitectureViolationsSection(architectureAnalysis)}
            ${generateDddPatternsSection(architectureAnalysis)}
        </div>
    </div>
</div>

${generateJavaScript(analyses, architectureAnalysis, cohesionDistribution, complexityDistribution)}
"""
    }
    
    private fun generateClassTableRows(analyses: List<ClassAnalysis>): String {
        return analyses.sortedByDescending { it.lcom }.joinToString("") { analysis ->
            val qualityClass = when (analysis.lcom) {
                0 -> "cohesion-excellent"
                in 1..2 -> "cohesion-good"
                in 3..5 -> "cohesion-moderate"
                else -> "cohesion-poor"
            }
            val quality = when (analysis.lcom) {
                0 -> "<span class='badge bg-success'>Excellent</span>"
                in 1..2 -> "<span class='badge bg-info'>Good</span>"
                in 3..5 -> "<span class='badge bg-warning'>Moderate</span>"
                else -> "<span class='badge bg-danger'>Poor</span>"
            }
            val qualityFilter = when (analysis.lcom) {
                0 -> "excellent"
                in 1..2 -> "good"
                in 3..5 -> "moderate"
                else -> "poor"
            }
            """
            <tr class="table-row $qualityClass" data-quality="$qualityFilter" data-class="${analysis.className.lowercase()}" data-file="${analysis.fileName.lowercase()}" data-lcom="${analysis.lcom}" data-methods="${analysis.methodCount}" data-properties="${analysis.propertyCount}">
                <td><strong>${analysis.className}</strong></td>
                <td><code>${analysis.fileName}</code></td>
                <td><span class="badge bg-secondary">${analysis.lcom}</span></td>
                <td>${analysis.methodCount}</td>
                <td>${analysis.propertyCount}</td>
                <td>$quality</td>
                <td>
                    ${analysis.suggestions.joinToString("<br>") { suggestion ->
                        val iconClass = when (suggestion.icon) {
                            "✅", "✨" -> "text-success"
                            "👍" -> "text-info"
                            "⚠️", "🔀", "⚡" -> "text-warning"
                            "❌", "🚨" -> "text-danger"
                            "🔧", "📏", "🧠", "📊" -> "text-secondary"
                            "📤", "🎯" -> "text-primary"
                            else -> "text-muted"
                        }
                        """<span class="$iconClass suggestion-item" data-bs-toggle="tooltip" data-bs-placement="top" title="${suggestion.tooltip}">${suggestion.icon} ${suggestion.message}</span>"""
                    }}
                </td>
            </tr>
            """
        }
    }
    
    private fun generateMethodTableRows(analyses: List<ClassAnalysis>): String {
        return analyses.flatMap { analysis ->
            analysis.complexity.methods.map { method ->
                val complexityLevel = when (method.cyclomaticComplexity) {
                    1 -> "simple"
                    in 2..5 -> "simple"
                    in 6..10 -> "moderate"
                    in 11..20 -> "complex"
                    else -> "very-complex"
                }
                val levelBadge = when (method.cyclomaticComplexity) {
                    1 -> "<span class='badge bg-success'>Simple</span>"
                    in 2..5 -> "<span class='badge bg-success'>Simple</span>"
                    in 6..10 -> "<span class='badge bg-info'>Moderate</span>"
                    in 11..20 -> "<span class='badge bg-warning'>Complex</span>"
                    else -> "<span class='badge bg-danger'>Very Complex</span>"
                }
                val recommendation = when (method.cyclomaticComplexity) {
                    in 1..5 -> "✅ Good complexity"
                    in 6..10 -> "⚠️ Consider simplifying"
                    in 11..20 -> "🔧 Refactor recommended"
                    else -> "🚨 Critical - needs immediate attention"
                }
                """
                <tr class="table-row method-row" data-complexity-level="$complexityLevel" data-class="${analysis.className.lowercase()}" data-method="${method.methodName.lowercase()}" data-complexity="${method.cyclomaticComplexity}" data-lines="${method.lineCount}">
                    <td><strong>${analysis.className}</strong></td>
                    <td><code>${method.methodName}</code></td>
                    <td><span class="badge bg-secondary">${method.cyclomaticComplexity}</span></td>
                    <td>${method.lineCount}</td>
                    <td>$levelBadge</td>
                    <td>$recommendation</td>
                </tr>
                """
            }
        }.joinToString("")
    }
    
    private fun generateArchitectureViolationsSection(architectureAnalysis: com.metrics.model.architecture.ArchitectureAnalysis): String {
        return """
            <!-- Architecture Violations -->
            <div class="row mt-4">
                <div class="col-12">
                    <div class="card">
                        <div class="card-header">
                            <h5>Architecture Violations</h5>
                        </div>
                        <div class="card-body">
                            ${if (architectureAnalysis.layeredArchitecture.violations.isNotEmpty()) {
                                """
                                <div class="table-responsive">
                                    <table class="table table-striped">
                                        <thead>
                                            <tr>
                                                <th>From Class</th>
                                                <th>To Class</th>
                                                <th>Violation Type</th>
                                                <th>Suggestion</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            ${architectureAnalysis.layeredArchitecture.violations.joinToString("") { violation ->
                                                """
                                                <tr>
                                                    <td><strong>${violation.fromClass}</strong></td>
                                                    <td><strong>${violation.toClass}</strong></td>
                                                    <td>
                                                        <span class="badge ${when (violation.violationType) {
                                                            com.metrics.model.common.ViolationType.LAYER_VIOLATION -> "bg-warning"
                                                            com.metrics.model.common.ViolationType.CIRCULAR_DEPENDENCY -> "bg-danger"
                                                            else -> "bg-secondary"
                                                        }}">${violation.violationType}</span>
                                                    </td>
                                                    <td>${violation.suggestion}</td>
                                                </tr>
                                                """
                                            }}
                                        </tbody>
                                    </table>
                                </div>
                                """
                            } else {
                                """
                                <div class="alert alert-success">
                                    <i class="fas fa-check-circle"></i> No architecture violations detected!
                                </div>
                                """
                            }}
                        </div>
                    </div>
                </div>
            </div>
        """
    }
    
    private fun generateDddPatternsSection(architectureAnalysis: com.metrics.model.architecture.ArchitectureAnalysis): String {
        return """
            <!-- DDD Patterns Details -->
            <div class="row mt-4">
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-header">
                            <h5>Detected Entities</h5>
                        </div>
                        <div class="card-body">
                            ${if (architectureAnalysis.dddPatterns.entities.isNotEmpty()) {
                                """
                                <div class="table-responsive">
                                    <table class="table table-sm">
                                        <thead>
                                            <tr>
                                                <th>Class</th>
                                                <th>Has ID</th>
                                                <th>Mutable</th>
                                                <th>Confidence</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            ${architectureAnalysis.dddPatterns.entities.joinToString("") { entity ->
                                                """
                                                <tr>
                                                    <td><strong>${entity.className}</strong></td>
                                                    <td>${if (entity.hasUniqueId) "✅" else "❌"}</td>
                                                    <td>${if (entity.isMutable) "✅" else "❌"}</td>
                                                    <td>
                                                        <span class="badge ${when {
                                                            entity.confidence >= 0.7 -> "bg-success"
                                                            entity.confidence >= 0.5 -> "bg-warning"
                                                            else -> "bg-secondary"
                                                        }}">${"%.0f".format(entity.confidence * 100)}%</span>
                                                    </td>
                                                </tr>
                                                """
                                            }}
                                        </tbody>
                                    </table>
                                </div>
                                """
                            } else {
                                "<p class='text-muted'>No entities detected</p>"
                            }}
                        </div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="card">
                        <div class="card-header">
                            <h5>Detected Services</h5>
                        </div>
                        <div class="card-body">
                            ${if (architectureAnalysis.dddPatterns.services.isNotEmpty()) {
                                """
                                <div class="table-responsive">
                                    <table class="table table-sm">
                                        <thead>
                                            <tr>
                                                <th>Class</th>
                                                <th>Stateless</th>
                                                <th>Domain Logic</th>
                                                <th>Confidence</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            ${architectureAnalysis.dddPatterns.services.joinToString("") { service ->
                                                """
                                                <tr>
                                                    <td><strong>${service.className}</strong></td>
                                                    <td>${if (service.isStateless) "✅" else "❌"}</td>
                                                    <td>${if (service.hasDomainLogic) "✅" else "❌"}</td>
                                                    <td>
                                                        <span class="badge ${when {
                                                            service.confidence >= 0.7 -> "bg-success"
                                                            service.confidence >= 0.5 -> "bg-warning"
                                                            else -> "bg-secondary"
                                                        }}">${"%.0f".format(service.confidence * 100)}%</span>
                                                    </td>
                                                </tr>
                                                """
                                            }}
                                        </tbody>
                                    </table>
                                </div>
                                """
                            } else {
                                "<p class='text-muted'>No services detected</p>"
                            }}
                        </div>
                    </div>
                </div>
            </div>
        """
    }
    
    private fun generateJavaScript(
        analyses: List<ClassAnalysis>, 
        architectureAnalysis: com.metrics.model.architecture.ArchitectureAnalysis,
        cohesionDistribution: Map<String, List<ClassAnalysis>>,
        complexityDistribution: Map<String, List<com.metrics.model.analysis.MethodComplexity>>
    ): String {
        return """
<script>
// Architecture Layer Chart
const layerData = ${architectureAnalysis.layeredArchitecture.layers.let { layers ->
    val labels = layers.map { it.name }
    val data = layers.map { it.classes.size }
    val colors = listOf("#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0", "#9966FF", "#FF9F40")
    "{ labels: [${labels.joinToString(",") { "'$it'" }}], data: [${data.joinToString(",")}], colors: [${colors.take(labels.size).joinToString(",") { "'$it'" }}] }"
}};

const layerCtx = document.getElementById('layerChart').getContext('2d');
new Chart(layerCtx, {
    type: 'bar',
    data: {
        labels: layerData.labels,
        datasets: [{
            label: 'Classes per Layer',
            data: layerData.data,
            backgroundColor: layerData.colors,
            borderWidth: 1
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            title: {
                display: true,
                text: 'Architecture Layers'
            }
        },
        scales: {
            y: {
                beginAtZero: true,
                ticks: {
                    stepSize: 1
                }
            }
        }
    }
});

// DDD Patterns Chart
const dddData = ${architectureAnalysis.dddPatterns.let { ddd ->
    val labels = listOf("Entities", "Value Objects", "Services", "Repositories", "Aggregates", "Domain Events")
    val data = listOf(ddd.entities.size, ddd.valueObjects.size, ddd.services.size, ddd.repositories.size, ddd.aggregates.size, ddd.domainEvents.size)
    val colors = listOf("#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0", "#9966FF", "#FF9F40")
    "{ labels: [${labels.joinToString(",") { "'$it'" }}], data: [${data.joinToString(",")}], colors: [${colors.joinToString(",") { "'$it'" }}] }"
}};

const dddCtx = document.getElementById('dddChart').getContext('2d');
new Chart(dddCtx, {
    type: 'doughnut',
    data: {
        labels: dddData.labels,
        datasets: [{
            data: dddData.data,
            backgroundColor: dddData.colors,
            borderWidth: 2
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            title: {
                display: true,
                text: 'DDD Patterns Distribution'
            }
        }
    }
});

// Initialize dependency graph and all other JavaScript functionality
${generateDependencyGraphScript(architectureAnalysis)}
${generateChartsScript(analyses, cohesionDistribution, complexityDistribution)}
${generateTableInteractivityScript()}
</script>
"""
    }
    
    private fun generateDependencyGraphScript(architectureAnalysis: com.metrics.model.architecture.ArchitectureAnalysis): String {
        return """
// D3.js Interactive Dependency Graph Visualization
const graphData = {
    nodes: ${architectureAnalysis.dependencyGraph.nodes.let { nodes ->
        "[${nodes.joinToString(",") { node -> 
            "{ id: '${node.id}', label: '${node.className.substringAfterLast(".")}', fullName: '${node.className}', layer: '${node.layer ?: "unknown"}' }"
        }}]"
    }},
    links: ${architectureAnalysis.dependencyGraph.edges.let { edges ->
        "[${edges.take(100).joinToString(",") { edge -> // Limit to first 100 edges for performance
            "{ source: '${edge.fromId}', target: '${edge.toId}', type: '${edge.dependencyType}' }"
        }}]"
    }}
};

// Global variables for D3.js graph
let dependencyGraphInitialized = false;
let simulation;
let svg;
let tooltip;

function initializeDependencyGraph() {
    if (dependencyGraphInitialized) return;
    
    const dependencyGraphContainer = document.getElementById('dependencyGraph');
    if (!dependencyGraphContainer) {
        console.error('Dependency graph container not found');
        return;
    }
    
    // Get container dimensions
    const containerRect = dependencyGraphContainer.getBoundingClientRect();
    const width = containerRect.width > 0 ? containerRect.width : 800;
    const height = 600;
    
    // Clear previous content
    dependencyGraphContainer.innerHTML = '';
    
    // Create SVG
    svg = d3.select('#dependencyGraph')
        .append('svg')
        .attr('width', width)
        .attr('height', height);
    
    // Create tooltip
    tooltip = d3.select('body').append('div')
        .attr('class', 'tooltip')
        .style('opacity', 0);
    
    // Create force simulation
    simulation = d3.forceSimulation(graphData.nodes)
        .force('link', d3.forceLink(graphData.links).id(d => d.id).distance(80))
        .force('charge', d3.forceManyBody().strength(-300))
        .force('center', d3.forceCenter(width / 2, height / 2))
        .force('collision', d3.forceCollide().radius(30));
    
    // Add zoom behavior
    const zoom = d3.zoom()
        .scaleExtent([0.1, 4])
        .on('zoom', (event) => {
            g.attr('transform', event.transform);
        });
    
    svg.call(zoom);
    
    // Create main group for pan/zoom
    const g = svg.append('g');
    
    // Create links
    const link = g.append('g')
        .selectAll('line')
        .data(graphData.links)
        .enter().append('line')
        .attr('class', 'link')
        .style('stroke-width', 2);
    
    // Create nodes
    const node = g.append('g')
        .selectAll('g')
        .data(graphData.nodes)
        .enter().append('g')
        .attr('class', d => 'node ' + d.layer)
        .call(d3.drag()
            .on('start', dragstarted)
            .on('drag', dragged)
            .on('end', dragended));
    
    // Add circles to nodes
    node.append('circle')
        .attr('r', 20)
        .style('fill', d => {
            switch(d.layer) {
                case 'presentation': return '#e74c3c';
                case 'application': return '#f39c12';
                case 'domain': return '#2ecc71';
                case 'infrastructure': return '#3498db';
                default: return '#95a5a6';
            }
        });
    
    // Add labels to nodes
    node.append('text')
        .text(d => d.label)
        .attr('dy', 5)
        .style('text-anchor', 'middle')
        .style('font-size', '10px')
        .style('fill', 'white');
    
    // Add hover effects
    node.on('mouseover', function(event, d) {
        tooltip.transition()
            .duration(200)
            .style('opacity', .9);
        tooltip.html(d.fullName + '<br/>Layer: ' + d.layer)
            .style('left', (event.pageX + 10) + 'px')
            .style('top', (event.pageY - 28) + 'px');
    })
    .on('mouseout', function() {
        tooltip.transition()
            .duration(500)
            .style('opacity', 0);
    });
    
    // Update positions on tick
    simulation.on('tick', () => {
        link
            .attr('x1', d => d.source.x)
            .attr('y1', d => d.source.y)
            .attr('x2', d => d.target.x)
            .attr('y2', d => d.target.y);

        node
            .attr('transform', d => 'translate(' + d.x + ',' + d.y + ')');
    });
    
    // Drag functions
    function dragstarted(event, d) {
        if (!event.active) simulation.alphaTarget(0.3).restart();
        d.fx = d.x;
        d.fy = d.y;
    }
    
    function dragged(event, d) {
        d.fx = event.x;
        d.fy = event.y;
    }
    
    function dragended(event, d) {
        if (!event.active) simulation.alphaTarget(0);
        d.fx = null;
        d.fy = null;
    }
    
    dependencyGraphInitialized = true;
}

// Initialize the dependency graph when the Architecture tab is shown
document.addEventListener('DOMContentLoaded', function() {
    const architectureTab = document.getElementById('architecture-tab');
    if (architectureTab) {
        architectureTab.addEventListener('shown.bs.tab', function() {
            setTimeout(() => {
                initializeDependencyGraph();
            }, 100);
        });
    }
});
"""
    }
    
    private fun generateChartsScript(
        analyses: List<ClassAnalysis>,
        cohesionDistribution: Map<String, List<ClassAnalysis>>,
        complexityDistribution: Map<String, List<com.metrics.model.analysis.MethodComplexity>>
    ): String {
        return """
// LCOM Distribution Chart
const lcomData = ${analyses.map { it.lcom }.let { lcomValues ->
    val histogram = mutableMapOf<Int, Int>()
    lcomValues.forEach { lcom ->
        histogram[lcom] = histogram.getOrDefault(lcom, 0) + 1
    }
    histogram.toSortedMap().let { sortedMap ->
        "{ labels: [${sortedMap.keys.joinToString(",") { "'$it'" }}], data: [${sortedMap.values.joinToString(",")}] }"
    }
}};

const lcomCtx = document.getElementById('lcomChart').getContext('2d');
new Chart(lcomCtx, {
    type: 'bar',
    data: {
        labels: lcomData.labels,
        datasets: [{
            label: 'Number of Classes',
            data: lcomData.data,
            backgroundColor: 'rgba(54, 162, 235, 0.8)',
            borderColor: 'rgba(54, 162, 235, 1)',
            borderWidth: 1
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            title: {
                display: true,
                text: 'LCOM Value Distribution'
            }
        },
        scales: {
            y: {
                beginAtZero: true,
                ticks: {
                    stepSize: 1
                }
            }
        }
    }
});

// Cohesion Quality Chart
const cohesionData = ${cohesionDistribution.let { dist ->
    val labels = listOf("Excellent", "Good", "Moderate", "Poor")
    val data = labels.map { dist[it]?.size ?: 0 }
    val colors = listOf("#28a745", "#17a2b8", "#ffc107", "#dc3545")
    "{ labels: [${labels.joinToString(",") { "'$it'" }}], data: [${data.joinToString(",")}], colors: [${colors.joinToString(",") { "'$it'" }}] }"
}};

const cohesionCtx = document.getElementById('cohesionChart').getContext('2d');
new Chart(cohesionCtx, {
    type: 'doughnut',
    data: {
        labels: cohesionData.labels,
        datasets: [{
            data: cohesionData.data,
            backgroundColor: cohesionData.colors,
            borderWidth: 2
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            title: {
                display: true,
                text: 'Cohesion Quality Distribution'
            }
        }
    }
});

// Complexity Distribution Chart
const complexityData = ${complexityDistribution.let { dist ->
    val labels = listOf("Simple (1)", "Low (2-5)", "Moderate (6-10)", "High (11-20)", "Very High (21+)")
    val data = labels.map { dist[it]?.size ?: 0 }
    val colors = listOf("#28a745", "#17a2b8", "#ffc107", "#fd7e14", "#dc3545")
    "{ labels: [${labels.joinToString(",") { "'$it'" }}], data: [${data.joinToString(",")}], colors: [${colors.joinToString(",") { "'$it'" }}] }"
}};

const complexityCtx = document.getElementById('complexityChart').getContext('2d');
new Chart(complexityCtx, {
    type: 'bar',
    data: {
        labels: complexityData.labels,
        datasets: [{
            label: 'Number of Methods',
            data: complexityData.data,
            backgroundColor: complexityData.colors,
            borderWidth: 1
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            title: {
                display: true,
                text: 'Method Complexity Distribution'
            }
        },
        scales: {
            y: {
                beginAtZero: true,
                ticks: {
                    stepSize: 1
                }
            }
        }
    }
});

// Complexity vs Size Scatter Chart
const scatterData = [${analyses.flatMap { analysis ->
    analysis.complexity.methods.map { method ->
        "{x: ${method.lineCount}, y: ${method.cyclomaticComplexity}, label: '${method.methodName}'}"
    }
}.joinToString(",")}];

const scatterCtx = document.getElementById('complexityScatterChart').getContext('2d');
new Chart(scatterCtx, {
    type: 'scatter',
    data: {
        datasets: [{
            label: 'Methods',
            data: scatterData,
            backgroundColor: 'rgba(54, 162, 235, 0.6)',
            borderColor: 'rgba(54, 162, 235, 1)',
            borderWidth: 1
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            title: {
                display: true,
                text: 'Method Complexity vs Lines of Code'
            },
            tooltip: {
                callbacks: {
                    label: function(context) {
                        return context.raw.label + ' (CC: ' + context.raw.y + ', Lines: ' + context.raw.x + ')';
                    }
                }
            }
        },
        scales: {
            x: {
                title: {
                    display: true,
                    text: 'Lines of Code'
                }
            },
            y: {
                title: {
                    display: true,
                    text: 'Cyclomatic Complexity'
                }
            }
        }
    }
});
"""
    }
    
    private fun generateTableInteractivityScript(): String {
        return """
// Table sorting and filtering functionality
let sortDirection = {};
let currentFilter = { lcom: 'all', complexity: 'all' };

// Filter functionality for both tabs
document.querySelectorAll('.filter-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        // Update active filter button within the same tab
        const parentTab = this.closest('.tab-pane');
        const tabButtons = parentTab.querySelectorAll('.filter-btn');
        tabButtons.forEach(b => b.classList.remove('active'));
        this.classList.add('active');
        
        const filter = this.dataset.filter;
        const tabId = parentTab.id;
        
        if (tabId === 'lcom') {
            currentFilter.lcom = filter;
            filterLcomTable();
        } else if (tabId === 'complexity') {
            currentFilter.complexity = filter;
            filterComplexityTable();
        }
    });
});

function filterLcomTable() {
    const rows = document.querySelectorAll('#classTable tbody tr');
    rows.forEach(row => {
        const quality = row.dataset.quality;
        if (currentFilter.lcom === 'all' || quality === currentFilter.lcom) {
            row.classList.remove('filtered');
        } else {
            row.classList.add('filtered');
        }
    });
}

function filterComplexityTable() {
    const rows = document.querySelectorAll('#methodTable tbody tr');
    rows.forEach(row => {
        const complexityLevel = row.dataset.complexityLevel;
        if (currentFilter.complexity === 'all' || complexityLevel === currentFilter.complexity) {
            row.classList.remove('filtered');
        } else {
            row.classList.add('filtered');
        }
    });
}

// Sort functionality
document.querySelectorAll('.sortable').forEach(th => {
    th.addEventListener('click', function() {
        const column = this.dataset.column;
        const currentDirection = sortDirection[column] || 'asc';
        const newDirection = currentDirection === 'asc' ? 'desc' : 'asc';
        
        // Update sort direction
        sortDirection[column] = newDirection;
        
        // Update sort indicators within the same table
        const table = this.closest('table');
        const indicators = table.querySelectorAll('.sort-indicator');
        indicators.forEach(indicator => {
            indicator.classList.remove('active');
            indicator.textContent = '↕️';
        });
        
        const indicator = this.querySelector('.sort-indicator');
        indicator.classList.add('active');
        indicator.textContent = newDirection === 'asc' ? '↑' : '↓';
        
        // Determine which table to sort
        const tableId = table.id;
        if (tableId === 'classTable') {
            sortLcomTable(column, newDirection);
        } else if (tableId === 'methodTable') {
            sortComplexityTable(column, newDirection);
        }
    });
});

function sortLcomTable(column, direction) {
    const tbody = document.querySelector('#classTable tbody');
    const rows = Array.from(tbody.querySelectorAll('tr'));
    
    rows.sort((a, b) => {
        let aVal, bVal;
        
        switch(column) {
            case 'class':
                aVal = a.dataset.class;
                bVal = b.dataset.class;
                break;
            case 'file':
                aVal = a.dataset.file;
                bVal = b.dataset.file;
                break;
            case 'lcom':
                aVal = parseInt(a.dataset.lcom);
                bVal = parseInt(b.dataset.lcom);
                break;
            case 'methods':
                aVal = parseInt(a.dataset.methods);
                bVal = parseInt(b.dataset.methods);
                break;
            case 'properties':
                aVal = parseInt(a.dataset.properties);
                bVal = parseInt(b.dataset.properties);
                break;
            case 'quality':
                const qualityOrder = {'excellent': 0, 'good': 1, 'moderate': 2, 'poor': 3};
                aVal = qualityOrder[a.dataset.quality];
                bVal = qualityOrder[b.dataset.quality];
                break;
            default:
                return 0;
        }
        
        if (typeof aVal === 'string') {
            return direction === 'asc' ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
        } else {
            return direction === 'asc' ? aVal - bVal : bVal - aVal;
        }
    });
    
    // Re-append sorted rows
    rows.forEach(row => tbody.appendChild(row));
    
    // Re-apply filter after sorting
    filterLcomTable();
}

function sortComplexityTable(column, direction) {
    const tbody = document.querySelector('#methodTable tbody');
    const rows = Array.from(tbody.querySelectorAll('tr'));
    
    rows.sort((a, b) => {
        let aVal, bVal;
        
        switch(column) {
            case 'class':
                aVal = a.dataset.class;
                bVal = b.dataset.class;
                break;
            case 'method':
                aVal = a.dataset.method;
                bVal = b.dataset.method;
                break;
            case 'complexity':
                aVal = parseInt(a.dataset.complexity);
                bVal = parseInt(b.dataset.complexity);
                break;
            case 'lines':
                aVal = parseInt(a.dataset.lines);
                bVal = parseInt(b.dataset.lines);
                break;
            case 'complexity-level':
                const complexityOrder = {'simple': 0, 'moderate': 1, 'complex': 2, 'very-complex': 3};
                aVal = complexityOrder[a.dataset.complexityLevel];
                bVal = complexityOrder[b.dataset.complexityLevel];
                break;
            default:
                return 0;
        }
        
        if (typeof aVal === 'string') {
            return direction === 'asc' ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
        } else {
            return direction === 'asc' ? aVal - bVal : bVal - aVal;
        }
    });
    
    // Re-append sorted rows
    rows.forEach(row => tbody.appendChild(row));
    
    // Re-apply filter after sorting
    filterComplexityTable();
}

// Initialize tooltips
document.addEventListener('DOMContentLoaded', function() {
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
});
"""
    }
}