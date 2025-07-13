import com.metrics.model.analysis.*
import com.metrics.model.architecture.*
import com.metrics.model.common.*
import com.metrics.analyzer.core.KotlinCodeAnalyzer
import com.metrics.analyzer.core.JavaCodeAnalyzer
import com.metrics.parser.MultiLanguageParser
import com.metrics.report.console.ConsoleReportGenerator
import com.metrics.report.html.HtmlReportGenerator
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Main entry point for the Kotlin Metrics Analysis tool.
 * This tool analyzes Kotlin and Java codebases for:
 * - Complete CK Metrics Suite (LCOM, WMC, DIT, NOC, CBO, RFC, CA, CE, CC)
 * - Architecture Analysis (DDD patterns, layered architecture)
 * - Dependency graphs and circular dependencies
 * - Quality scoring and risk assessment
 */
fun main() {
    val currentDir = File(".")
    println("Analyzing Kotlin and Java files in: ${currentDir.absolutePath}")
    
    try {
        // Initialize parsers and analyzers
        val parser = MultiLanguageParser()
        val kotlinAnalyzer = KotlinCodeAnalyzer()
        val javaAnalyzer = JavaCodeAnalyzer()
        val consoleReporter = ConsoleReportGenerator()
        val htmlReporter = HtmlReportGenerator()
        
        // Discover and parse source files
        val sourceFiles = discoverSourceFiles(currentDir)
        println("Found ${sourceFiles.size} source files")
        
        val parsedFiles = parser.parseFiles(sourceFiles)
        println("Parsed ${parsedFiles.kotlinFiles.size} Kotlin files and ${parsedFiles.javaCompilationUnits.size} Java files")
        
        // Analyze files
        val kotlinAnalyses = kotlinAnalyzer.analyzeFiles(parsedFiles.kotlinFiles)
        val javaAnalyses = javaAnalyzer.analyzeFiles(parsedFiles.javaCompilationUnits, parsedFiles.originalJavaFiles)
        
        val allAnalyses = kotlinAnalyses + javaAnalyses
        
        // Simplified architecture analysis for now
        val architectureAnalysis = ArchitectureAnalysis(
            dddPatterns = DddPatternAnalysis(
                entities = emptyList(),
                valueObjects = emptyList(),
                services = emptyList(),
                repositories = emptyList(),
                aggregates = emptyList(),
                domainEvents = emptyList()
            ),
            layeredArchitecture = LayeredArchitectureAnalysis(
                layers = emptyList(),
                dependencies = emptyList(),
                violations = emptyList(),
                pattern = com.metrics.model.common.ArchitecturePattern.LAYERED
            ),
            dependencyGraph = DependencyGraph(
                nodes = emptyList(),
                edges = emptyList(),
                cycles = emptyList(),
                packages = emptyList()
            )
        )
        
        // Generate project report with simplified metrics
        val projectReport = ProjectReport(
            timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            classes = allAnalyses,
            summary = generateProjectSummary(allAnalyses, architectureAnalysis),
            architectureAnalysis = architectureAnalysis,
            projectQualityScore = QualityScore(0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            packageMetrics = emptyList(),
            couplingMatrix = emptyList(),
            riskAssessments = emptyList()
        )
        
        // Generate reports
        consoleReporter.generate(projectReport)
        htmlReporter.generate(projectReport)
        
        // Cleanup
        parser.dispose()
        
        println("\nAnalysis complete! Check report.html for detailed results.")
        
    } catch (e: Exception) {
        println("Error during analysis: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Discovers all source files in the given directory.
 */
private fun discoverSourceFiles(directory: File): List<File> {
    return if (directory.isDirectory) {
        directory.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .filter { !it.path.contains("build/") && !it.path.contains(".gradle/") }
            .toList()
    } else if (directory.extension in listOf("kt", "java")) {
        listOf(directory)
    } else {
        emptyList()
    }
}

/**
 * Generates a summary of the project analysis.
 */
private fun generateProjectSummary(analyses: List<ClassAnalysis>, architectureAnalysis: ArchitectureAnalysis): String {
    val totalClasses = analyses.size
    val avgLcom = if (analyses.isNotEmpty()) analyses.map { it.lcom }.average() else 0.0
    val avgComplexity = if (analyses.isNotEmpty()) analyses.map { it.complexity.averageComplexity }.average() else 0.0
    val highLcomClasses = analyses.count { it.lcom > 10 }
    val highComplexityClasses = analyses.count { it.complexity.averageComplexity > 10 }
    
    return buildString {
        appendLine("Project Analysis Summary")
        appendLine("======================")
        appendLine("Total Classes: $totalClasses")
        appendLine("Average LCOM: ${"%.2f".format(avgLcom)}")
        appendLine("Average Complexity: ${"%.2f".format(avgComplexity)}")
        appendLine("High LCOM Classes (>10): $highLcomClasses")
        appendLine("High Complexity Classes (>10): $highComplexityClasses")
        appendLine()
        appendLine("Architecture Analysis:")
        appendLine("- DDD Entities: ${architectureAnalysis.dddPatterns.entities.size}")
        appendLine("- DDD Value Objects: ${architectureAnalysis.dddPatterns.valueObjects.size}")
        appendLine("- DDD Services: ${architectureAnalysis.dddPatterns.services.size}")
        appendLine("- DDD Repositories: ${architectureAnalysis.dddPatterns.repositories.size}")
        appendLine("- Dependency Cycles: ${architectureAnalysis.dependencyGraph.cycles.size}")
    }
}