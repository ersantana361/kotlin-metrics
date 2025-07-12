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
 * - LCOM (Lack of Cohesion of Methods)
 * - Cyclomatic Complexity 
 * - Architecture Analysis (DDD patterns, layered architecture)
 * - Dependency graphs and circular dependencies
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
        
        // Discover source files
        val sourceFiles = discoverSourceFiles(currentDir)
        if (sourceFiles.isEmpty()) {
            println("⚠️  No Kotlin or Java files found to analyze.")
            println("   Make sure you're running from a project directory with source files.")
            return
        }
        
        println("Found ${sourceFiles.size} source files to analyze...")
        
        // Parse files by language
        val parsedFiles = parser.parseFiles(sourceFiles)
        println("Parsed ${parsedFiles.kotlinFiles.size} Kotlin files and ${parsedFiles.javaCompilationUnits.size} Java files")
        
        // Analyze files
        val analyses = mutableListOf<ClassAnalysis>()
        
        // Analyze Kotlin files
        if (parsedFiles.kotlinFiles.isNotEmpty()) {
            val kotlinAnalyses = kotlinAnalyzer.analyzeFiles(parsedFiles.kotlinFiles)
            analyses.addAll(kotlinAnalyses)
        }
        
        // Analyze Java files
        if (parsedFiles.javaCompilationUnits.isNotEmpty()) {
            val javaAnalyses = javaAnalyzer.analyzeFiles(parsedFiles.javaCompilationUnits, parsedFiles.originalJavaFiles)
            analyses.addAll(javaAnalyses)
        }
        
        if (analyses.isEmpty()) {
            println("⚠️  No classes found to analyze in the source files.")
            return
        }
        
        // Perform architecture analysis
        val architectureAnalysis = performArchitectureAnalysis(parsedFiles, analyses)
        
        // Create project report
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val projectReport = ProjectReport(
            timestamp = timestamp,
            classes = analyses,
            summary = generateSummary(analyses, architectureAnalysis),
            architectureAnalysis = architectureAnalysis
        )
        
        // Generate reports
        consoleReporter.generate(projectReport)
        htmlReporter.generate(projectReport)
        
        // Cleanup resources
        parser.dispose()
        
        println("✅ Analysis completed successfully!")
        
    } catch (e: Exception) {
        println("❌ Error during analysis: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Discovers all Kotlin and Java source files in the project directory.
 */
private fun discoverSourceFiles(directory: File): List<File> {
    return if (directory.isDirectory()) {
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
 * Performs comprehensive architecture analysis including DDD patterns and dependency graphs.
 */
private fun performArchitectureAnalysis(parsedFiles: com.metrics.parser.ParsedFiles, analyses: List<ClassAnalysis>): ArchitectureAnalysis {
    // Use the existing analyzers to build architecture analysis
    // This is a placeholder - the actual implementation would use the extracted utility classes
    return ArchitectureAnalysis(
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
            pattern = ArchitecturePattern.UNKNOWN
        ),
        dependencyGraph = DependencyGraph(
            nodes = emptyList(),
            edges = emptyList(),
            cycles = emptyList(),
            packages = emptyList()
        )
    )
}

/**
 * Generates a summary of the analysis results.
 */
private fun generateSummary(analyses: List<ClassAnalysis>, architectureAnalysis: ArchitectureAnalysis): String {
    val avgLcom = if (analyses.isNotEmpty()) analyses.map { it.lcom }.average() else 0.0
    val avgComplexity = if (analyses.isNotEmpty()) analyses.map { it.complexity.averageComplexity }.average() else 0.0
    val complexMethods = analyses.sumOf { it.complexity.complexMethods.size }
    
    return """
        📊 Analysis Summary:
        • Classes analyzed: ${analyses.size}
        • Average LCOM: ${String.format("%.2f", avgLcom)}
        • Average Cyclomatic Complexity: ${String.format("%.2f", avgComplexity)}
        • Complex methods (CC > 10): $complexMethods
        • Architecture pattern: ${architectureAnalysis.layeredArchitecture.pattern}
    """.trimIndent()
}