package com.metrics.report.markdown

import com.metrics.model.analysis.*
import com.metrics.report.ReportGenerator
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Generates Markdown reports from analysis results.
 * Can generate both single-file reports and project-wide reports.
 */
class MarkdownReportGenerator(
    private val outputFile: File? = null
) : ReportGenerator {

    override fun generate(report: ProjectReport) {
        val content = generateMarkdownContent(report)
        writeReport(content)
    }
    
    /**
     * Check if an output file is configured.
     */
    fun hasOutputFile(): Boolean = outputFile != null
    
    /**
     * Write a report to the configured output file or stdout.
     */
    fun writeReport(content: String) {
        if (outputFile != null) {
            outputFile.writeText(content)
            println("Markdown report generated: ${outputFile.absolutePath}")
        } else {
            println(content)
        }
    }

    /**
     * Generates a markdown report for a single class analysis.
     */
    fun generateSingleClassReport(classAnalysis: ClassAnalysis): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val quality = classAnalysis.qualityScore
        val ck = classAnalysis.ckMetrics
        val risk = classAnalysis.riskAssessment
        
        return """
            # Code Metrics Report

            **File:** `${classAnalysis.fileName}`  
            **Class:** `${classAnalysis.className}`  
            **Generated:** $timestamp

            ## ${quality.getQualityEmoji()} Quality Overview

            | Metric | Score | Level |
            |--------|-------|-------|
            | Overall Quality | ${"%.1f".format(quality.overall)}/10 | ${quality.getQualityLevel()} |
            | Cohesion | ${"%.1f".format(quality.cohesion)}/10 | ${getScoreLevel(quality.cohesion)} |
            | Complexity | ${"%.1f".format(quality.complexity)}/10 | ${getScoreLevel(quality.complexity)} |
            | Coupling | ${"%.1f".format(quality.coupling)}/10 | ${getScoreLevel(quality.coupling)} |
            | Inheritance | ${"%.1f".format(quality.inheritance)}/10 | ${getScoreLevel(quality.inheritance)} |
            | Architecture | ${"%.1f".format(quality.architecture)}/10 | ${getScoreLevel(quality.architecture)} |

            ## 📊 Complete CK Metrics Suite

            ### Cohesion Metrics
            | Metric | Value | Description |
            |--------|-------|-------------|
            | **LCOM** | ${ck.lcom} | Lack of Cohesion of Methods ${getLcomInterpretation(ck.lcom)} |

            ### Complexity Metrics
            | Metric | Value | Description |
            |--------|-------|-------------|
            | **WMC** | ${ck.wmc} | Weighted Methods per Class ${getWmcInterpretation(ck.wmc)} |
            | **CC** | ${ck.cyclomaticComplexity} | Cyclomatic Complexity ${getCcInterpretation(ck.cyclomaticComplexity)} |

            ### Coupling Metrics
            | Metric | Value | Description |
            |--------|-------|-------------|
            | **CBO** | ${ck.cbo} | Coupling Between Objects ${getCboInterpretation(ck.cbo)} |
            | **RFC** | ${ck.rfc} | Response For a Class ${getRfcInterpretation(ck.rfc)} |
            | **CA** | ${ck.ca} | Afferent Coupling ${getCaInterpretation(ck.ca)} |
            | **CE** | ${ck.ce} | Efferent Coupling ${getCeInterpretation(ck.ce)} |

            ### Inheritance Metrics
            | Metric | Value | Description |
            |--------|-------|-------------|
            | **DIT** | ${ck.dit} | Depth of Inheritance Tree ${getDitInterpretation(ck.dit)} |
            | **NOC** | ${ck.noc} | Number of Children ${getNocInterpretation(ck.noc)} |

            ## 🔍 Detailed Analysis

            ### Class Structure
            - **Methods:** ${classAnalysis.methodCount}
            - **Properties:** ${classAnalysis.propertyCount}
            - **Average Method Complexity:** ${"%.1f".format(classAnalysis.complexity.averageComplexity)}
            - **Max Method Complexity:** ${classAnalysis.complexity.maxComplexity}

            ${generateMethodComplexityTable(classAnalysis.complexity)}

            ## ⚠️ Risk Assessment

            **Risk Level:** ${getRiskEmoji(risk.level)} ${risk.level}  
            **Impact:** ${risk.impact}  
            **Priority:** ${risk.priority}/10

            ${generateRiskFactors(risk.reasons)}

            ${generateSuggestions(classAnalysis.suggestions)}

            ## 📖 Metrics Reference

            ### CK Metrics Explained
            - **LCOM (Lack of Cohesion of Methods):** Measures how well methods and properties work together. Lower is better.
            - **WMC (Weighted Methods per Class):** Sum of complexities of all methods. Lower is better.
            - **CBO (Coupling Between Objects):** Number of classes this class depends on. Lower is better.
            - **RFC (Response For a Class):** Total methods that can be invoked. Lower is better.
            - **CA (Afferent Coupling):** Number of classes that depend on this class. Higher indicates more responsibility.
            - **CE (Efferent Coupling):** Number of classes this class depends on. Lower is better.
            - **DIT (Depth of Inheritance Tree):** Distance from root class. Moderate values are best.
            - **NOC (Number of Children):** Direct subclasses. Higher indicates abstraction.
            - **CC (Cyclomatic Complexity):** Measures code complexity through control flow. Lower is better.

            ---
            *Generated by Kotlin Metrics Tool*
        """.trimIndent()
    }

    private fun generateMarkdownContent(report: ProjectReport): String {
        val avgScore = report.classes.map { it.qualityScore.overall }.average()
        val qualityDist = report.classes.groupBy { it.qualityScore.getQualityLevel() }
        val riskCounts = report.classes.groupBy { it.riskAssessment.level }
        
        val stats = mapOf(
            "LCOM" to report.classes.map { it.ckMetrics.lcom },
            "WMC" to report.classes.map { it.ckMetrics.wmc },
            "CBO" to report.classes.map { it.ckMetrics.cbo },
            "CC" to report.classes.map { it.ckMetrics.cyclomaticComplexity }
        )
        
        return """
            # Project Code Metrics Report

            **Generated:** ${report.timestamp}  
            **Classes Analyzed:** ${report.classes.size}

            ## 📊 Project Overview

            **Average Quality Score:** ${"%.1f".format(avgScore)}/10

            ### Quality Distribution
            ${qualityDist.entries.joinToString("\n") { (level, classes) ->
                val emoji = getQualityEmoji(level)
                "- $emoji **$level:** ${classes.size} classes"
            }}

            ## 📈 Summary Statistics

            | Metric | Min | Max | Avg | High Risk Count |
            |--------|-----|-----|-----|-----------------|
            ${stats.entries.joinToString("\n") { (name, values) ->
                val threshold = when(name) {
                    "LCOM" -> 10
                    "WMC" -> 50
                    "CBO" -> 10
                    "CC" -> 20
                    else -> 10
                }
                "| $name | ${values.minOrNull() ?: 0} | ${values.maxOrNull() ?: 0} | ${"%.1f".format(values.average())} | ${values.count { it > threshold }} |"
            }}

            ## ⚠️ Risk Assessment

            ${RiskLevel.values().joinToString("\n") { level ->
                val count = riskCounts[level]?.size ?: 0
                val emoji = getRiskEmoji(level)
                "- $emoji **${level.name}:** $count classes"
            }}

            ## 🔍 Detailed Class Analysis

            ${report.classes.sortedByDescending { it.qualityScore.overall }.joinToString("\n\n") { classAnalysis ->
                generateClassSummary(classAnalysis)
            }}

            ---
            *Generated by Kotlin Metrics Tool*
        """.trimIndent()
    }

    private fun generateClassSummary(classAnalysis: ClassAnalysis): String {
        val quality = classAnalysis.qualityScore
        val ck = classAnalysis.ckMetrics
        val risk = classAnalysis.riskAssessment
        
        val suggestions = if (classAnalysis.suggestions.isNotEmpty()) {
            "\n\n**Suggestions:**\n${classAnalysis.suggestions.take(3).joinToString("\n") { "- ${it.message}" }}"
        } else ""
        
        return """
            ### ${quality.getQualityEmoji()} ${classAnalysis.className}

            **File:** `${classAnalysis.fileName}`  
            **Quality:** ${"%.1f".format(quality.overall)}/10 (${quality.getQualityLevel()})  
            **Risk:** ${getRiskEmoji(risk.level)} ${risk.level}

            | Metric | Value | Score |
            |--------|-------|-------|
            | LCOM | ${ck.lcom} | ${"%.1f".format(quality.cohesion)} |
            | WMC | ${ck.wmc} | ${"%.1f".format(quality.complexity)} |
            | CBO | ${ck.cbo} | ${"%.1f".format(quality.coupling)} |
            | DIT | ${ck.dit} | ${"%.1f".format(quality.inheritance)} |$suggestions
        """.trimIndent()
    }

    private fun generateMethodComplexityTable(complexity: ComplexityAnalysis): String {
        if (complexity.methods.isEmpty()) return ""
        
        val header = """
            ### Method Complexity Breakdown
            | Method | Complexity | Risk Level |
            |--------|------------|------------|
        """.trimIndent()
        
        val rows = complexity.methods.joinToString("\n") { method ->
            val riskLevel = when {
                method.cyclomaticComplexity > 10 -> "🚨 High"
                method.cyclomaticComplexity > 5 -> "🟡 Medium"
                else -> "🟢 Low"
            }
            "| `${method.methodName}` | ${method.cyclomaticComplexity} | $riskLevel |"
        }
        
        return "$header\n$rows\n"
    }

    private fun generateRiskFactors(reasons: List<String>): String {
        if (reasons.isEmpty()) return ""
        
        return """
            ### Risk Factors
            ${reasons.joinToString("\n") { "- $it" }}
        """.trimIndent()
    }

    private fun generateSuggestions(suggestions: List<Suggestion>): String {
        if (suggestions.isEmpty()) return ""
        
        return """
            ## 💡 Improvement Suggestions

            ${suggestions.joinToString("\n\n") { suggestion ->
                """
                ### ${suggestion.icon}
                - **Message:** ${suggestion.message}
                - **Tooltip:** ${suggestion.tooltip}
                """.trimIndent()
            }}
        """.trimIndent()
    }

    private fun getScoreLevel(score: Double): String = when {
        score >= 8.0 -> "Excellent"
        score >= 6.0 -> "Good"
        score >= 4.0 -> "Moderate"
        score >= 2.0 -> "Poor"
        else -> "Critical"
    }

    private fun getQualityEmoji(level: String): String = when(level) {
        "Excellent" -> "🟢"
        "Good" -> "🟡"
        "Moderate" -> "🟠"
        "Poor" -> "🔴"
        "Critical" -> "🚨"
        else -> "⚪"
    }

    private fun getRiskEmoji(level: RiskLevel): String = when (level) {
        RiskLevel.LOW -> "🟢"
        RiskLevel.MEDIUM -> "🟡"
        RiskLevel.HIGH -> "🔴"
        RiskLevel.CRITICAL -> "🚨"
    }

    private fun getLcomInterpretation(lcom: Int): String = when {
        lcom == 0 -> "🟢 Perfect cohesion"
        lcom <= 5 -> "🟢 Good cohesion"
        lcom <= 10 -> "🟡 Moderate cohesion"
        lcom <= 20 -> "🔴 Poor cohesion"
        else -> "🚨 Very poor cohesion"
    }

    private fun getWmcInterpretation(wmc: Int): String = when {
        wmc <= 10 -> "🟢 Low complexity"
        wmc <= 20 -> "🟡 Moderate complexity"
        wmc <= 50 -> "🔴 High complexity"
        else -> "🚨 Very high complexity"
    }

    private fun getCcInterpretation(cc: Int): String = when {
        cc <= 10 -> "🟢 Low complexity"
        cc <= 20 -> "🟡 Moderate complexity"
        cc <= 50 -> "🔴 High complexity"
        else -> "🚨 Very high complexity"
    }

    private fun getCboInterpretation(cbo: Int): String = when {
        cbo <= 5 -> "🟢 Low coupling"
        cbo <= 10 -> "🟡 Moderate coupling"
        cbo <= 20 -> "🔴 High coupling"
        else -> "🚨 Very high coupling"
    }

    private fun getRfcInterpretation(rfc: Int): String = when {
        rfc <= 20 -> "🟢 Low response"
        rfc <= 50 -> "🟡 Moderate response"
        rfc <= 100 -> "🔴 High response"
        else -> "🚨 Very high response"
    }

    private fun getCaInterpretation(ca: Int): String = when {
        ca <= 2 -> "🟢 Low afferent coupling"
        ca <= 5 -> "🟡 Moderate afferent coupling"
        ca <= 10 -> "🔴 High afferent coupling"
        else -> "🚨 Very high afferent coupling"
    }

    private fun getCeInterpretation(ce: Int): String = when {
        ce <= 5 -> "🟢 Low efferent coupling"
        ce <= 10 -> "🟡 Moderate efferent coupling"
        ce <= 20 -> "🔴 High efferent coupling"
        else -> "🚨 Very high efferent coupling"
    }

    private fun getDitInterpretation(dit: Int): String = when {
        dit <= 2 -> "🟢 Shallow inheritance"
        dit <= 4 -> "🟡 Moderate inheritance"
        dit <= 6 -> "🔴 Deep inheritance"
        else -> "🚨 Very deep inheritance"
    }

    private fun getNocInterpretation(noc: Int): String = when {
        noc == 0 -> "🟢 Leaf class"
        noc <= 3 -> "🟡 Few children"
        noc <= 7 -> "🔴 Many children"
        else -> "🚨 Too many children"
    }
}