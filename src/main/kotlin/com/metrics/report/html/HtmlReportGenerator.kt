package com.metrics.report.html

import com.metrics.model.analysis.*
import com.metrics.model.architecture.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Generates comprehensive HTML reports with reorganized dashboard structure.
 * Phase 3: UI Reorganization with new tab structure and enhanced visualizations.
 */
class HtmlReportGenerator {
    
    fun generateReport(analyses: List<ClassAnalysis>, architectureAnalysis: ArchitectureAnalysis): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val reportFile = File("kotlin-metrics-report.html")
        
        val html = buildString {
            append(generateHtmlHeader())
            append(generateHtmlBody(analyses, architectureAnalysis, timestamp))
            append(generateHtmlFooter())
        }
        
        reportFile.writeText(html)
        println("HTML report saved to: ${reportFile.absolutePath}")
        return reportFile
    }
    
    private fun generateHtmlHeader(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kotlin Metrics Report - Comprehensive Quality Analysis</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script src="https://d3js.org/d3.v7.min.js"></script>
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <style>
        :root {
            --primary-color: #2c3e50;
            --secondary-color: #3498db;
            --success-color: #27ae60;
            --warning-color: #f39c12;
            --danger-color: #e74c3c;
            --info-color: #17a2b8;
            --light-bg: #f8f9fa;
            --card-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }
        
        body { 
            background-color: var(--light-bg); 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
        
        .main-header {
            background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
            color: white;
            padding: 2rem 0;
            margin-bottom: 2rem;
        }
        
        .metric-card { 
            transition: all 0.3s ease;
            border: none;
            box-shadow: var(--card-shadow);
            border-radius: 12px;
        }
        .metric-card:hover { 
            transform: translateY(-5px);
            box-shadow: 0 8px 15px rgba(0, 0, 0, 0.15);
        }
        
        /* Quality Level Indicators */
        .quality-excellent { border-left: 5px solid var(--success-color); }
        .quality-good { border-left: 5px solid var(--info-color); }
        .quality-moderate { border-left: 5px solid var(--warning-color); }
        .quality-poor { border-left: 5px solid var(--danger-color); }
        
        /* Enhanced Tab System */
        .nav-tabs {
            border-bottom: 3px solid var(--primary-color);
            margin-bottom: 2rem;
        }
        .nav-tabs .nav-link {
            border: none;
            border-radius: 0;
            color: var(--primary-color);
            font-weight: 600;
            padding: 1rem 2rem;
            transition: all 0.3s ease;
        }
        .nav-tabs .nav-link:hover {
            border-color: transparent;
            background-color: rgba(52, 152, 219, 0.1);
        }
        .nav-tabs .nav-link.active {
            background-color: var(--primary-color);
            color: white;
            border-color: var(--primary-color);
        }
        
        /* Enhanced Chart Containers */
        .chart-container { 
            height: 400px; 
            position: relative;
            background: white;
            border-radius: 8px;
            padding: 1rem;
        }
        
        /* Table Enhancements */
        .table-enhanced {
            background: white;
            border-radius: 8px;
            overflow: hidden;
            box-shadow: var(--card-shadow);
        }
        .sortable { 
            cursor: pointer; 
            user-select: none;
            transition: background-color 0.2s;
        }
        .sortable:hover { background-color: rgba(52, 152, 219, 0.1); }
        .sort-indicator { margin-left: 8px; opacity: 0.5; }
        .sort-indicator.active { opacity: 1; color: var(--primary-color); }
        
        /* Filter System */
        .filter-panel {
            background: white;
            border-radius: 8px;
            padding: 1.5rem;
            margin-bottom: 1.5rem;
            box-shadow: var(--card-shadow);
        }
        .filter-btn { 
            margin: 0.25rem;
            border-radius: 20px;
            padding: 0.5rem 1rem;
            transition: all 0.2s;
        }
        .filter-btn.active { 
            background-color: var(--primary-color);
            border-color: var(--primary-color);
            color: white;
        }
        
        /* Risk Assessment */
        .risk-critical { color: var(--danger-color); font-weight: bold; }
        .risk-high { color: #fd7e14; font-weight: 600; }
        .risk-medium { color: var(--warning-color); font-weight: 500; }
        .risk-low { color: var(--success-color); }
        
        /* Progress Bars for Quality Scores */
        .quality-progress {
            height: 8px;
            border-radius: 4px;
            background-color: #e9ecef;
            overflow: hidden;
        }
        .quality-progress-bar {
            height: 100%;
            transition: width 0.6s ease;
        }
        
        /* Coupling Matrix */
        .coupling-matrix {
            font-family: monospace;
            font-size: 0.8rem;
        }
        .coupling-cell {
            min-width: 30px;
            text-align: center;
            padding: 0.25rem;
            border: 1px solid #dee2e6;
        }
        .coupling-strong { background-color: #dc3545; color: white; }
        .coupling-moderate { background-color: #ffc107; color: black; }
        .coupling-weak { background-color: #28a745; color: white; }
        
        /* D3.js Dependency Graph Enhancements */
        #dependencyGraph { 
            height: 600px; 
            border: 1px solid #dee2e6; 
            border-radius: 12px; 
            background: white;
            box-shadow: var(--card-shadow);
        }
        .node { cursor: pointer; }
        .node circle { stroke: #333; stroke-width: 2px; }
        .node text { font: 12px sans-serif; pointer-events: none; text-anchor: middle; }
        .link { stroke: #999; stroke-opacity: 0.6; stroke-width: 1px; }
        .link.cycle { stroke: #dc3545; stroke-width: 3px; opacity: 0.8; }
        .link.strong-coupling { stroke-width: 3px; }
        
        /* Layer-based node colors */
        .node.presentation { fill: #e74c3c; }
        .node.application { fill: #f39c12; }
        .node.domain { fill: #27ae60; }
        .node.infrastructure { fill: #3498db; }
        .node.data { fill: #9b59b6; }
        .node.unknown { fill: #95a5a6; }
        
        /* Enhanced tooltips */
        .tooltip { 
            position: absolute; 
            padding: 12px; 
            background: rgba(0,0,0,0.9); 
            color: white; 
            border-radius: 8px; 
            font-size: 12px; 
            pointer-events: none; 
            z-index: 1000;
            max-width: 300px;
            box-shadow: 0 4px 8px rgba(0,0,0,0.3);
        }
        
        /* Animation classes */
        .fade-in { animation: fadeIn 0.5s ease-in; }
        @keyframes fadeIn {
            from { opacity: 0; transform: translateY(20px); }
            to { opacity: 1; transform: translateY(0); }
        }
        
        /* Responsive adjustments */
        @media (max-width: 768px) {
            .nav-tabs .nav-link { padding: 0.75rem 1rem; font-size: 0.9rem; }
            .chart-container { height: 300px; }
            .metric-card { margin-bottom: 1rem; }
        }
    </style>
</head>
<body>
"""
    
    private fun generateHtmlBody(analyses: List<ClassAnalysis>, architectureAnalysis: ArchitectureAnalysis, timestamp: String): String {
        return buildString {
            append(generateMainHeader(timestamp))
            append(generateNavigationTabs())
            append(generateTabContent(analyses, architectureAnalysis))
        }
    }
    
    private fun generateMainHeader(timestamp: String): String = """
<div class="main-header">
    <div class="container-fluid">
        <div class="row">
            <div class="col-12 text-center">
                <h1 class="display-4 mb-3">
                    <i class="fas fa-chart-line me-3"></i>Code Quality Analysis Report
                </h1>
                <p class="lead mb-0">Comprehensive CK Metrics & Architecture Assessment</p>
                <p class="text-light"><i class="fas fa-clock me-2"></i>Generated: $timestamp</p>
            </div>
        </div>
    </div>
</div>
"""
    
    private fun generateNavigationTabs(): String = """
<div class="container-fluid">
    <ul class="nav nav-tabs" id="metricsTab" role="tablist">
        <li class="nav-item" role="presentation">
            <button class="nav-link active" id="overview-tab" data-bs-toggle="tab" data-bs-target="#overview" type="button" role="tab">
                <i class="fas fa-tachometer-alt me-2"></i>Overview
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" id="quality-tab" data-bs-toggle="tab" data-bs-target="#quality" type="button" role="tab">
                <i class="fas fa-gem me-2"></i>Quality
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" id="coupling-tab" data-bs-toggle="tab" data-bs-target="#coupling" type="button" role="tab">
                <i class="fas fa-link me-2"></i>Coupling
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" id="design-tab" data-bs-toggle="tab" data-bs-target="#design" type="button" role="tab">
                <i class="fas fa-sitemap me-2"></i>Design
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" id="architecture-tab" data-bs-toggle="tab" data-bs-target="#architecture" type="button" role="tab">
                <i class="fas fa-building me-2"></i>Architecture
            </button>
        </li>
        <li class="nav-item" role="presentation">
            <button class="nav-link" id="details-tab" data-bs-toggle="tab" data-bs-target="#details" type="button" role="tab">
                <i class="fas fa-list-alt me-2"></i>Details
            </button>
        </li>
    </ul>
"""
    
    private fun generateTabContent(analyses: List<ClassAnalysis>, architectureAnalysis: ArchitectureAnalysis): String {
        return buildString {
            append("""<div class="tab-content" id="metricsTabContent">""")
            append(generateOverviewTab(analyses, architectureAnalysis))
            append(generateQualityTab(analyses))
            append(generateCouplingTab(analyses))
            append(generateDesignTab(analyses))
            append(generateArchitectureTab(architectureAnalysis))
            append(generateDetailsTab(analyses))
            append("""</div>""")
            append(generateJavaScript(analyses, architectureAnalysis))
            append("""</div>""") // Close container-fluid
        }
    }
    
    private fun generateOverviewTab(analyses: List<ClassAnalysis>, architectureAnalysis: ArchitectureAnalysis): String {
        val projectQualityScore = calculateProjectQualityScore(analyses)
        val totalClasses = analyses.size
        val avgLcom = if (analyses.isNotEmpty()) analyses.map { it.lcom }.average() else 0.0
        val avgComplexity = if (analyses.isNotEmpty()) analyses.map { it.complexity.totalComplexity }.average() else 0.0
        val highRiskClasses = analyses.count { it.riskAssessment.level == RiskLevel.HIGH || it.riskAssessment.level == RiskLevel.CRITICAL }
        
        return """
        <div class="tab-pane fade show active" id="overview" role="tabpanel">
            <div class="row">
                <!-- Project Summary Cards -->
                <div class="col-md-3 col-sm-6 mb-4">
                    <div class="card metric-card text-center">
                        <div class="card-body">
                            <h3 class="text-primary">${totalClasses}</h3>
                            <p class="card-text">Total Classes</p>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 col-sm-6 mb-4">
                    <div class="card metric-card text-center">
                        <div class="card-body">
                            <h3 class="text-info">%.1f</h3>
                            <p class="card-text">Overall Quality Score</p>
                            <div class="quality-progress">
                                <div class="quality-progress-bar bg-info" style="width: %.1f%%"></div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 col-sm-6 mb-4">
                    <div class="card metric-card text-center">
                        <div class="card-body">
                            <h3 class="text-warning">%.1f</h3>
                            <p class="card-text">Avg LCOM</p>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 col-sm-6 mb-4">
                    <div class="card metric-card text-center">
                        <div class="card-body">
                            <h3 class="text-danger">${highRiskClasses}</h3>
                            <p class="card-text">High Risk Classes</p>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Project Overview Charts -->
            <div class="row">
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-chart-pie me-2"></i>Quality Score Distribution</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="overviewQualityChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-chart-bar me-2"></i>Risk Assessment Summary</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="overviewRiskChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Top Issues -->
            <div class="row">
                <div class="col-12">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-exclamation-triangle me-2"></i>Top Quality Issues</h5>
                        </div>
                        <div class="card-body">
                            ${generateTopIssuesTable(analyses)}
                        </div>
                    </div>
                </div>
            </div>
        </div>
        """.format(projectQualityScore.overall, projectQualityScore.overall * 10, avgLcom)
    }
    
    private fun generateQualityTab(analyses: List<ClassAnalysis>): String {
        return """
        <div class="tab-pane fade" id="quality" role="tabpanel">
            <div class="filter-panel">
                <h6><i class="fas fa-filter me-2"></i>Quality Filters</h6>
                <div class="btn-group" role="group">
                    <button type="button" class="btn btn-outline-primary filter-btn active" data-filter="all">All Classes</button>
                    <button type="button" class="btn btn-outline-success filter-btn" data-filter="excellent">Excellent (9-10)</button>
                    <button type="button" class="btn btn-outline-info filter-btn" data-filter="good">Good (7-8)</button>
                    <button type="button" class="btn btn-outline-warning filter-btn" data-filter="moderate">Moderate (5-6)</button>
                    <button type="button" class="btn btn-outline-danger filter-btn" data-filter="poor">Poor (<5)</button>
                </div>
            </div>
            
            <div class="row">
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-puzzle-piece me-2"></i>Cohesion Analysis (LCOM)</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="cohesionChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-cogs me-2"></i>Complexity Analysis (WMC)</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="complexityChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="row">
                <div class="col-12">
                    <div class="card table-enhanced">
                        <div class="card-header">
                            <h5><i class="fas fa-table me-2"></i>Quality Metrics by Class</h5>
                        </div>
                        <div class="card-body">
                            ${generateQualityTable(analyses)}
                        </div>
                    </div>
                </div>
            </div>
        </div>
        """
    }
    
    private fun generateCouplingTab(analyses: List<ClassAnalysis>): String {
        return """
        <div class="tab-pane fade" id="coupling" role="tabpanel">
            <div class="row">
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-exchange-alt me-2"></i>Coupling Between Objects (CBO)</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="cboChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-reply-all me-2"></i>Response for Class (RFC)</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="rfcChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="row">
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-arrow-right me-2"></i>Afferent Coupling (CA)</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="caChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-arrow-left me-2"></i>Efferent Coupling (CE)</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="ceChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="row">
                <div class="col-12">
                    <div class="card table-enhanced">
                        <div class="card-header">
                            <h5><i class="fas fa-table me-2"></i>Coupling Metrics by Class</h5>
                        </div>
                        <div class="card-body">
                            ${generateCouplingTable(analyses)}
                        </div>
                    </div>
                </div>
            </div>
        </div>
        """
    }
    
    private fun generateDesignTab(analyses: List<ClassAnalysis>): String {
        return """
        <div class="tab-pane fade" id="design" role="tabpanel">
            <div class="row">
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-layer-group me-2"></i>Depth of Inheritance Tree (DIT)</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="ditChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-code-branch me-2"></i>Number of Children (NOC)</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="nocChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="row">
                <div class="col-12">
                    <div class="card table-enhanced">
                        <div class="card-header">
                            <h5><i class="fas fa-table me-2"></i>Inheritance Metrics by Class</h5>
                        </div>
                        <div class="card-body">
                            ${generateInheritanceTable(analyses)}
                        </div>
                    </div>
                </div>
            </div>
        </div>
        """
    }
    
    private fun generateArchitectureTab(architectureAnalysis: ArchitectureAnalysis): String {
        return """
        <div class="tab-pane fade" id="architecture" role="tabpanel">
            <div class="row">
                <div class="col-md-8 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-project-diagram me-2"></i>Dependency Graph</h5>
                        </div>
                        <div class="card-body">
                            <div id="dependencyGraph"></div>
                        </div>
                    </div>
                </div>
                <div class="col-md-4 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-layer-group me-2"></i>Architecture Layers</h5>
                        </div>
                        <div class="card-body">
                            ${generateLayersInfo(architectureAnalysis.layeredArchitecture)}
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="row">
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-cubes me-2"></i>DDD Pattern Distribution</h5>
                        </div>
                        <div class="card-body">
                            <div class="chart-container">
                                <canvas id="dddChart"></canvas>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-exclamation-circle me-2"></i>Architecture Violations</h5>
                        </div>
                        <div class="card-body">
                            ${generateViolationsInfo(architectureAnalysis.layeredArchitecture)}
                        </div>
                    </div>
                </div>
            </div>
        </div>
        """
    }
    
    private fun generateDetailsTab(analyses: List<ClassAnalysis>): String {
        return """
        <div class="tab-pane fade" id="details" role="tabpanel">
            <div class="filter-panel">
                <h6><i class="fas fa-search me-2"></i>Search & Filter</h6>
                <div class="row">
                    <div class="col-md-6">
                        <input type="text" class="form-control" id="classSearchInput" placeholder="Search by class name...">
                    </div>
                    <div class="col-md-6">
                        <select class="form-select" id="sortSelect">
                            <option value="name">Sort by Name</option>
                            <option value="quality">Sort by Quality Score</option>
                            <option value="lcom">Sort by LCOM</option>
                            <option value="complexity">Sort by Complexity</option>
                            <option value="coupling">Sort by Coupling</option>
                            <option value="risk">Sort by Risk Level</option>
                        </select>
                    </div>
                </div>
            </div>
            
            <div class="card table-enhanced">
                <div class="card-header">
                    <h5><i class="fas fa-list-alt me-2"></i>Complete Class Analysis</h5>
                </div>
                <div class="card-body">
                    ${generateCompleteDetailsTable(analyses)}
                </div>
            </div>
        </div>
        """
    }
    
    // Helper methods for generating table content and calculations
    private fun calculateProjectQualityScore(analyses: List<ClassAnalysis>): QualityScore {
        if (analyses.isEmpty()) {
            return QualityScore(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
        
        val avgCohesion = analyses.map { it.qualityScore.cohesion }.average()
        val avgComplexity = analyses.map { it.qualityScore.complexity }.average()
        val avgCoupling = analyses.map { it.qualityScore.coupling }.average()
        val avgInheritance = analyses.map { it.qualityScore.inheritance }.average()
        val avgArchitecture = analyses.map { it.qualityScore.architecture }.average()
        val avgOverall = analyses.map { it.qualityScore.overall }.average()
        
        return QualityScore(
            cohesion = avgCohesion,
            complexity = avgComplexity,
            coupling = avgCoupling,
            inheritance = avgInheritance,
            architecture = avgArchitecture,
            overall = avgOverall
        )
    }
    
    private fun generateTopIssuesTable(analyses: List<ClassAnalysis>): String {
        val highRiskClasses = analyses
            .filter { it.riskAssessment.level == RiskLevel.HIGH || it.riskAssessment.level == RiskLevel.CRITICAL }
            .sortedByDescending { it.riskAssessment.priority }
            .take(10)
        
        if (highRiskClasses.isEmpty()) {
            return """<p class="text-success"><i class="fas fa-check-circle me-2"></i>No critical quality issues found!</p>"""
        }
        
        return buildString {
            append("""<div class="table-responsive">""")
            append("""<table class="table table-hover">""")
            append("""<thead class="table-dark">""")
            append("""<tr><th>Class</th><th>Risk Level</th><th>Issues</th><th>Impact</th></tr>""")
            append("""</thead><tbody>""")
            
            highRiskClasses.forEach { analysis ->
                val riskClass = when (analysis.riskAssessment.level) {
                    RiskLevel.CRITICAL -> "risk-critical"
                    RiskLevel.HIGH -> "risk-high"
                    else -> "risk-medium"
                }
                
                append("""<tr>""")
                append("""<td><strong>${analysis.className}</strong></td>""")
                append("""<td><span class="$riskClass">${analysis.riskAssessment.level}</span></td>""")
                append("""<td>${analysis.riskAssessment.reasons.joinToString(", ")}</td>""")
                append("""<td>${analysis.riskAssessment.impact}</td>""")
                append("""</tr>""")
            }
            
            append("""</tbody></table></div>""")
        }
    }
    
    private fun generateQualityTable(analyses: List<ClassAnalysis>): String {
        return generateDataTable(analyses) { analysis ->
            """
            <tr class="table-row" data-quality="${analysis.qualityScore.overall}">
                <td><strong>${analysis.className}</strong></td>
                <td><span class="badge bg-info">${"%.1f".format(analysis.qualityScore.overall)}</span></td>
                <td>${analysis.lcom}</td>
                <td>${analysis.ckMetrics.wmc}</td>
                <td><span class="badge bg-${getQualityBadgeColor(analysis.qualityScore.overall)}">${getQualityLevel(analysis.qualityScore.overall)}</span></td>
            </tr>
            """
        }
    }
    
    private fun generateCouplingTable(analyses: List<ClassAnalysis>): String {
        return generateDataTable(analyses) { analysis ->
            """
            <tr class="table-row">
                <td><strong>${analysis.className}</strong></td>
                <td>${analysis.ckMetrics.cbo}</td>
                <td>${analysis.ckMetrics.rfc}</td>
                <td>${analysis.ckMetrics.ca}</td>
                <td>${analysis.ckMetrics.ce}</td>
                <td><span class="badge bg-${getQualityBadgeColor(analysis.qualityScore.coupling)}">${"%.1f".format(analysis.qualityScore.coupling)}</span></td>
            </tr>
            """
        }
    }
    
    private fun generateInheritanceTable(analyses: List<ClassAnalysis>): String {
        return generateDataTable(analyses) { analysis ->
            """
            <tr class="table-row">
                <td><strong>${analysis.className}</strong></td>
                <td>${analysis.ckMetrics.dit}</td>
                <td>${analysis.ckMetrics.noc}</td>
                <td><span class="badge bg-${getQualityBadgeColor(analysis.qualityScore.inheritance)}">${"%.1f".format(analysis.qualityScore.inheritance)}</span></td>
            </tr>
            """
        }
    }
    
    private fun generateCompleteDetailsTable(analyses: List<ClassAnalysis>): String {
        return buildString {
            append("""<div class="table-responsive">""")
            append("""<table class="table table-hover" id="detailsTable">""")
            append("""<thead class="table-dark">""")
            append("""<tr>""")
            append("""<th class="sortable" data-column="name">Class <i class="fas fa-sort sort-indicator"></i></th>""")
            append("""<th class="sortable" data-column="quality">Quality <i class="fas fa-sort sort-indicator"></i></th>""")
            append("""<th class="sortable" data-column="lcom">LCOM <i class="fas fa-sort sort-indicator"></i></th>""")
            append("""<th class="sortable" data-column="wmc">WMC <i class="fas fa-sort sort-indicator"></i></th>""")
            append("""<th class="sortable" data-column="cbo">CBO <i class="fas fa-sort sort-indicator"></i></th>""")
            append("""<th class="sortable" data-column="dit">DIT <i class="fas fa-sort sort-indicator"></i></th>""")
            append("""<th class="sortable" data-column="risk">Risk <i class="fas fa-sort sort-indicator"></i></th>""")
            append("""<th>Actions</th>""")
            append("""</tr></thead><tbody>""")
            
            analyses.forEach { analysis ->
                append("""<tr class="table-row">""")
                append("""<td><strong>${analysis.className}</strong><br><small class="text-muted">${analysis.fileName}</small></td>""")
                append("""<td><span class="badge bg-${getQualityBadgeColor(analysis.qualityScore.overall)}">${"%.1f".format(analysis.qualityScore.overall)}</span></td>""")
                append("""<td>${analysis.lcom}</td>""")
                append("""<td>${analysis.ckMetrics.wmc}</td>""")
                append("""<td>${analysis.ckMetrics.cbo}</td>""")
                append("""<td>${analysis.ckMetrics.dit}</td>""")
                append("""<td><span class="badge bg-${getRiskBadgeColor(analysis.riskAssessment.level)}">${analysis.riskAssessment.level}</span></td>""")
                append("""<td><button class="btn btn-sm btn-outline-info" onclick="showClassDetails('${analysis.className}')"><i class="fas fa-eye"></i></button></td>""")
                append("""</tr>""")
            }
            
            append("""</tbody></table></div>""")
        }
    }
    
    private fun generateDataTable(analyses: List<ClassAnalysis>, rowGenerator: (ClassAnalysis) -> String): String {
        return buildString {
            append("""<div class="table-responsive">""")
            append("""<table class="table table-hover">""")
            append("""<tbody>""")
            analyses.forEach { analysis ->
                append(rowGenerator(analysis))
            }
            append("""</tbody></table></div>""")
        }
    }
    
    private fun generateLayersInfo(layeredArchitecture: LayeredArchitectureAnalysis): String {
        return buildString {
            if (layeredArchitecture.layers.isEmpty()) {
                append("""<p class="text-muted">No layer information available</p>""")
            } else {
                layeredArchitecture.layers.forEach { layer ->
                    append("""<div class="mb-2">""")
                    append("""<strong>${layer.name}</strong> (${layer.classes.size} classes)""")
                    append("""</div>""")
                }
            }
        }
    }
    
    private fun generateViolationsInfo(layeredArchitecture: LayeredArchitectureAnalysis): String {
        return buildString {
            if (layeredArchitecture.violations.isEmpty()) {
                append("""<p class="text-success"><i class="fas fa-check-circle me-2"></i>No architecture violations found!</p>""")
            } else {
                append("""<div class="list-group">""")
                layeredArchitecture.violations.forEach { violation ->
                    append("""<div class="list-group-item list-group-item-warning">""")
                    append("""<strong>${violation.violationType}</strong><br>""")
                    append("""<small>${violation.suggestion}</small>""")
                    append("""</div>""")
                }
                append("""</div>""")
            }
        }
    }
    
    private fun getQualityLevel(score: Double): String = when {
        score >= 9.0 -> "Excellent"
        score >= 7.0 -> "Good"
        score >= 5.0 -> "Moderate"
        else -> "Poor"
    }
    
    private fun getQualityBadgeColor(score: Double): String = when {
        score >= 9.0 -> "success"
        score >= 7.0 -> "info"
        score >= 5.0 -> "warning"
        else -> "danger"
    }
    
    private fun getRiskBadgeColor(level: RiskLevel): String = when (level) {
        RiskLevel.LOW -> "success"
        RiskLevel.MEDIUM -> "warning"
        RiskLevel.HIGH -> "warning"
        RiskLevel.CRITICAL -> "danger"
    }
    
    private fun generateJavaScript(analyses: List<ClassAnalysis>, architectureAnalysis: ArchitectureAnalysis): String {
        val jsGenerator = SimpleJavaScriptGenerator()
        return jsGenerator.generateJavaScript(analyses, architectureAnalysis)
    }
    
    private fun generateHtmlFooter(): String = """
</body>
</html>
"""
}