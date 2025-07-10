import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.psi.KtFile
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class MethodComplexity(
    val methodName: String,
    val cyclomaticComplexity: Int,
    val lineCount: Int
)

data class ComplexityAnalysis(
    val methods: List<MethodComplexity>,
    val totalComplexity: Int,
    val averageComplexity: Double,
    val maxComplexity: Int,
    val complexMethods: List<MethodComplexity> // CC > 10
)

data class ClassAnalysis(
    val className: String,
    val fileName: String,
    val lcom: Int,
    val methodCount: Int,
    val propertyCount: Int,
    val methodDetails: Map<String, Set<String>>,
    val suggestions: List<Suggestion>,
    val complexity: ComplexityAnalysis
)

data class ProjectReport(
    val timestamp: String,
    val classes: List<ClassAnalysis>,
    val summary: String
)

fun main() {
    val currentDir = File(".")
    println("Analyzing Kotlin files in: ${currentDir.absolutePath}")
    
    val target = currentDir

    val files = if (target.isDirectory()) {
        target.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
    } else {
        listOf(target)
    }

    val disposable: Disposable = Disposer.newDisposable()
    val configuration = CompilerConfiguration()
    configuration.put(CommonConfigurationKeys.MODULE_NAME, "kotlin-metrics")
    configuration.addJvmClasspathRoot(File("."))
    val env = KotlinCoreEnvironment.createForProduction(
        disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
    )

    val psiFileFactory = PsiFileFactory.getInstance(env.project)
    val analyses = mutableListOf<ClassAnalysis>()

    for (file in files) {
        val ktFile = psiFileFactory.createFileFromText(
            file.name,
            KotlinLanguage.INSTANCE,
            file.readText()
        ) as KtFile

        for (classOrObject in ktFile.declarations.filterIsInstance<KtClassOrObject>()) {
            val analysis = analyzeClass(classOrObject, file.name)
            analyses.add(analysis)
        }
    }

    generateSummary(analyses)
    generateHtmlReport(analyses)

    Disposer.dispose(disposable)
}

fun analyzeClass(classOrObject: KtClassOrObject, fileName: String): ClassAnalysis {
    val className = classOrObject.name ?: "Anonymous"
    val props = classOrObject.declarations.filterIsInstance<KtProperty>().map { it.name!! }
    val methods = classOrObject.body?.functions ?: emptyList()

    val methodProps = mutableMapOf<String, Set<String>>()

    for (funDecl in methods) {
        val usedProps = mutableSetOf<String>()
        funDecl.bodyExpression?.forEachDescendantOfType<KtNameReferenceExpression> {
            val name = it.getReferencedName()
            if (props.contains(name)) {
                usedProps.add(name)
            }
        }
        methodProps[funDecl.name ?: "anonymous"] = usedProps
    }

    // Calculate LCOM
    val methodsList = methodProps.entries.toList()
    var pairsWithoutCommon = 0
    var pairsWithCommon = 0

    for (i in methodsList.indices) {
        for (j in i + 1 until methodsList.size) {
            val props1 = methodsList[i].value
            val props2 = methodsList[j].value
            if (props1.intersect(props2).isEmpty()) {
                pairsWithoutCommon++
            } else {
                pairsWithCommon++
            }
        }
    }

    var lcom = pairsWithoutCommon - pairsWithCommon
    if (lcom < 0) lcom = 0

    val complexity = analyzeComplexity(methods)
    val suggestions = generateSuggestions(lcom, methodProps, props, complexity)

    return ClassAnalysis(
        className = className,
        fileName = fileName,
        lcom = lcom,
        methodCount = methods.size,
        propertyCount = props.size,
        methodDetails = methodProps,
        suggestions = suggestions,
        complexity = complexity
    )
}

data class Suggestion(
    val icon: String,
    val message: String,
    val tooltip: String
)

fun analyzeComplexity(methods: List<KtNamedFunction>): ComplexityAnalysis {
    val methodComplexities = methods.map { method ->
        val complexity = calculateCyclomaticComplexity(method)
        val lineCount = method.text.lines().size
        MethodComplexity(method.name ?: "anonymous", complexity, lineCount)
    }
    
    val totalComplexity = methodComplexities.sumOf { it.cyclomaticComplexity }
    val averageComplexity = if (methodComplexities.isNotEmpty()) {
        totalComplexity.toDouble() / methodComplexities.size
    } else 0.0
    val maxComplexity = methodComplexities.maxOfOrNull { it.cyclomaticComplexity } ?: 0
    val complexMethods = methodComplexities.filter { it.cyclomaticComplexity > 10 }
    
    return ComplexityAnalysis(
        methods = methodComplexities,
        totalComplexity = totalComplexity,
        averageComplexity = averageComplexity,
        maxComplexity = maxComplexity,
        complexMethods = complexMethods
    )
}

