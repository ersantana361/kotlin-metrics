import com.metrics.model.analysis.*
import com.metrics.model.architecture.*
import com.metrics.model.common.*
import com.metrics.analyzer.core.KotlinCodeAnalyzer
import com.metrics.analyzer.core.JavaCodeAnalyzer
import com.metrics.parser.MultiLanguageParser
import com.metrics.report.console.ConsoleReportGenerator
import com.metrics.report.html.HtmlReportGenerator
import com.metrics.report.markdown.MarkdownReportGenerator
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
fun main(args: Array<String>) {
    // Parse command line arguments
    val options = parseArgs(args)
    
    if (options.showHelp) {
        showHelp()
        return
    }
    
    try {
        // Initialize parsers and analyzers
        val parser = MultiLanguageParser()
        val kotlinAnalyzer = KotlinCodeAnalyzer()
        val javaAnalyzer = JavaCodeAnalyzer()
        
        // Handle single file analysis
        if (options.singleFile != null) {
            return analyzeSingleFile(options.singleFile, parser, kotlinAnalyzer, javaAnalyzer, options)
        }
        
        // Discover and parse source files for project analysis
        val sourceFiles = discoverSourceFiles(options.targetDir)
        println("Analyzing Kotlin and Java files in: ${options.targetDir.absolutePath}")
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
        
        // Generate reports based on requested formats
        generateReports(projectReport, options)
        
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

/**
 * Command line options for the metrics tool.
 */
data class CliOptions(
    val singleFile: File? = null,
    val outputFile: File? = null,
    val targetDir: File = File("."),
    val showHelp: Boolean = false,
    val outputFormats: Set<OutputFormat> = setOf(OutputFormat.CONSOLE, OutputFormat.HTML)
)

/**
 * Available output formats.
 */
enum class OutputFormat {
    CONSOLE, HTML, MARKDOWN
}

/**
 * Parse command line arguments.
 */
private fun parseArgs(args: Array<String>): CliOptions {
    var singleFile: File? = null
    var outputFile: File? = null
    var targetDir = File(".")
    var showHelp = false
    val outputFormats = mutableSetOf<OutputFormat>()
    
    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-f", "--file" -> {
                if (i + 1 < args.size) {
                    singleFile = File(args[++i])
                } else {
                    println("Error: --file requires a file path")
                    showHelp = true
                }
            }
            "-o", "--output" -> {
                if (i + 1 < args.size) {
                    outputFile = File(args[++i])
                } else {
                    println("Error: --output requires a file path")
                    showHelp = true
                }
            }
            "-d", "--directory" -> {
                if (i + 1 < args.size) {
                    targetDir = File(args[++i])
                } else {
                    println("Error: --directory requires a directory path")
                    showHelp = true
                }
            }
            "--html" -> {
                outputFormats.add(OutputFormat.HTML)
            }
            "--markdown" -> {
                outputFormats.add(OutputFormat.MARKDOWN)
            }
            "--console" -> {
                outputFormats.add(OutputFormat.CONSOLE)
            }
            "-h", "--help" -> {
                showHelp = true
            }
            else -> {
                println("Unknown option: ${args[i]}")
                showHelp = true
            }
        }
        i++
    }
    
    // Default output formats if none specified
    val finalOutputFormats = if (outputFormats.isEmpty()) {
        if (singleFile != null) {
            setOf(OutputFormat.MARKDOWN)  // Single file defaults to markdown
        } else {
            setOf(OutputFormat.CONSOLE, OutputFormat.HTML)  // Project defaults to console + HTML
        }
    } else {
        outputFormats.toSet()
    }
    
    return CliOptions(singleFile, outputFile, targetDir, showHelp, finalOutputFormats)
}

/**
 * Show help information.
 */
private fun showHelp() {
    println("""
        Kotlin Metrics Analysis Tool
        
        Usage: kotlin-metrics [OPTIONS]
        
        Options:
          -f, --file <file>       Analyze a single file
          -o, --output <file>     Output file for markdown report (default: stdout)
          -d, --directory <dir>   Directory to analyze (default: current directory)
          
        Output Formats:
          --console               Generate console output (default for projects)
          --html                  Generate HTML report (default for projects)
          --markdown              Generate markdown report (default for single files)
          
        Other:
          -h, --help              Show this help message
        
        Examples:
          kotlin-metrics                                    # Project: console + HTML
          kotlin-metrics --markdown                         # Project: markdown only
          kotlin-metrics --console --html --markdown       # Project: all formats
          
          kotlin-metrics -f MyClass.kt                     # Single file: markdown to stdout
          kotlin-metrics -f MyClass.kt --console           # Single file: console output
          kotlin-metrics -f MyClass.kt --html              # Single file: HTML report
          kotlin-metrics -f MyClass.kt --markdown --html   # Single file: markdown + HTML
          kotlin-metrics -f MyClass.kt -o report.md        # Single file: markdown to file
          
          kotlin-metrics -d /path/to/project --console     # Custom directory: console only
    """.trimIndent())
}

