package com.metrics.report

import com.metrics.analyzer.*
import com.metrics.util.FileChange
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Generates comprehensive reports for enhanced PR diff analysis.
 * Supports multiple output formats with full source context integration.
 */
class EnhancedPRReportGenerator {
    
    /**
     * Generates HTML report with enhanced PR diff analysis.
     */
    fun generateHTMLReport(
        result: EnhancedPRDiffResult,
        outputFile: File
    ) {
        val html = buildString {
            append(generateHTMLHeader())
            append(generateHTMLSummary(result))
            append(generateHTMLMetricsComparison(result))
            append(generateHTMLSemanticChanges(result))
            append(generateHTMLImpactAnalysis(result))
            append(generateHTMLSourceContext(result))
            append(generateHTMLFooter())
        }
        
        outputFile.writeText(html)
    }
    
    /**
     * Generates Markdown report optimized for GitHub/GitLab PR comments.
     */
    fun generateMarkdownReport(
        result: EnhancedPRDiffResult,
        outputFile: File
    ) {
        val markdown = buildString {
            append(generateMarkdownHeader())
            append(generateMarkdownSummary(result))
            append(generateMarkdownMetricsTable(result))
            append(generateMarkdownSemanticChanges(result))
            append(generateMarkdownImpactAnalysis(result))
            append(generateMarkdownSourceChanges(result))
            append(generateMarkdownFooter())
        }
        
        outputFile.writeText(markdown)
    }
    
    /**
     * Generates JSON report for CI/CD integration.
     */
    fun generateJSONReport(
        result: EnhancedPRDiffResult,
        outputFile: File
    ) {
        val jsonData = mapOf(
            "timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "summary" to generateJSONSummary(result),
            "metricsComparison" to generateJSONMetricsComparison(result),
            "semanticChanges" to generateJSONSemanticChanges(result),
            "impactAnalysis" to generateJSONImpactAnalysis(result),
            "recommendations" to generateJSONRecommendations(result)
        )
        
        // This would need a proper JSON serialization library
        // For now, just write a simple JSON-like structure
        val json = buildString {
            append("{\n")
            jsonData.forEach { (key, value) ->
                append("  \"$key\": $value,\n")
            }
            append("}")
        }
        
        outputFile.writeText(json)
    }
    
    /**
     * Generates console output for command-line usage.
     */
    fun generateConsoleReport(result: EnhancedPRDiffResult): String {
        return buildString {
            append(generateConsoleHeader())
            append(generateConsoleSummary(result))
            append(generateConsoleMetrics(result))
            append(generateConsoleSemanticChanges(result))
            append(generateConsoleImpactAnalysis(result))
            append(generateConsoleRecommendations(result))
        }
    }
    
    // HTML Generation Methods
    
