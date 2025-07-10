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

data class ClassAnalysis(
    val className: String,
    val fileName: String,
    val lcom: Int,
    val methodCount: Int,
    val propertyCount: Int,
    val methodDetails: Map<String, Set<String>>,
    val suggestions: List<Suggestion>
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

    val suggestions = generateSuggestions(lcom, methodProps, props)

    return ClassAnalysis(
        className = className,
        fileName = fileName,
        lcom = lcom,
        methodCount = methods.size,
        propertyCount = props.size,
        methodDetails = methodProps,
        suggestions = suggestions
    )
}

data class Suggestion(
    val icon: String,
    val message: String,
    val tooltip: String
)

fun generateSuggestions(lcom: Int, methodProps: Map<String, Set<String>>, props: List<String>): List<Suggestion> {
    val suggestions = mutableListOf<Suggestion>()
    
    // Primary LCOM-based suggestion
    when {
        lcom == 0 -> suggestions.add(Suggestion(
            "✅", 
            "Excellent cohesion",
            "All methods share properties effectively. This indicates a well-designed class with strong internal cohesion."
        ))
        lcom in 1..2 -> suggestions.add(Suggestion(
            "👍", 
            "Good cohesion",
            "Minor improvements possible. Look for opportunities to increase property sharing between methods."
        ))
        lcom in 3..5 -> suggestions.add(Suggestion(
            "⚠️", 
            "Moderate cohesion - consider refactoring",
            "Some methods don't share properties. Group related functionality or split into focused classes."
        ))
        lcom > 5 -> suggestions.add(Suggestion(
            "❌", 
            "Poor cohesion - refactoring needed",
            "High LCOM indicates multiple responsibilities. Apply Single Responsibility Principle by extracting related methods into separate classes."
        ))
    }
    
    // Specific actionable suggestions
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
    
    // Additional pattern-based suggestions
    if (lcom > 3 && methodProps.size > 8) {
        suggestions.add(Suggestion(
            "🔀",
            "Large class with poor cohesion",
            "Consider splitting into ${if (lcom > 6) "3-4" else "2-3"} smaller, focused classes based on method-property relationships."
        ))
    }
    
    return suggestions
}

fun generateSummary(analyses: List<ClassAnalysis>) {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    
    println("\n📊 KOTLIN METRICS ANALYSIS SUMMARY")
    println("Generated: $timestamp")
    println("-".repeat(50))
    
    println("Total classes analyzed: ${analyses.size}")
    if (analyses.isNotEmpty()) {
        println("Average LCOM: ${analyses.map { it.lcom }.average().let { "%.2f".format(it) }}")
        
        val cohesionDistribution = analyses.groupBy { 
            when (it.lcom) {
                0 -> "Excellent"
                in 1..2 -> "Good"
                in 3..5 -> "Moderate"
                else -> "Poor"
            }
        }
        
        println("\nCohesion Quality:")
        cohesionDistribution.forEach { (level, classes) ->
            val icon = when (level) {
                "Excellent" -> "✅"
                "Good" -> "👍"
                "Moderate" -> "⚠️"
                else -> "❌"
            }
            println("  $icon $level: ${classes.size} classes")
        }
        
        val worstClasses = analyses.filter { it.lcom > 5 }
        if (worstClasses.isNotEmpty()) {
            println("\n❌ Classes needing attention:")
            worstClasses.take(3).forEach { 
                println("  • ${it.className} (LCOM: ${it.lcom})")
            }
        }
    } else {
        println("No Kotlin classes found to analyze.")
    }
    
    println("\n📄 Detailed report generated: kotlin-metrics-report.html")
    println("-".repeat(50))
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
        <div class="col-md-3">
            <div class="card metric-card">
                <div class="card-body text-center">
                    <h5 class="card-title">Average LCOM</h5>
                    <h2 class="text-info">${if (analyses.isNotEmpty()) "%.2f".format(analyses.map { it.lcom }.average()) else "0"}</h2>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card metric-card">
                <div class="card-body text-center">
                    <h5 class="card-title">Excellent Classes</h5>
                    <h2 class="text-success">${cohesionDistribution["Excellent"]?.size ?: 0}</h2>
                </div>
            </div>
        </div>
        <div class="col-md-3">
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
        <!-- Future tabs will be added here -->
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
                                                            "✅" -> "text-success"
                                                            "👍" -> "text-info"
                                                            "⚠️" -> "text-warning"
                                                            "❌" -> "text-danger"
                                                            "🔧" -> "text-secondary"
                                                            "📤" -> "text-primary"
                                                            "🔀" -> "text-warning"
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