fun calculateCyclomaticComplexity(method: KtNamedFunction): Int {
    var complexity = 1 // Base complexity
    
    method.bodyExpression?.accept(object : KtTreeVisitorVoid() {
        override fun visitIfExpression(expression: KtIfExpression) {
            complexity++
            super.visitIfExpression(expression)
        }
        
        override fun visitWhenExpression(expression: KtWhenExpression) {
            // Add 1 for each when entry (branch)
            complexity += expression.entries.size
            super.visitWhenExpression(expression)
        }
        
        override fun visitForExpression(expression: KtForExpression) {
            complexity++
            super.visitForExpression(expression)
        }
        
        override fun visitWhileExpression(expression: KtWhileExpression) {
            complexity++
            super.visitWhileExpression(expression)
        }
        
        override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
            complexity++
            super.visitDoWhileExpression(expression)
        }
        
        override fun visitTryExpression(expression: KtTryExpression) {
            // Add 1 for each catch clause
            complexity += expression.catchClauses.size
            super.visitTryExpression(expression)
        }
        
        override fun visitBinaryExpression(expression: KtBinaryExpression) {
            // Add complexity for logical operators (&& and ||)
            when (expression.operationToken.toString()) {
                "ANDAND", "OROR" -> complexity++
            }
            super.visitBinaryExpression(expression)
        }
        
        override fun visitCallExpression(expression: KtCallExpression) {
            // Add complexity for elvis operator (?:) and safe calls that might branch
            val calleeText = expression.calleeExpression?.text
            if (calleeText?.contains("?:") == true) {
                complexity++
            }
            super.visitCallExpression(expression)
        }
    })
    
    return complexity
}

fun generateSuggestions(lcom: Int, methodProps: Map<String, Set<String>>, props: List<String>, complexity: ComplexityAnalysis): List<Suggestion> {
    val suggestions = mutableListOf<Suggestion>()
    
    // Specific actionable suggestions only
    val unusedProps = props.filter { prop -> 
        methodProps.values.none { it.contains(prop) } 
    }
    
    if (unusedProps.isNotEmpty()) {
        suggestions.add(Suggestion(
            "🔧",
            "${unusedProps.size} unused ${if (unusedProps.size == 1) "property" else "properties"}",
            "Unused: ${unusedProps.joinToString(", ")}. Remove dead code or integrate these properties into class functionality."
        ))
    }
    
    val methodsWithoutProps = methodProps.filter { it.value.isEmpty() }
    if (methodsWithoutProps.isNotEmpty()) {
        val methodCount = methodsWithoutProps.size
        val methodList = methodsWithoutProps.keys.take(3).joinToString(", ") + 
                        if (methodCount > 3) " +${methodCount - 3} more" else ""
        
        suggestions.add(Suggestion(
            "📤",
            "$methodCount ${if (methodCount == 1) "method doesn't" else "methods don't"} use properties",
            "Methods: $methodList. Consider moving to utility classes or making them static/extension functions."
        ))
    }
    
    // Pattern-based suggestions for poor cohesion
    if (lcom > 5 && methodProps.size > 8) {
        suggestions.add(Suggestion(
            "🔀",
            "Split large unfocused class",
            "Consider breaking into ${if (lcom > 8) "3-4" else "2-3"} smaller classes based on method-property relationships."
        ))
    } else if (lcom > 3) {
        suggestions.add(Suggestion(
            "🎯",
            "Group related functionality",
            "Look for methods that share properties and consider extracting them into focused classes."
        ))
    }
    
    // Method complexity suggestions
    val methodCount = methodProps.size
    if (methodCount > 15) {
        suggestions.add(Suggestion(
            "📏",
            "Class has $methodCount methods",
            "Large classes are harder to maintain. Consider if this class has too many responsibilities."
        ))
    }
    
    // Property usage patterns
    val propertyUsage = props.map { prop ->
        val usageCount = methodProps.values.count { it.contains(prop) }
        prop to usageCount
    }
    
    val lightlyUsedProps = propertyUsage.filter { it.second in 1..2 && methodCount > 3 }.map { it.first }
    
    if (lightlyUsedProps.isNotEmpty()) {
        suggestions.add(Suggestion(
            "⚡",
            "${lightlyUsedProps.size} rarely used ${if (lightlyUsedProps.size == 1) "property" else "properties"}",
            "Properties: ${lightlyUsedProps.joinToString(", ")}. Consider if these belong in a separate class."
        ))
    }
    
    // Complexity-based suggestions
    if (complexity.complexMethods.isNotEmpty()) {
        val complexMethodNames = complexity.complexMethods.take(3).joinToString(", ") { it.methodName } +
                if (complexity.complexMethods.size > 3) " +${complexity.complexMethods.size - 3} more" else ""
        
        suggestions.add(Suggestion(
            "🧠",
            "${complexity.complexMethods.size} complex ${if (complexity.complexMethods.size == 1) "method" else "methods"} (CC > 10)",
            "Methods: $complexMethodNames. Break down complex logic, extract helper methods, or simplify conditional logic."
        ))
    }
    
    if (complexity.averageComplexity > 7) {
        suggestions.add(Suggestion(
            "📊",
            "High average complexity (${String.format("%.1f", complexity.averageComplexity)})",
            "Consider refactoring methods to reduce branching logic and improve readability."
        ))
    }
    
    val veryComplexMethods = complexity.methods.filter { it.cyclomaticComplexity > 20 }
    if (veryComplexMethods.isNotEmpty()) {
        suggestions.add(Suggestion(
            "🚨",
            "${veryComplexMethods.size} very complex ${if (veryComplexMethods.size == 1) "method" else "methods"} (CC > 20)",
            "Methods: ${veryComplexMethods.joinToString(", ") { it.methodName }}. These methods are extremely difficult to test and maintain."
        ))
    }
    
    // No suggestions for well-designed classes
    if (suggestions.isEmpty() && lcom <= 2 && complexity.averageComplexity <= 5) {
        suggestions.add(Suggestion(
            "✨",
            "Well-designed class",
            "Good cohesion and low complexity. Consider this class as a model for others."
        ))
    }
    
    return suggestions
}