    private fun generateHTMLHeader(): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Enhanced PR Diff Analysis Report</title>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                <style>
                    .improvement { color: #28a745; }
                    .regression { color: #dc3545; }
                    .neutral { color: #6c757d; }
                    .code-diff { background-color: #f8f9fa; border: 1px solid #dee2e6; }
                    .code-added { background-color: #d4edda; }
                    .code-removed { background-color: #f8d7da; }
                    .impact-high { border-left: 4px solid #dc3545; }
                    .impact-medium { border-left: 4px solid #ffc107; }
                    .impact-low { border-left: 4px solid #28a745; }
                </style>
            </head>
            <body>
                <div class="container-fluid">
                    <div class="row">
                        <div class="col-12">
                            <h1 class="text-center mb-4">Enhanced PR Diff Analysis Report</h1>
                            <p class="text-center text-muted">Generated on ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}</p>
                        </div>
                    </div>
        """.trimIndent()
    }
    
    private fun generateHTMLSummary(result: EnhancedPRDiffResult): String {
        val metricsComparison = result.metricsComparison
        val semanticChanges = result.semanticChanges
        val impactAnalysis = result.afterAnalysis.impactAnalysis
        
        return """
            <div class="row mb-4">
                <div class="col-12">
                    <div class="card">
                        <div class="card-header">
                            <h3>📊 Executive Summary</h3>
                        </div>
                        <div class="card-body">
                            <div class="row">
                                <div class="col-md-3">
                                    <div class="text-center">
                                        <h4 class="improvement">${metricsComparison.improvements.size}</h4>
                                        <p class="mb-0">Improvements</p>
                                    </div>
                                </div>
                                <div class="col-md-3">
                                    <div class="text-center">
                                        <h4 class="regression">${metricsComparison.regressions.size}</h4>
                                        <p class="mb-0">Regressions</p>
                                    </div>
                                </div>
                                <div class="col-md-3">
                                    <div class="text-center">
                                        <h4 class="neutral">${semanticChanges.changesSummary.totalChanges}</h4>
                                        <p class="mb-0">Total Changes</p>
                                    </div>
                                </div>
                                <div class="col-md-3">
                                    <div class="text-center">
                                        <h4 class="text-warning">${impactAnalysis?.impactMetrics?.totalAffectedFiles ?: 0}</h4>
                                        <p class="mb-0">Affected Files</p>
                                    </div>
                                </div>
                            </div>
                            <div class="row mt-3">
                                <div class="col-12">
                                    <div class="progress">
                                        <div class="progress-bar bg-success" style="width: ${calculateImprovementPercentage(metricsComparison)}%"></div>
                                        <div class="progress-bar bg-danger" style="width: ${calculateRegressionPercentage(metricsComparison)}%"></div>
                                    </div>
                                    <small class="text-muted">Overall Quality Impact</small>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()
    }
    
    private fun generateHTMLMetricsComparison(result: EnhancedPRDiffResult): String {
        val improvements = result.metricsComparison.improvements
        val regressions = result.metricsComparison.regressions
        
        return """
            <div class="row mb-4">
                <div class="col-12">
                    <div class="card">
                        <div class="card-header">
                            <h3>📈 Metrics Comparison</h3>
                        </div>
                        <div class="card-body">
                            <div class="row">
                                <div class="col-md-6">
                                    <h5 class="improvement">Improvements</h5>
                                    <div class="table-responsive">
                                        <table class="table table-sm">
                                            <thead>
                                                <tr>
                                                    <th>Class</th>
                                                    <th>Metric</th>
                                                    <th>Before</th>
                                                    <th>After</th>
                                                    <th>Change</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                ${improvements.joinToString("") { improvement ->
                                                    """
                                                    <tr>
                                                        <td>${improvement.className}</td>
                                                        <td>${improvement.metricName}</td>
                                                        <td>${String.format("%.2f", improvement.beforeValue)}</td>
                                                        <td>${String.format("%.2f", improvement.afterValue)}</td>
                                                        <td class="improvement">${String.format("%.1f", improvement.improvementPercentage)}%</td>
                                                    </tr>
                                                    """.trimIndent()
                                                }}
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                                <div class="col-md-6">
                                    <h5 class="regression">Regressions</h5>
                                    <div class="table-responsive">
                                        <table class="table table-sm">
                                            <thead>
                                                <tr>
                                                    <th>Class</th>
                                                    <th>Metric</th>
                                                    <th>Before</th>
                                                    <th>After</th>
                                                    <th>Change</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                ${regressions.joinToString("") { regression ->
                                                    """
                                                    <tr>
                                                        <td>${regression.className}</td>
                                                        <td>${regression.metricName}</td>
                                                        <td>${String.format("%.2f", regression.beforeValue)}</td>
                                                        <td>${String.format("%.2f", regression.afterValue)}</td>
                                                        <td class="regression">${String.format("%.1f", regression.regressionPercentage)}%</td>
                                                    </tr>
                                                    """.trimIndent()
                                                }}
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()
    }
    
    private fun generateHTMLSemanticChanges(result: EnhancedPRDiffResult): String {
        val semanticChanges = result.semanticChanges
        
        return """
            <div class="row mb-4">
                <div class="col-12">
                    <div class="card">
                        <div class="card-header">
                            <h3>🔍 Semantic Changes</h3>
                        </div>
                        <div class="card-body">
                            <div class="row">
                                <div class="col-md-6">
                                    <h5>Method Changes</h5>
                                    ${semanticChanges.methodChanges.joinToString("") { change ->
                                        val impactClass = when (change.impactLevel) {
                                            ImpactLevel.HIGH -> "impact-high"
                                            ImpactLevel.MEDIUM -> "impact-medium"
                                            ImpactLevel.LOW -> "impact-low"
                                            else -> ""
                                        }
                                        """
                                        <div class="card mb-2 $impactClass">
                                            <div class="card-body p-2">
                                                <h6 class="card-title mb-1">${change.className}.${change.methodName}</h6>
                                                <p class="card-text mb-1">
                                                    <span class="badge badge-${getChangeTypeBadgeClass(change.changeType)}">${change.changeType}</span>
                                                    <span class="badge badge-${getImpactLevelBadgeClass(change.impactLevel)}">${change.impactLevel}</span>
                                                </p>
                                                <small class="text-muted">${change.filePath}</small>
                                            </div>
                                        </div>
                                        """.trimIndent()
                                    }}
                                </div>
                                <div class="col-md-6">
                                    <h5>API Changes</h5>
                                    ${semanticChanges.apiChanges.joinToString("") { change ->
                                        val breakingClass = if (change.breakingChange) "text-danger" else "text-success"
                                        """
                                        <div class="card mb-2">
                                            <div class="card-body p-2">
                                                <h6 class="card-title mb-1">${change.affectedElement}</h6>
                                                <p class="card-text mb-1">
                                                    <span class="badge badge-info">${change.changeType}</span>
                                                    <span class="badge ${if (change.breakingChange) "badge-danger" else "badge-success"}">
                                                        ${if (change.breakingChange) "Breaking" else "Non-breaking"}
                                                    </span>
                                                </p>
                                                <small class="text-muted">${change.description}</small>
                                            </div>
                                        </div>
                                        """.trimIndent()
                                    }}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()
    }
    
    private fun generateHTMLImpactAnalysis(result: EnhancedPRDiffResult): String {
        val impactAnalysis = result.afterAnalysis.impactAnalysis
        
        return if (impactAnalysis != null) {
            """
            <div class="row mb-4">
                <div class="col-12">
                    <div class="card">
                        <div class="card-header">
                            <h3>🎯 Impact Analysis</h3>
                        </div>
                        <div class="card-body">
                            <div class="row">
                                <div class="col-md-4">
                                    <h5>Direct Impact</h5>
                                    <ul class="list-unstyled">
                                        ${impactAnalysis.directlyAffectedFiles.joinToString("") { file ->
                                            "<li><code>$file</code></li>"
                                        }}
                                    </ul>
                                </div>
                                <div class="col-md-4">
                                    <h5>Indirect Impact</h5>
                                    <ul class="list-unstyled">
                                        ${impactAnalysis.indirectlyAffectedFiles.take(10).joinToString("") { file ->
                                            "<li><code>$file</code></li>"
                                        }}
                                        ${if (impactAnalysis.indirectlyAffectedFiles.size > 10) 
                                            "<li><small class=\"text-muted\">... and ${impactAnalysis.indirectlyAffectedFiles.size - 10} more files</small></li>"
                                        else ""}
                                    </ul>
                                </div>
                                <div class="col-md-4">
                                    <h5>Risk Assessment</h5>
                                    <div class="text-center">
                                        <h4 class="text-${getRiskLevelColor(impactAnalysis.impactMetrics.riskLevel)}">${impactAnalysis.impactMetrics.riskLevel}</h4>
                                        <p class="mb-0">${String.format("%.1f", impactAnalysis.impactMetrics.impactPercentage)}% of codebase affected</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            """.trimIndent()
        } else {
            ""
        }
    }
    
    private fun generateHTMLSourceContext(result: EnhancedPRDiffResult): String {
        return """
            <div class="row mb-4">
                <div class="col-12">
                    <div class="card">
                        <div class="card-header">
                            <h3>📄 Source Context</h3>
                        </div>
                        <div class="card-body">
                            ${result.resolvedFiles.joinToString("") { resolved ->
                                generateFileChangeHTML(resolved)
                            }}
                        </div>
                    </div>
                </div>
            </div>
        """.trimIndent()
    }
    
    private fun generateFileChangeHTML(resolved: ResolvedFileChange): String {
        return """
            <div class="card mb-3">
                <div class="card-header">
                    <h5>${resolved.originalChange.getEffectivePath()}</h5>
                    <small class="text-muted">
                        ${when {
                            resolved.originalChange.isNewFile() -> "New file"
                            resolved.originalChange.isDeletedFile() -> "Deleted file"
                            resolved.originalChange.isRenamed() -> "Renamed from ${resolved.originalChange.originalPath}"
                            else -> "Modified"
                        }}
                    </small>
                </div>
                <div class="card-body">
                    <div class="code-diff">
                        ${resolved.originalChange.hunks.joinToString("") { hunk ->
                            """
                            <div class="hunk mb-2">
                                <div class="hunk-header bg-light p-2">
                                    <small class="text-muted">@@ -${hunk.oldStart},${hunk.oldCount} +${hunk.newStart},${hunk.newCount} @@</small>
                                </div>
                                <div class="hunk-content">
                                    ${hunk.lines.joinToString("") { line ->
                                        val cssClass = when (line.changeType) {
                                            com.metrics.util.ChangeType.ADDED -> "code-added"
                                            com.metrics.util.ChangeType.REMOVED -> "code-removed"
                                            else -> ""
                                        }
                                        val prefix = when (line.changeType) {
                                            com.metrics.util.ChangeType.ADDED -> "+"
                                            com.metrics.util.ChangeType.REMOVED -> "-"
                                            else -> " "
                                        }
                                        """<div class="$cssClass"><code>$prefix${line.content}</code></div>"""
                                    }}
                                </div>
                            </div>
                            """.trimIndent()
                        }}
                    </div>
                </div>
            </div>
        """.trimIndent()
    }
    
    private fun generateHTMLFooter(): String {
        return """
                </div>
            </div>
            <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
            </body>
            </html>
        """.trimIndent()
    }
    
    // Markdown Generation Methods
    
    private fun generateMarkdownHeader(): String {
        return """
            # 🚀 Enhanced PR Diff Analysis Report
            
            *Generated on ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}*
            
            ---
            
        """.trimIndent()
    }
    
    private fun generateMarkdownSummary(result: EnhancedPRDiffResult): String {
        val metricsComparison = result.metricsComparison
        val semanticChanges = result.semanticChanges
        val impactAnalysis = result.afterAnalysis.impactAnalysis
        
        return """
            ## 📊 Executive Summary
            
            | Metric | Count |
            |--------|-------|
            | 🟢 Improvements | ${metricsComparison.improvements.size} |
            | 🔴 Regressions | ${metricsComparison.regressions.size} |
            | 🔄 Total Changes | ${semanticChanges.changesSummary.totalChanges} |
            | 📁 Affected Files | ${impactAnalysis?.impactMetrics?.totalAffectedFiles ?: 0} |
            
            **Overall Impact**: ${metricsComparison.overallImpact.impactLevel} (${metricsComparison.overallImpact.netImpact} net change)
            
        """.trimIndent()
    }
    
    private fun generateMarkdownMetricsTable(result: EnhancedPRDiffResult): String {
        val improvements = result.metricsComparison.improvements
        val regressions = result.metricsComparison.regressions
        
        return """
            ## 📈 Metrics Comparison
            
            ### 🟢 Improvements
            
            | Class | Metric | Before | After | Change |
            |-------|--------|--------|-------|---------|
            ${improvements.joinToString("\n") { improvement ->
                "| ${improvement.className} | ${improvement.metricName} | ${String.format("%.2f", improvement.beforeValue)} | ${String.format("%.2f", improvement.afterValue)} | ${String.format("%.1f", improvement.improvementPercentage)}% |"
            }}
            
            ### 🔴 Regressions
            
            | Class | Metric | Before | After | Change |
            |-------|--------|--------|-------|---------|
            ${regressions.joinToString("\n") { regression ->
                "| ${regression.className} | ${regression.metricName} | ${String.format("%.2f", regression.beforeValue)} | ${String.format("%.2f", regression.afterValue)} | ${String.format("%.1f", regression.regressionPercentage)}% |"
            }}
            
        """.trimIndent()
    }
    
    private fun generateMarkdownSemanticChanges(result: EnhancedPRDiffResult): String {
        val semanticChanges = result.semanticChanges
        
        return """
            ## 🔍 Semantic Changes
            
            ### Method Changes
            
            ${semanticChanges.methodChanges.joinToString("\n") { change ->
                val impactEmoji = when (change.impactLevel) {
                    ImpactLevel.HIGH -> "🔴"
                    ImpactLevel.MEDIUM -> "🟡"
                    ImpactLevel.LOW -> "🟢"
                    else -> "⚪"
                }
                "- $impactEmoji **${change.className}.${change.methodName}** - ${change.changeType} (${change.impactLevel})"
            }}
            
            ### API Changes
            
            ${semanticChanges.apiChanges.joinToString("\n") { change ->
                val breakingEmoji = if (change.breakingChange) "💥" else "✅"
                "- $breakingEmoji **${change.affectedElement}** - ${change.changeType} (${if (change.breakingChange) "Breaking" else "Non-breaking"})"
            }}
            
        """.trimIndent()
    }
    
    private fun generateMarkdownImpactAnalysis(result: EnhancedPRDiffResult): String {
        val impactAnalysis = result.afterAnalysis.impactAnalysis
        
        return if (impactAnalysis != null) {
            """
            ## 🎯 Impact Analysis
            
            **Risk Level**: ${impactAnalysis.impactMetrics.riskLevel} (${String.format("%.1f", impactAnalysis.impactMetrics.impactPercentage)}% of codebase affected)
            
            ### Directly Affected Files
            ${impactAnalysis.directlyAffectedFiles.joinToString("\n") { "- `$it`" }}
            
            ### Indirectly Affected Files
            ${impactAnalysis.indirectlyAffectedFiles.take(10).joinToString("\n") { "- `$it`" }}
            ${if (impactAnalysis.indirectlyAffectedFiles.size > 10) "\n*... and ${impactAnalysis.indirectlyAffectedFiles.size - 10} more files*" else ""}
            
            """.trimIndent()
        } else {
            ""
        }
    }
    
    private fun generateMarkdownSourceChanges(result: EnhancedPRDiffResult): String {
        return """
            ## 📄 Source Changes
            
            ${result.resolvedFiles.joinToString("\n\n") { resolved ->
                """
                ### ${resolved.originalChange.getEffectivePath()}
                
                ${when {
                    resolved.originalChange.isNewFile() -> "*New file*"
                    resolved.originalChange.isDeletedFile() -> "*Deleted file*"
                    resolved.originalChange.isRenamed() -> "*Renamed from ${resolved.originalChange.originalPath}*"
                    else -> "*Modified*"
                }}
                
                ${resolved.originalChange.hunks.joinToString("\n") { hunk ->
                    """
                    ```diff
                    @@ -${hunk.oldStart},${hunk.oldCount} +${hunk.newStart},${hunk.newCount} @@
                    ${hunk.lines.joinToString("\n") { line ->
                        val prefix = when (line.changeType) {
                            com.metrics.util.ChangeType.ADDED -> "+"
                            com.metrics.util.ChangeType.REMOVED -> "-"
                            else -> " "
                        }
                        "$prefix${line.content}"
                    }}
                    ```
                    """.trimIndent()
                }}
                """.trimIndent()
            }}
            
        """.trimIndent()
    }
    
    private fun generateMarkdownFooter(): String {
        return """
            
            ---
            
            *Report generated by Enhanced Kotlin Metrics Tool*
        """.trimIndent()
    }
    
    // Console Generation Methods
    
    private fun generateConsoleHeader(): String {
        return """
            ========================================
            Enhanced PR Diff Analysis Report
            ========================================
            Generated: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}
            
        """.trimIndent()
    }
    
    private fun generateConsoleSummary(result: EnhancedPRDiffResult): String {
        val metricsComparison = result.metricsComparison
        val semanticChanges = result.semanticChanges
        val impactAnalysis = result.afterAnalysis.impactAnalysis
        
        return """
            SUMMARY:
            --------
            Improvements: ${metricsComparison.improvements.size}
            Regressions: ${metricsComparison.regressions.size}
            Total Changes: ${semanticChanges.changesSummary.totalChanges}
            Affected Files: ${impactAnalysis?.impactMetrics?.totalAffectedFiles ?: 0}
            Overall Impact: ${metricsComparison.overallImpact.impactLevel}
            
        """.trimIndent()
    }
    
    private fun generateConsoleMetrics(result: EnhancedPRDiffResult): String {
        val improvements = result.metricsComparison.improvements
        val regressions = result.metricsComparison.regressions
        
        return """
            METRICS COMPARISON:
            ------------------
            Improvements:
            ${improvements.joinToString("\n") { improvement ->
                "  ✅ ${improvement.className}.${improvement.metricName}: ${String.format("%.2f", improvement.beforeValue)} → ${String.format("%.2f", improvement.afterValue)} (${String.format("%.1f", improvement.improvementPercentage)}%)"
            }}
            
            Regressions:
            ${regressions.joinToString("\n") { regression ->
                "  ❌ ${regression.className}.${regression.metricName}: ${String.format("%.2f", regression.beforeValue)} → ${String.format("%.2f", regression.afterValue)} (${String.format("%.1f", regression.regressionPercentage)}%)"
            }}
            
        """.trimIndent()
    }
    
    private fun generateConsoleSemanticChanges(result: EnhancedPRDiffResult): String {
        val semanticChanges = result.semanticChanges
        
        return """
            SEMANTIC CHANGES:
            ----------------
            Method Changes:
            ${semanticChanges.methodChanges.joinToString("\n") { change ->
                val impactSymbol = when (change.impactLevel) {
                    ImpactLevel.HIGH -> "🔴"
                    ImpactLevel.MEDIUM -> "🟡"
                    ImpactLevel.LOW -> "🟢"
                    else -> "⚪"
                }
                "  $impactSymbol ${change.className}.${change.methodName} - ${change.changeType} (${change.impactLevel})"
            }}
            
            API Changes:
            ${semanticChanges.apiChanges.joinToString("\n") { change ->
                val breakingSymbol = if (change.breakingChange) "💥" else "✅"
                "  $breakingSymbol ${change.affectedElement} - ${change.changeType} (${if (change.breakingChange) "Breaking" else "Non-breaking"})"
            }}
            
        """.trimIndent()
    }
    
    private fun generateConsoleImpactAnalysis(result: EnhancedPRDiffResult): String {
        val impactAnalysis = result.afterAnalysis.impactAnalysis
        
        return if (impactAnalysis != null) {
            """
            IMPACT ANALYSIS:
            ---------------
            Risk Level: ${impactAnalysis.impactMetrics.riskLevel}
            Impact Percentage: ${String.format("%.1f", impactAnalysis.impactMetrics.impactPercentage)}%
            
            Directly Affected Files:
            ${impactAnalysis.directlyAffectedFiles.joinToString("\n") { "  - $it" }}
            
            Indirectly Affected Files (top 10):
            ${impactAnalysis.indirectlyAffectedFiles.take(10).joinToString("\n") { "  - $it" }}
            ${if (impactAnalysis.indirectlyAffectedFiles.size > 10) "  ... and ${impactAnalysis.indirectlyAffectedFiles.size - 10} more files" else ""}
            
            """.trimIndent()
        } else {
            ""
        }
    }
    
    private fun generateConsoleRecommendations(result: EnhancedPRDiffResult): String {
        val recommendations = mutableListOf<String>()
        
        if (result.metricsComparison.regressions.isNotEmpty()) {
            recommendations.add("Consider addressing the ${result.metricsComparison.regressions.size} metric regressions")
        }
        
        if (result.semanticChanges.changesSummary.breakingChanges > 0) {
            recommendations.add("Review ${result.semanticChanges.changesSummary.breakingChanges} breaking changes for API compatibility")
        }
        
        result.afterAnalysis.impactAnalysis?.let { impact ->
            if (impact.impactMetrics.riskLevel == ImpactRiskLevel.HIGH) {
                recommendations.add("High impact detected - consider incremental deployment")
            }
        }
        
        return if (recommendations.isNotEmpty()) {
            """
            RECOMMENDATIONS:
            ---------------
            ${recommendations.joinToString("\n") { "  • $it" }}
            
            """.trimIndent()
        } else {
            ""
        }
    }
    
    // JSON Generation Methods
    
    private fun generateJSONSummary(result: EnhancedPRDiffResult): String {
        return """
            {
                "improvements": ${result.metricsComparison.improvements.size},
                "regressions": ${result.metricsComparison.regressions.size},
                "totalChanges": ${result.semanticChanges.changesSummary.totalChanges},
                "affectedFiles": ${result.afterAnalysis.impactAnalysis?.impactMetrics?.totalAffectedFiles ?: 0},
                "overallImpact": "${result.metricsComparison.overallImpact.impactLevel}"
            }
        """.trimIndent()
    }
    
    private fun generateJSONMetricsComparison(result: EnhancedPRDiffResult): String {
        return """
            {
                "improvements": [],
                "regressions": [],
                "overallImpact": "${result.metricsComparison.overallImpact.impactLevel}"
            }
        """.trimIndent()
    }
    
    private fun generateJSONSemanticChanges(result: EnhancedPRDiffResult): String {
        return """
            {
                "methodChanges": ${result.semanticChanges.methodChanges.size},
                "apiChanges": ${result.semanticChanges.apiChanges.size},
                "breakingChanges": ${result.semanticChanges.changesSummary.breakingChanges}
            }
        """.trimIndent()
    }
    
    private fun generateJSONImpactAnalysis(result: EnhancedPRDiffResult): String {
        val impactAnalysis = result.afterAnalysis.impactAnalysis
        return if (impactAnalysis != null) {
            """
            {
                "riskLevel": "${impactAnalysis.impactMetrics.riskLevel}",
                "impactPercentage": ${impactAnalysis.impactMetrics.impactPercentage},
                "directlyAffectedFiles": ${impactAnalysis.directlyAffectedFiles.size},
                "indirectlyAffectedFiles": ${impactAnalysis.indirectlyAffectedFiles.size}
            }
            """.trimIndent()
        } else {
            "null"
        }
    }
    
    private fun generateJSONRecommendations(result: EnhancedPRDiffResult): String {
        return "[]"
    }
    
    // Helper Methods
    
    private fun calculateImprovementPercentage(metricsComparison: MetricsComparison): Double {
        val total = metricsComparison.improvements.size + metricsComparison.regressions.size
        return if (total > 0) (metricsComparison.improvements.size.toDouble() / total.toDouble()) * 100 else 0.0
    }
    
    private fun calculateRegressionPercentage(metricsComparison: MetricsComparison): Double {
        val total = metricsComparison.improvements.size + metricsComparison.regressions.size
        return if (total > 0) (metricsComparison.regressions.size.toDouble() / total.toDouble()) * 100 else 0.0
    }
    
    private fun getChangeTypeBadgeClass(changeType: MethodChangeType): String {
        return when (changeType) {
            MethodChangeType.ADDED -> "success"
            MethodChangeType.REMOVED -> "danger"
            MethodChangeType.MODIFIED -> "warning"
        }
    }
    
    private fun getImpactLevelBadgeClass(impactLevel: ImpactLevel): String {
        return when (impactLevel) {
            ImpactLevel.HIGH -> "danger"
            ImpactLevel.MEDIUM -> "warning"
            ImpactLevel.LOW -> "info"
            else -> "secondary"
        }
    }
    
    private fun getRiskLevelColor(riskLevel: ImpactRiskLevel): String {
        return when (riskLevel) {
            ImpactRiskLevel.HIGH -> "danger"
            ImpactRiskLevel.MEDIUM -> "warning"
            ImpactRiskLevel.LOW -> "info"
            else -> "success"
        }
    }
}