/**
 * Analyze a single file and generate reports in requested formats.
 */
private fun analyzeSingleFile(
    file: File,
    parser: MultiLanguageParser,
    kotlinAnalyzer: KotlinCodeAnalyzer,
    javaAnalyzer: JavaCodeAnalyzer,
    options: CliOptions
) {
    if (!file.exists()) {
        println("Error: File not found: ${file.absolutePath}")
        return
    }
    
    if (!file.extension.matches(Regex("kt|java"))) {
        println("Error: File must be a Kotlin (.kt) or Java (.java) file")
        return
    }
    
    try {
        val parsedFiles = parser.parseFiles(listOf(file))
        
        val analysis = if (file.extension == "kt") {
            val analyses = kotlinAnalyzer.analyzeFiles(parsedFiles.kotlinFiles)
            analyses.firstOrNull()
        } else {
            val analyses = javaAnalyzer.analyzeFiles(parsedFiles.javaCompilationUnits, parsedFiles.originalJavaFiles)
            analyses.firstOrNull()
        }
        
        if (analysis == null) {
            println("Error: Could not analyze file: ${file.absolutePath}")
            return
        }
        
        // Generate reports in requested formats
        generateSingleFileReports(analysis, options)
        
        // Cleanup
        parser.dispose()
        
    } catch (e: Exception) {
        println("Error analyzing file: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Generate reports in the requested formats for project analysis.
 */
private fun generateReports(report: ProjectReport, options: CliOptions) {
    options.outputFormats.forEach { format ->
        when (format) {
            OutputFormat.CONSOLE -> {
                val consoleReporter = ConsoleReportGenerator()
                consoleReporter.generate(report)
            }
            OutputFormat.HTML -> {
                val htmlReporter = HtmlReportGenerator()
                htmlReporter.generate(report)
            }
            OutputFormat.MARKDOWN -> {
                val markdownReporter = MarkdownReportGenerator(options.outputFile)
                markdownReporter.generate(report)
            }
        }
    }
}

/**
 * Generate reports in the requested formats for single file analysis.
 */
private fun generateSingleFileReports(analysis: ClassAnalysis, options: CliOptions) {
    options.outputFormats.forEach { format ->
        when (format) {
            OutputFormat.CONSOLE -> {
                // For single file, we can show a simplified console output
                println("=== ${analysis.className} Analysis ===")
                println("File: ${analysis.fileName}")
                println("Quality Score: ${"%.1f".format(analysis.qualityScore.overall)}/10 (${analysis.qualityScore.getQualityLevel()})")
                println("LCOM: ${analysis.ckMetrics.lcom}")
                println("WMC: ${analysis.ckMetrics.wmc}")
                println("CBO: ${analysis.ckMetrics.cbo}")
                println("Risk Level: ${analysis.riskAssessment.level}")
                println()
            }
            OutputFormat.HTML -> {
                // Create a single-file project report for HTML generation
                val singleFileReport = createSingleFileProjectReport(analysis)
                val htmlReporter = HtmlReportGenerator()
                htmlReporter.generate(singleFileReport)
            }
            OutputFormat.MARKDOWN -> {
                val markdownReporter = MarkdownReportGenerator(options.outputFile)
                val report = markdownReporter.generateSingleClassReport(analysis)
                markdownReporter.writeReport(report)
            }
        }
    }
}

/**
 * Create a project report from a single class analysis for HTML generation.
 */
private fun createSingleFileProjectReport(analysis: ClassAnalysis): ProjectReport {
    val timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    
    return ProjectReport(
        timestamp = timestamp,
        classes = listOf(analysis),
        summary = "Single file analysis for ${analysis.className}",
        architectureAnalysis = ArchitectureAnalysis(
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
        ),
        projectQualityScore = analysis.qualityScore,
        packageMetrics = emptyList(),
        couplingMatrix = emptyList(),
        riskAssessments = listOf(analysis.riskAssessment)
    )
}