fun generateSummary(analyses: List<ClassAnalysis>) {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    
    println("\n" + "=".repeat(60))
    println("               📊 KOTLIN METRICS ANALYSIS SUMMARY")
    println("                    Generated: $timestamp")
    println("=".repeat(60))
    
    if (analyses.isEmpty()) {
        println("\n⚠️  No Kotlin classes found to analyze.")
        println("   Make sure you're running from a Kotlin project directory.")
        println("\n📄 Empty report generated: kotlin-metrics-report.html")
        println("=".repeat(60))
        return
    }
    
    // Project Overview
    val totalMethods = analyses.sumOf { it.complexity.methods.size }
    val totalProperties = analyses.sumOf { it.propertyCount }
    
    println("\n📈 PROJECT OVERVIEW")
    println("   Classes analyzed: ${analyses.size}")
    println("   Total methods: $totalMethods")
    println("   Total properties: $totalProperties")
    
    // Key Metrics
    val avgLcom = analyses.map { it.lcom }.average()
    val avgComplexity = analyses.map { it.complexity.averageComplexity }.average()
    
    println("\n🎯 KEY METRICS")
    println("   Average LCOM: ${String.format("%.2f", avgLcom)} ${getLcomQualityIcon(avgLcom)}")
    println("   Average Complexity: ${String.format("%.2f", avgComplexity)} ${getComplexityQualityIcon(avgComplexity)}")
    
    // Quality Distribution
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
    
    // Issues Summary
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
    
    // Worst Offenders
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
    
    println("\n📄 Interactive report: kotlin-metrics-report.html")
    println("   Open in browser for detailed analysis, charts, and suggestions")
    println("=".repeat(60))
}

fun getLcomQualityIcon(avgLcom: Double): String = when {
    avgLcom <= 2 -> "✅"
    avgLcom <= 5 -> "👍"
    avgLcom <= 8 -> "⚠️"
    else -> "❌"
}

fun getComplexityQualityIcon(avgComplexity: Double): String = when {
    avgComplexity <= 5 -> "✅"
    avgComplexity <= 7 -> "👍"
    avgComplexity <= 10 -> "⚠️"
    else -> "❌"
}

fun generateHtmlReport(analyses: List<ClassAnalysis>) {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val reportFile = File("kotlin-metrics-report.html")
    
    val html = buildString {
        append(generateHtmlHeader())
        append(generateHtmlBody(analyses, timestamp))
        append(generateHtmlFooter())
    }
    
    reportFile.writeText(html)
    println("HTML report saved to: ${reportFile.absolutePath}")
}

fun generateHtmlHeader(): String = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kotlin Metrics Report</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
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
    </style>
</head>
<body>
"""

fun generateHtmlBody(analyses: List<ClassAnalysis>, timestamp: String): String {
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
                                        ${analyses.sortedByDescending { it.lcom }.joinToString("") { analysis ->
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
                                        }}
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
                                        ${analyses.flatMap { analysis ->
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
                                        }.joinToString("")}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
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

// Table sorting and filtering functionality
let sortDirection = {};
let currentFilter = 'all';

// Filter functionality
document.querySelectorAll('.filter-btn').forEach(btn => {
    btn.addEventListener('click', function() {
        // Update active filter button
        document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
        this.classList.add('active');
        
        currentFilter = this.dataset.filter;
        filterTable();
    });
});

function filterTable() {
    const rows = document.querySelectorAll('#classTable tbody tr');
    rows.forEach(row => {
        const quality = row.dataset.quality;
        if (currentFilter === 'all' || quality === currentFilter) {
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
        
        // Update sort indicators
        document.querySelectorAll('.sort-indicator').forEach(indicator => {
            indicator.classList.remove('active');
            indicator.textContent = '↕️';
        });
        
        const indicator = this.querySelector('.sort-indicator');
        indicator.classList.add('active');
        indicator.textContent = newDirection === 'asc' ? '↑' : '↓';
        
        sortTable(column, newDirection);
    });
});

function sortTable(column, direction) {
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
    filterTable();
}

// Initialize tooltips
document.addEventListener('DOMContentLoaded', function() {
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
});
</script>
"""
}

fun generateHtmlFooter(): String = """
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
"""