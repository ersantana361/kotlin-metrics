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
        
        /* Enhanced tooltips and guides */
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
        
        /* Custom tooltips for metrics */
        .metric-tooltip {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 1rem;
            border-radius: 12px;
            max-width: 400px;
            box-shadow: 0 8px 25px rgba(0,0,0,0.3);
            font-size: 14px;
            line-height: 1.5;
        }
        
        .metric-tooltip h6 {
            color: #fff;
            margin-bottom: 0.5rem;
            font-weight: bold;
        }
        
        .metric-tooltip .metric-value {
            background: rgba(255,255,255,0.2);
            padding: 0.25rem 0.5rem;
            border-radius: 4px;
            font-weight: bold;
            display: inline-block;
            margin: 0.25rem 0;
        }
        
        /* Help icons */
        .help-icon {
            color: var(--info-color);
            cursor: help;
            margin-left: 8px;
            font-size: 0.9em;
            transition: color 0.2s;
        }
        
        .help-icon:hover {
            color: var(--primary-color);
        }
        
        /* Metric interpretation guides */
        .interpretation-guide {
            background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
            color: white;
            padding: 1.25rem;
            border-radius: 12px;
            margin: 1rem 0;
            box-shadow: 0 6px 20px rgba(0,0,0,0.15);
        }
        
        .interpretation-guide h6 {
            color: #fff;
            margin-bottom: 1rem;
            font-weight: 600;
            text-align: center;
        }
        
        .metric-scale {
            display: flex;
            justify-content: space-between;
            margin: 0.75rem 0;
            background: rgba(255,255,255,0.2);
            border-radius: 6px;
            padding: 0.5rem;
        }
        
        .scale-item {
            text-align: center;
            flex: 1;
            padding: 0.25rem;
            border-radius: 4px;
            font-size: 0.85em;
            font-weight: 500;
        }
        
        .scale-excellent { background: rgba(40, 167, 69, 0.8); }
        .scale-good { background: rgba(23, 162, 184, 0.8); }
        .scale-moderate { background: rgba(255, 193, 7, 0.8); }
        .scale-poor { background: rgba(220, 53, 69, 0.8); }
        
        /* Architecture guide styles */
        .architecture-guide {
            background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
            color: white;
            padding: 1.25rem;
            border-radius: 12px;
            margin: 1rem 0;
            box-shadow: 0 6px 20px rgba(0,0,0,0.15);
        }
        
        .ddd-pattern-guide {
            background: rgba(255,255,255,0.15);
            border-radius: 8px;
            padding: 0.75rem;
            margin: 0.5rem 0;
        }
        
        .pattern-confidence {
            display: inline-block;
            background: rgba(255,255,255,0.3);
            padding: 0.2rem 0.5rem;
            border-radius: 4px;
            font-size: 0.8em;
            font-weight: bold;
            margin-left: 0.5rem;
        }
        
        /* Info boxes */
        .info-box {
            background: #e7f3ff;
            border-left: 4px solid var(--info-color);
            padding: 1rem;
            margin: 1rem 0;
            border-radius: 0 8px 8px 0;
        }
        
        .info-box h6 {
            color: var(--primary-color);
            margin-bottom: 0.5rem;
        }
        
        .warning-box {
            background: #fff3cd;
            border-left: 4px solid var(--warning-color);
            padding: 1rem;
            margin: 1rem 0;
            border-radius: 0 8px 8px 0;
        }
        
        .warning-box h6 {
            color: #856404;
            margin-bottom: 0.5rem;
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
            <!-- Quality Score Interpretation Guide -->
            <div class="interpretation-guide">
                <h6><i class="fas fa-graduation-cap me-2"></i>Understanding Quality Scores</h6>
                <p>Quality scores range from 0-10 and combine multiple software metrics to assess code maintainability, reliability, and design quality.</p>
                <div class="metric-scale">
                    <div class="scale-item scale-excellent">9-10<br>Excellent</div>
                    <div class="scale-item scale-good">7-8<br>Good</div>
                    <div class="scale-item scale-moderate">5-6<br>Moderate</div>
                    <div class="scale-item scale-poor">0-4<br>Poor</div>
                </div>
                <small><strong>Formula:</strong> Cohesion (25%) + Complexity (25%) + Coupling (25%) + Inheritance (15%) + Architecture (10%)</small>
            </div>
        
            <div class="row">
                <!-- Project Summary Cards with Enhanced Tooltips -->
                <div class="col-md-3 col-sm-6 mb-4">
                    <div class="card metric-card text-center">
                        <div class="card-body">
                            <h3 class="text-primary">${totalClasses}</h3>
                            <p class="card-text">Total Classes 
                                <i class="fas fa-question-circle help-icon" 
                                   data-bs-toggle="tooltip" 
                                   data-bs-placement="top" 
                                   data-bs-html="true"
                                   title="<div class='metric-tooltip'><h6>Total Classes</h6>Number of classes analyzed in your codebase. <br><strong>Why it matters:</strong> Larger codebases require more attention to architecture and quality patterns.</div>"></i>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 col-sm-6 mb-4">
                    <div class="card metric-card text-center">
                        <div class="card-body">
                            <h3 class="text-info">%.1f</h3>
                            <p class="card-text">Overall Quality Score 
                                <i class="fas fa-question-circle help-icon" 
                                   data-bs-toggle="tooltip" 
                                   data-bs-placement="top" 
                                   data-bs-html="true"
                                   title="<div class='metric-tooltip'><h6>Quality Score</h6>Composite score (0-10) combining:<br>• <span class='metric-value'>Cohesion (25%)</span><br>• <span class='metric-value'>Complexity (25%)</span><br>• <span class='metric-value'>Coupling (25%)</span><br>• <span class='metric-value'>Inheritance (15%)</span><br>• <span class='metric-value'>Architecture (10%)</span><br><strong>Target:</strong> Aim for 7+ for maintainable code</div>"></i>
                            </p>
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
                            <p class="card-text">Avg LCOM 
                                <i class="fas fa-question-circle help-icon" 
                                   data-bs-toggle="tooltip" 
                                   data-bs-placement="top" 
                                   data-bs-html="true"
                                   title="<div class='metric-tooltip'><h6>LCOM (Lack of Cohesion of Methods)</h6>Measures how well methods in a class work together through shared properties.<br><br><span class='metric-value'>LCOM = 0:</span> Excellent cohesion<br><span class='metric-value'>LCOM 1-2:</span> Good cohesion<br><span class='metric-value'>LCOM 3-5:</span> Moderate cohesion<br><span class='metric-value'>LCOM >5:</span> Poor cohesion<br><br><strong>Impact:</strong> High LCOM suggests the class has multiple responsibilities and should be split.</div>"></i>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 col-sm-6 mb-4">
                    <div class="card metric-card text-center">
                        <div class="card-body">
                            <h3 class="text-danger">${highRiskClasses}</h3>
                            <p class="card-text">High Risk Classes 
                                <i class="fas fa-question-circle help-icon" 
                                   data-bs-toggle="tooltip" 
                                   data-bs-placement="top" 
                                   data-bs-html="true"
                                   title="<div class='metric-tooltip'><h6>High Risk Classes</h6>Classes with quality scores below 5.0 or specific risk factors:<br><br>• <span class='metric-value'>LCOM > 10:</span> Very poor cohesion<br>• <span class='metric-value'>WMC > 50:</span> Extremely complex<br>• <span class='metric-value'>CBO > 20:</span> Excessive coupling<br>• <span class='metric-value'>DIT > 6:</span> Deep inheritance<br><br><strong>Action:</strong> These classes should be prioritized for refactoring.</div>"></i>
                            </p>
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
            <!-- LCOM and Complexity Interpretation Guide -->
            <div class="interpretation-guide">
                <h6><i class="fas fa-puzzle-piece me-2"></i>Cohesion & Complexity Analysis</h6>
                <div class="row">
                    <div class="col-md-6">
                        <h6>LCOM (Lack of Cohesion of Methods)</h6>
                        <p><strong>What it measures:</strong> How well methods within a class work together by sharing instance properties.</p>
                        <div class="metric-scale">
                            <div class="scale-item scale-excellent">0<br>Perfect</div>
                            <div class="scale-item scale-good">1-2<br>Good</div>
                            <div class="scale-item scale-moderate">3-5<br>Fair</div>
                            <div class="scale-item scale-poor">6+<br>Poor</div>
                        </div>
                        <small><strong>Formula:</strong> LCOM = P - Q (minimum 0)<br>P = method pairs with no shared properties<br>Q = method pairs with shared properties</small>
                    </div>
                    <div class="col-md-6">
                        <h6>WMC (Weighted Methods per Class)</h6>
                        <p><strong>What it measures:</strong> Sum of cyclomatic complexity of all methods in a class.</p>
                        <div class="metric-scale">
                            <div class="scale-item scale-excellent">1-10<br>Simple</div>
                            <div class="scale-item scale-good">11-20<br>Moderate</div>
                            <div class="scale-item scale-moderate">21-50<br>Complex</div>
                            <div class="scale-item scale-poor">51+<br>Very Complex</div>
                        </div>
                        <small><strong>Impact:</strong> High WMC makes classes harder to understand, test, and maintain.</small>
                    </div>
                </div>
            </div>
            
            <div class="info-box">
                <h6><i class="fas fa-lightbulb me-2"></i>Improvement Tips</h6>
                <ul class="mb-0">
                    <li><strong>High LCOM:</strong> Consider splitting the class into smaller, more focused classes (Single Responsibility Principle)</li>
                    <li><strong>High WMC:</strong> Break down complex methods, extract utility functions, reduce nested conditions</li>
                    <li><strong>Both High:</strong> Class likely has multiple responsibilities - prime candidate for refactoring</li>
                </ul>
            </div>
        
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
            <!-- Coupling Metrics Interpretation Guide -->
            <div class="interpretation-guide">
                <h6><i class="fas fa-link me-2"></i>Understanding Coupling Metrics</h6>
                <div class="row">
                    <div class="col-md-6">
                        <h6>CBO (Coupling Between Objects)</h6>
                        <p><strong>What it measures:</strong> Number of classes this class is coupled to (bidirectional).</p>
                        <div class="metric-scale">
                            <div class="scale-item scale-excellent">0-5<br>Low</div>
                            <div class="scale-item scale-good">6-10<br>Moderate</div>
                            <div class="scale-item scale-moderate">11-20<br>High</div>
                            <div class="scale-item scale-poor">21+<br>Very High</div>
                        </div>
                        
                        <h6 class="mt-3">RFC (Response For a Class)</h6>
                        <p><strong>What it measures:</strong> Number of methods that can be invoked in response to a message.</p>
                        <div class="metric-scale">
                            <div class="scale-item scale-excellent">0-20<br>Low</div>
                            <div class="scale-item scale-good">21-40<br>Moderate</div>
                            <div class="scale-item scale-moderate">41-60<br>High</div>
                            <div class="scale-item scale-poor">61+<br>Very High</div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <h6>CA (Afferent Coupling)</h6>
                        <p><strong>What it measures:</strong> Number of classes that depend on this class (incoming dependencies).</p>
                        <div class="metric-scale">
                            <div class="scale-item scale-excellent">0-5<br>Low</div>
                            <div class="scale-item scale-good">6-10<br>Moderate</div>
                            <div class="scale-item scale-moderate">11-20<br>High</div>
                            <div class="scale-item scale-poor">21+<br>Very High</div>
                        </div>
                        
                        <h6 class="mt-3">CE (Efferent Coupling)</h6>
                        <p><strong>What it measures:</strong> Number of classes this class depends on (outgoing dependencies).</p>
                        <div class="metric-scale">
                            <div class="scale-item scale-excellent">0-5<br>Low</div>
                            <div class="scale-item scale-good">6-10<br>Moderate</div>
                            <div class="scale-item scale-moderate">11-20<br>High</div>
                            <div class="scale-item scale-poor">21+<br>Very High</div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="warning-box">
                <h6><i class="fas fa-exclamation-triangle me-2"></i>Why Coupling Matters</h6>
                <ul class="mb-0">
                    <li><strong>High CBO:</strong> Class is tightly coupled - changes ripple through many dependencies</li>
                    <li><strong>High RFC:</strong> Class interface is complex - difficult to understand and test</li>
                    <li><strong>High CA:</strong> Class is central to system - changes affect many other classes</li>
                    <li><strong>High CE:</strong> Class depends on many others - fragile to external changes</li>
                    <li><strong>Goal:</strong> Aim for loose coupling with focused responsibilities</li>
                </ul>
            </div>
        
            <div class="row">
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-exchange-alt me-2"></i>Coupling Between Objects (CBO)
                                <i class="fas fa-question-circle help-icon" 
                                   data-bs-toggle="tooltip" 
                                   data-bs-placement="top" 
                                   data-bs-html="true"
                                   title="<div class='metric-tooltip'><h6>CBO - Coupling Between Objects</h6>Counts unique classes this class references or is referenced by.<br><br><strong>Low CBO (0-5):</strong> Good isolation<br><strong>High CBO (20+):</strong> Tightly coupled, hard to maintain<br><br><strong>Reduce by:</strong> Dependency injection, interfaces, facade patterns</div>"></i>
                            </h5>
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
            <!-- Inheritance Metrics Interpretation Guide -->
            <div class="interpretation-guide">
                <h6><i class="fas fa-sitemap me-2"></i>Inheritance Design Analysis</h6>
                <div class="row">
                    <div class="col-md-6">
                        <h6>DIT (Depth of Inheritance Tree)</h6>
                        <p><strong>What it measures:</strong> Maximum length from the class to the root of inheritance tree.</p>
                        <div class="metric-scale">
                            <div class="scale-item scale-excellent">1-2<br>Shallow</div>
                            <div class="scale-item scale-good">3-4<br>Moderate</div>
                            <div class="scale-item scale-moderate">5-6<br>Deep</div>
                            <div class="scale-item scale-poor">7+<br>Very Deep</div>
                        </div>
                        <small><strong>Deep inheritance problems:</strong> Complex behavior understanding, difficult debugging, tight coupling</small>
                    </div>
                    <div class="col-md-6">
                        <h6>NOC (Number of Children)</h6>
                        <p><strong>What it measures:</strong> Number of immediate subclasses of a class.</p>
                        <div class="metric-scale">
                            <div class="scale-item scale-excellent">0-3<br>Focused</div>
                            <div class="scale-item scale-good">4-7<br>Moderate</div>
                            <div class="scale-item scale-moderate">8-15<br>Many</div>
                            <div class="scale-item scale-poor">16+<br>Too Many</div>
                        </div>
                        <small><strong>Many children indicate:</strong> Important abstraction or potential over-generalization</small>
                    </div>
                </div>
            </div>
            
            <div class="info-box">
                <h6><i class="fas fa-balance-scale me-2"></i>Inheritance Design Principles</h6>
                <div class="row">
                    <div class="col-md-4">
                        <strong>Favor Composition over Inheritance</strong><br>
                        <small>Use delegation and composition when inheritance creates deep hierarchies</small>
                    </div>
                    <div class="col-md-4">
                        <strong>Liskov Substitution Principle</strong><br>
                        <small>Subclasses should be substitutable for their base classes</small>
                    </div>
                    <div class="col-md-4">
                        <strong>Open/Closed Principle</strong><br>
                        <small>Design for extension without modification of existing code</small>
                    </div>
                </div>
            </div>
        
            <div class="row">
                <div class="col-md-6 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-layer-group me-2"></i>Depth of Inheritance Tree (DIT)
                                <i class="fas fa-question-circle help-icon" 
                                   data-bs-toggle="tooltip" 
                                   data-bs-placement="top" 
                                   data-bs-html="true"
                                   title="<div class='metric-tooltip'><h6>DIT - Depth of Inheritance Tree</h6>Measures how deep a class is in inheritance hierarchy.<br><br><strong>Shallow (1-2):</strong> Easy to understand<br><strong>Deep (6+):</strong> Complex, hard to debug<br><br><strong>Problems with deep inheritance:</strong><br>• Hard to understand behavior<br>• Difficult debugging<br>• Tight coupling<br>• Fragile base class problem</div>"></i>
                            </h5>
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
            <!-- Architecture Analysis Guide -->
            <div class="architecture-guide">
                <h6><i class="fas fa-building me-2"></i>Architecture Analysis Guide</h6>
                <div class="row">
                    <div class="col-md-6">
                        <h6>DDD (Domain-Driven Design) Patterns</h6>
                        <div class="ddd-pattern-guide">
                            <strong>Entities:</strong> Objects with identity and lifecycle<br>
                            <small>Confidence based on: ID fields, mutability, equals/hashCode</small>
                        </div>
                        <div class="ddd-pattern-guide">
                            <strong>Value Objects:</strong> Immutable objects defined by attributes<br>
                            <small>Confidence based on: immutability, data classes, no ID</small>
                        </div>
                        <div class="ddd-pattern-guide">
                            <strong>Services:</strong> Stateless operations on domain objects<br>
                            <small>Confidence based on: naming patterns, statelessness</small>
                        </div>
                        <div class="ddd-pattern-guide">
                            <strong>Repositories:</strong> Data access abstraction<br>
                            <small>Confidence based on: CRUD operations, data access patterns</small>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <h6>Layered Architecture Patterns</h6>
                        <div class="ddd-pattern-guide">
                            <strong>Presentation Layer:</strong> UI, controllers, API endpoints<br>
                            <small>Handles user interaction and data presentation</small>
                        </div>
                        <div class="ddd-pattern-guide">
                            <strong>Application Layer:</strong> Use cases, application services<br>
                            <small>Orchestrates domain operations and business workflows</small>
                        </div>
                        <div class="ddd-pattern-guide">
                            <strong>Domain Layer:</strong> Business logic, entities, value objects<br>
                            <small>Core business rules and domain model</small>
                        </div>
                        <div class="ddd-pattern-guide">
                            <strong>Infrastructure Layer:</strong> Database, external services<br>
                            <small>Technical implementation details and external integrations</small>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="info-box">
                <h6><i class="fas fa-search me-2"></i>How Pattern Detection Works</h6>
                <div class="row">
                    <div class="col-md-6">
                        <strong>Analysis Criteria:</strong>
                        <ul class="small mb-0">
                            <li>Class and package naming conventions</li>
                            <li>Method signatures and responsibilities</li>
                            <li>Field types and mutability patterns</li>
                            <li>Annotation usage (JPA, Spring, etc.)</li>
                        </ul>
                    </div>
                    <div class="col-md-6">
                        <strong>Confidence Levels:</strong>
                        <ul class="small mb-0">
                            <li><strong>90-100%:</strong> Strong indicators present</li>
                            <li><strong>70-89%:</strong> Good pattern match</li>
                            <li><strong>50-69%:</strong> Moderate confidence</li>
                            <li><strong>Below 50%:</strong> Weak pattern match</li>
                        </ul>
                    </div>
                </div>
            </div>
        
            <div class="row">
                <div class="col-md-8 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-project-diagram me-2"></i>Dependency Graph
                                <i class="fas fa-question-circle help-icon" 
                                   data-bs-toggle="tooltip" 
                                   data-bs-placement="top" 
                                   data-bs-html="true"
                                   title="<div class='metric-tooltip'><h6>Dependency Graph Visualization</h6>Interactive graph showing class relationships and dependencies.<br><br><strong>Node Colors:</strong><br>• <span class='metric-value'>Red:</span> Presentation Layer<br>• <span class='metric-value'>Orange:</span> Application Layer<br>• <span class='metric-value'>Green:</span> Domain Layer<br>• <span class='metric-value'>Blue:</span> Infrastructure Layer<br><br><strong>Edges:</strong> Dependencies between classes<br><strong>Cycles:</strong> Circular dependencies (red lines)</div>"></i>
                            </h5>
                        </div>
                        <div class="card-body">
                            <div id="dependencyGraph"></div>
                        </div>
                    </div>
                </div>
                <div class="col-md-4 mb-4">
                    <div class="card">
                        <div class="card-header">
                            <h5><i class="fas fa-layer-group me-2"></i>Architecture Layers
                                <i class="fas fa-question-circle help-icon" 
                                   data-bs-toggle="tooltip" 
                                   data-bs-placement="top" 
                                   data-bs-html="true"
                                   title="<div class='metric-tooltip'><h6>Architecture Layer Analysis</h6>Detected layers based on package structure and naming patterns.<br><br><strong>Good Architecture:</strong><br>• Clear layer separation<br>• Dependencies flow inward<br>• No circular dependencies<br>• Domain layer independence</div>"></i>
                            </h5>
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
            append("""<th class="sortable" data-column="quality">Quality 
                        <i class="fas fa-question-circle help-icon" 
                           data-bs-toggle="tooltip" 
                           data-bs-placement="top" 
                           data-bs-html="true"
                           title="<div class='metric-tooltip'><h6>Quality Score (0-10)</h6>Composite score combining all CK metrics</div>"></i>
                        <i class="fas fa-sort sort-indicator"></i></th>""")
            append("""<th class="sortable" data-column="lcom">LCOM 
                        <i class="fas fa-question-circle help-icon" 
                           data-bs-toggle="tooltip" 
                           data-bs-placement="top" 
                           data-bs-html="true"
                           title="<div class='metric-tooltip'><h6>Lack of Cohesion of Methods</h6>0 = Perfect cohesion<br>Higher values = Poor cohesion</div>"></i>
                        <i class="fas fa-sort sort-indicator"></i></th>""")
            append("""<th class="sortable" data-column="wmc">WMC 
                        <i class="fas fa-question-circle help-icon" 
                           data-bs-toggle="tooltip" 
                           data-bs-placement="top" 
                           data-bs-html="true"
                           title="<div class='metric-tooltip'><h6>Weighted Methods per Class</h6>Sum of cyclomatic complexity of all methods</div>"></i>
                        <i class="fas fa-sort sort-indicator"></i></th>""")
            append("""<th class="sortable" data-column="cbo">CBO 
                        <i class="fas fa-question-circle help-icon" 
                           data-bs-toggle="tooltip" 
                           data-bs-placement="top" 
                           data-bs-html="true"
                           title="<div class='metric-tooltip'><h6>Coupling Between Objects</h6>Number of classes this class is coupled to</div>"></i>
                        <i class="fas fa-sort sort-indicator"></i></th>""")
            append("""<th class="sortable" data-column="dit">DIT 
                        <i class="fas fa-question-circle help-icon" 
                           data-bs-toggle="tooltip" 
                           data-bs-placement="top" 
                           data-bs-html="true"
                           title="<div class='metric-tooltip'><h6>Depth of Inheritance Tree</h6>Maximum inheritance depth from root</div>"></i>
                        <i class="fas fa-sort sort-indicator"></i></th>""")
            append("""<th class="sortable" data-column="risk">Risk 
                        <i class="fas fa-question-circle help-icon" 
                           data-bs-toggle="tooltip" 
                           data-bs-placement="top" 
                           data-bs-html="true"
                           title="<div class='metric-tooltip'><h6>Risk Assessment</h6>Based on quality score and metric thresholds</div>"></i>
                        <i class="fas fa-sort sort-indicator"></i></th>""")
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