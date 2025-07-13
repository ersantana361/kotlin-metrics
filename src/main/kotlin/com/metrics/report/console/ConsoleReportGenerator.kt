package com.metrics.report.console

import com.metrics.model.analysis.ClassAnalysis
import com.metrics.model.analysis.ProjectReport
import com.metrics.report.ReportGenerator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Generates console-based reports for code analysis results.
 */
class ConsoleReportGenerator : ReportGenerator {
    
    override fun generate(report: ProjectReport) {
        generateSummary(report.classes, report.architectureAnalysis)
    }
    
    private fun generateSummary(analyses: List<ClassAnalysis>, architectureAnalysis: com.metrics.model.architecture.ArchitectureAnalysis) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        
        println("\n" + "=".repeat(60))
        println("               📊 KOTLIN METRICS ANALYSIS SUMMARY")
        println("                    Generated: $timestamp")
        println("=".repeat(60))
        
        if (analyses.isEmpty()) {
            println("\n⚠️  No classes found to analyze.")
            println("   Make sure you're running from a project directory with source files.")
            return
        }
        
        generateProjectOverview(analyses)
        generateKeyMetrics(analyses)
        generateQualityDistribution(analyses)
        generateIssuesSummary(analyses)
        generateWorstOffenders(analyses)
        generateArchitectureSummary(architectureAnalysis)
        
        println("\n📄 Detailed HTML report generated: kotlin-metrics-report.html")
        println("=".repeat(60))
    }
    
    private fun generateProjectOverview(analyses: List<ClassAnalysis>) {
        val totalMethods = analyses.sumOf { it.complexity.methods.size }
        val totalProperties = analyses.sumOf { it.propertyCount }
        
        println("\n📈 PROJECT OVERVIEW")
        println("   Classes analyzed: ${analyses.size}")
        println("   Total methods: $totalMethods")
        println("   Total properties: $totalProperties")
    }
    
    private fun generateKeyMetrics(analyses: List<ClassAnalysis>) {
        val avgLcom = analyses.map { it.lcom }.average()
        val avgComplexity = analyses.map { it.complexity.averageComplexity }.average()
        
        println("\n🎯 KEY METRICS")
        println("   Average LCOM: ${String.format("%.2f", avgLcom)} ${getLcomQualityIcon(avgLcom)}")
        println("   Average Complexity: ${String.format("%.2f", avgComplexity)} ${getComplexityQualityIcon(avgComplexity)}")
    }
    
    private fun generateQualityDistribution(analyses: List<ClassAnalysis>) {
        val qualityDistribution = analyses.groupBy { 
            when {
                it.lcom <= 2 && it.complexity.averageComplexity <= 5 -> "Excellent"
                it.lcom <= 5 && it.complexity.averageComplexity <= 7 -> "Good"
                it.lcom <= 8 && it.complexity.averageComplexity <= 10 -> "Moderate"
                else -> "Poor"
            }
        }
        
        println("\n📊 QUALITY DISTRIBUTION")
        qualityDistribution.forEach { (level, classes) ->
            val icon = when (level) {
                "Excellent" -> "✅"
                "Good" -> "👍"
                "Moderate" -> "⚠️"
                else -> "❌"
            }
            val percentage = (classes.size * 100.0 / analyses.size).let { "%.1f".format(it) }
            println("   $icon $level: ${classes.size} classes ($percentage%)")
        }
    }
    
    private fun generateIssuesSummary(analyses: List<ClassAnalysis>) {
        val complexMethods = analyses.sumOf { it.complexity.complexMethods.size }
        val veryComplexMethods = analyses.sumOf { analysis -> 
            analysis.complexity.methods.count { it.cyclomaticComplexity > 20 }
        }
        val poorCohesionClasses = analyses.count { it.lcom > 5 }
        
        if (complexMethods > 0 || poorCohesionClasses > 0) {
            println("\n⚠️  ISSUES DETECTED")
            if (poorCohesionClasses > 0) {
                println("   📊 $poorCohesionClasses ${if (poorCohesionClasses == 1) "class has" else "classes have"} poor cohesion (LCOM > 5)")
            }
            if (complexMethods > 0) {
                println("   🧠 $complexMethods ${if (complexMethods == 1) "method is" else "methods are"} complex (CC > 10)")
            }
            if (veryComplexMethods > 0) {
                println("   🚨 $veryComplexMethods ${if (veryComplexMethods == 1) "method is" else "methods are"} very complex (CC > 20)")
            }
        } else {
            println("\n✨ EXCELLENT CODE QUALITY")
            println("   No significant issues detected!")
            println("   All classes have good cohesion and low complexity.")
        }
    }
    
    private fun generateWorstOffenders(analyses: List<ClassAnalysis>) {
        val worstClasses = analyses
            .filter { it.lcom > 5 || it.complexity.averageComplexity > 10 }
            .sortedWith(compareByDescending<ClassAnalysis> { it.lcom }.thenByDescending { it.complexity.averageComplexity })
        
        if (worstClasses.isNotEmpty()) {
            println("\n🎯 PRIORITY REFACTORING TARGETS")
            worstClasses.take(5).forEach { analysis ->
                val lcomBadge = if (analysis.lcom > 5) "LCOM:${analysis.lcom}" else ""
                val ccBadge = if (analysis.complexity.averageComplexity > 10) "CC:${String.format("%.1f", analysis.complexity.averageComplexity)}" else ""
                val badges = listOf(lcomBadge, ccBadge).filter { it.isNotEmpty() }.joinToString(" ")
                println("   📝 ${analysis.className} ($badges)")
            }
        }
    }
    
    private fun generateArchitectureSummary(architectureAnalysis: com.metrics.model.architecture.ArchitectureAnalysis) {
        println("\n🏗️ ARCHITECTURE ANALYSIS")
        println("   Pattern: ${architectureAnalysis.layeredArchitecture.pattern}")
        println("   Layers: ${architectureAnalysis.layeredArchitecture.layers.size}")
        println("   Dependencies: ${architectureAnalysis.layeredArchitecture.dependencies.size}")
        println("   Violations: ${architectureAnalysis.layeredArchitecture.violations.size}")
        
        // DDD Patterns Summary
        val ddd = architectureAnalysis.dddPatterns
        println("   DDD Patterns: ${ddd.entities.size} entities, ${ddd.services.size} services, ${ddd.repositories.size} repositories")
        
        // Dependency Graph Summary
        val graph = architectureAnalysis.dependencyGraph
        println("   Dependency Graph: ${graph.nodes.size} nodes, ${graph.edges.size} edges, ${graph.cycles.size} cycles")
    }
    
    private fun getLcomQualityIcon(avgLcom: Double): String = when {
        avgLcom <= 2 -> "✅"
        avgLcom <= 5 -> "👍"
        avgLcom <= 8 -> "⚠️"
        else -> "❌"
    }
    
    private fun getComplexityQualityIcon(avgComplexity: Double): String = when {
        avgComplexity <= 5 -> "✅"
        avgComplexity <= 10 -> "👍"
        avgComplexity <= 15 -> "⚠️"
        else -> "❌"
    }
}