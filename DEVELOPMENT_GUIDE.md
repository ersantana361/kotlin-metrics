# Development Guide

## Project Architecture

### Overview

The Kotlin & Java Metrics Analyzer is built with a clean, modular architecture that separates concerns and enables easy extension. The project follows domain-driven design principles with clear architectural boundaries.

### Directory Structure

```
kotlin-metrics/
├── src/main/kotlin/
│   ├── Main.kt                              # Application entry point
│   └── com/metrics/
│       ├── model/                           # Data models and domain objects
│       │   ├── analysis/                    # Analysis result models
│       │   │   ├── ClassAnalysis.kt         # Enhanced class analysis with CK metrics
│       │   │   ├── ComplexityAnalysis.kt    # Method complexity details
│       │   │   ├── CkMetrics.kt             # Complete CK metrics suite
│       │   │   ├── QualityScore.kt          # Quality assessment models
│       │   │   ├── RiskAssessment.kt        # Risk analysis models
│       │   │   └── ProjectReport.kt         # Enhanced project reporting
│       │   ├── architecture/                # Architecture analysis models
│       │   │   ├── DddClasses.kt           # DDD pattern models
│       │   │   ├── LayeredArchitecture.kt  # Layer analysis models
│       │   │   └── DependencyGraph.kt      # Graph analysis models
│       │   └── common/                      # Shared enums and constants
│       │       └── Enums.kt                # Common enumerations
│       ├── util/                           # Utility classes and calculators
│       │   ├── LcomCalculator.kt           # LCOM calculation
│       │   ├── WmcCalculator.kt            # WMC calculation
│       │   ├── InheritanceCalculator.kt    # DIT/NOC calculation
│       │   ├── CouplingCalculator.kt       # CBO/RFC/CA/CE calculation
│       │   ├── ComplexityCalculator.kt     # Cyclomatic complexity
│       │   ├── QualityScoreCalculator.kt   # Quality scoring engine
│       │   ├── SuggestionGenerator.kt      # Recommendation engine
│       │   └── BackwardCompatibilityHelper.kt # Legacy model support
│       ├── analyzer/                       # Analysis orchestration
│       │   └── core/                       # Core analysis engines
│       │       ├── CodeAnalyzer.kt         # Analysis interface
│       │       ├── KotlinCodeAnalyzer.kt   # Kotlin-specific analysis
│       │       └── JavaCodeAnalyzer.kt     # Java-specific analysis
│       └── report/                         # Report generation
│           ├── console/                    # Terminal output
│           │   └── ConsoleReportGenerator.kt
│           └── html/                       # HTML report generation
│               ├── HtmlReportGenerator.kt  # Main HTML generator
│               └── SimpleJavaScriptGenerator.kt # Interactive features
├── src/test/kotlin/                        # Test suite
├── build.gradle.kts                        # Build configuration
└── docs/                                   # Documentation
    ├── README.md
    ├── CK_METRICS_GUIDE.md
    ├── ARCHITECTURE_ANALYSIS.md
    ├── QUALITY_SCORING.md
    └── DEVELOPMENT_GUIDE.md
```

## Core Components

### 1. Analysis Engine Architecture

#### Main Entry Point (`Main.kt`)
```kotlin
fun main() {
    // 1. File Discovery
    val kotlinFiles = discoverKotlinFiles()
    val javaFiles = discoverJavaFiles()
    
    // 2. AST Parsing
    val ktFiles = parseKotlinFiles(kotlinFiles)
    val javaClasses = parseJavaFiles(javaFiles)
    
    // 3. Analysis Orchestration
    val analyses = analyzeClasses(ktFiles, javaClasses)
    val architectureAnalysis = analyzeArchitecture(ktFiles, javaFiles, analyses)
    
    // 4. Report Generation
    generateConsoleReport(analyses, architectureAnalysis)
    generateHtmlReport(analyses, architectureAnalysis)
}
```

#### Analysis Flow
```
File Discovery → AST Parsing → Metric Calculation → Quality Scoring → Report Generation
     ↓              ↓               ↓                  ↓               ↓
  .kt/.java     Kotlin PSI     CK Metrics         Quality Score    HTML/Console
    files      JavaParser      Calculation         Risk Assessment   Reports
```

### 2. Model Architecture

#### Enhanced Data Models
The project uses enriched data models that support both legacy and enhanced features:

```kotlin
// Legacy compatibility
data class ClassAnalysis(
    val className: String,
    val fileName: String,
    val lcom: Int,
    val methodCount: Int,
    val propertyCount: Int,
    val methodDetails: Map<String, Set<String>>,
    val suggestions: List<Suggestion>,
    val complexity: ComplexityAnalysis,
    // Enhanced CK metrics (v2.0)
    val ckMetrics: CkMetrics,
    val qualityScore: QualityScore,
    val riskAssessment: RiskAssessment
)

// Complete CK metrics suite
data class CkMetrics(
    val wmc: Int,                    // Weighted Methods per Class
    val cyclomaticComplexity: Int,   // Average CC
    val cbo: Int,                    // Coupling Between Objects
    val rfc: Int,                    // Response for a Class
    val ca: Int,                     // Afferent Coupling
    val ce: Int,                     // Efferent Coupling
    val dit: Int,                    // Depth of Inheritance Tree
    val noc: Int,                    // Number of Children
    val lcom: Int                    // Lack of Cohesion of Methods
)
```

### 3. Calculator Architecture

#### Modular Metric Calculators
Each CK metric has a dedicated calculator for maintainability:

```kotlin
// Example: WMC Calculator
object WmcCalculator {
    fun calculateWmc(classOrObject: KtClassOrObject): Int {
        val methods = classOrObject.declarations.filterIsInstance<KtNamedFunction>()
        return methods.sumOf { method ->
            ComplexityCalculator.calculateCyclomaticComplexity(method)
        }
    }
    
    fun calculateWmc(classDecl: ClassOrInterfaceDeclaration): Int {
        val methods = classDecl.methods
        return methods.sumOf { method ->
            ComplexityCalculator.calculateCyclomaticComplexity(method)
        }
    }
}

// Cross-language complexity calculation
object ComplexityCalculator {
    fun calculateCyclomaticComplexity(method: KtNamedFunction): Int {
        var complexity = 1  // Base complexity
        
        method.bodyExpression?.accept(object : KtTreeVisitorVoid() {
            override fun visitIfExpression(expression: KtIfExpression) {
                complexity++  // Each if adds 1
                super.visitIfExpression(expression)
            }
            
            override fun visitWhenExpression(expression: KtWhenExpression) {
                complexity += expression.entries.size  // Each branch adds 1
                super.visitWhenExpression(expression)
            }
            // ... other control flow constructs
        })
        
        return complexity
    }
    
    fun calculateCyclomaticComplexity(method: MethodDeclaration): Int {
        // Similar implementation for Java using JavaParser
    }
}
```

### 4. Quality Scoring Engine

#### Multi-Dimensional Scoring
```kotlin
object QualityScoreCalculator {
    fun calculateQualityScore(
        classOrObject: KtClassOrObject,
        ckMetrics: CkMetrics,
        allClasses: List<KtClassOrObject>
    ): QualityScore {
        val cohesionScore = calculateCohesionScore(ckMetrics.lcom)
        val complexityScore = calculateComplexityScore(ckMetrics.wmc, ckMetrics.cyclomaticComplexity)
        val couplingScore = calculateCouplingScore(ckMetrics.cbo, ckMetrics.rfc, ckMetrics.ca, ckMetrics.ce)
        val inheritanceScore = calculateInheritanceScore(ckMetrics.dit, ckMetrics.noc)
        val overallScore = calculateOverallDesignScore(cohesionScore, complexityScore, couplingScore, inheritanceScore, ckMetrics)
        
        val finalScore = cohesionScore * 0.20 +
                        complexityScore * 0.20 +
                        couplingScore * 0.25 +
                        inheritanceScore * 0.15 +
                        overallScore * 0.20
        
        return QualityScore(
            overall = finalScore,
            cohesion = cohesionScore,
            complexity = complexityScore,
            coupling = couplingScore,
            inheritance = inheritanceScore,
            design = overallScore
        )
    }
}
```

### 5. Report Generation Architecture

#### Multi-Format Output
```kotlin
// Console Report Generator
class ConsoleReportGenerator {
    fun generate(analyses: List<ClassAnalysis>, architectureAnalysis: ArchitectureAnalysis) {
        generateHeader()
        generateProjectOverview(analyses)
        generateCkMetricsSummary(analyses)
        generateQualityDistribution(analyses)
        generateRiskAssessment(analyses)
        generateArchitectureSummary(architectureAnalysis)
        generateRefactoringTargets(analyses)
    }
}

// HTML Report Generator  
class HtmlReportGenerator {
    fun generate(analyses: List<ClassAnalysis>, architectureAnalysis: ArchitectureAnalysis) {
        val jsGenerator = SimpleJavaScriptGenerator()
        
        return buildString {
            append(generateHeader())
            append(generateOverviewTab(analyses, architectureAnalysis))
            append(generateLcomTab(analyses))
            append(generateComplexityTab(analyses))
            append(generateCkMetricsTab(analyses))  // New tab
            append(generateArchitectureTab(architectureAnalysis))
            append(generateQualityAnalyticsTab(analyses))  // New tab
            append(jsGenerator.generateInteractiveFeatures(analyses, architectureAnalysis))
            append(generateFooter())
        }
    }
}
```

## Development Workflow

### 1. Setting Up Development Environment

#### Prerequisites
```bash
# Java 17 or higher
java -version

# Kotlin compiler (comes with Gradle)
./gradlew --version
```

#### Build and Test
```bash
# Clean build
./gradlew clean build

# Run tests
./gradlew test

# Build fat JAR
./gradlew fatJar

# Test the tool
java -jar build/libs/kotlin-metrics-all-1.0.0.jar
```

### 2. Adding New Metrics

#### Step 1: Define the Metric Model
```kotlin
// Add to CkMetrics.kt
data class CkMetrics(
    // ... existing metrics
    val newMetric: Int  // Add new metric field
)
```

#### Step 2: Create Calculator
```kotlin
// Create NewMetricCalculator.kt
object NewMetricCalculator {
    fun calculateNewMetric(classOrObject: KtClassOrObject): Int {
        // Implementation for Kotlin
    }
    
    fun calculateNewMetric(classDecl: ClassOrInterfaceDeclaration): Int {
        // Implementation for Java
    }
}
```

#### Step 3: Integrate into Analysis
```kotlin
// Update BackwardCompatibilityHelper.kt
fun createEnhancedKotlinClassAnalysis(...): ClassAnalysis {
    val newMetricValue = NewMetricCalculator.calculateNewMetric(classOrObject)
    
    val ckMetrics = CkMetrics(
        // ... existing metrics
        newMetric = newMetricValue
    )
    
    // Rest of analysis...
}
```

#### Step 4: Update Quality Scoring
```kotlin
// Update QualityScoreCalculator.kt
fun calculateQualityScore(ckMetrics: CkMetrics): QualityScore {
    val newMetricScore = calculateNewMetricScore(ckMetrics.newMetric)
    
    // Integrate into overall score calculation
}
```

#### Step 5: Update Reports
```kotlin
// Update report generators to display new metric
```

### 3. Adding New Architecture Patterns

#### Step 1: Define Pattern Model
```kotlin
// Add to architecture models
data class NewArchitecturePattern(
    val className: String,
    val fileName: String,
    val confidence: Double,
    val specificFields: List<String>
)
```

#### Step 2: Create Pattern Detector
```kotlin
object NewPatternDetector {
    fun detectPattern(classOrObject: KtClassOrObject): NewArchitecturePattern? {
        var confidence = 0.0
        
        // Detection logic
        if (hasPatternIndicator1(classOrObject)) confidence += 0.3
        if (hasPatternIndicator2(classOrObject)) confidence += 0.4
        if (hasPatternIndicator3(classOrObject)) confidence += 0.3
        
        return if (confidence > 0.5) {
            NewArchitecturePattern(
                className = classOrObject.name ?: "Unknown",
                fileName = classOrObject.containingKtFile.name,
                confidence = confidence,
                specificFields = extractSpecificFields(classOrObject)
            )
        } else null
    }
}
```

#### Step 3: Integrate into Architecture Analysis
```kotlin
// Update DDD pattern analysis functions
fun analyzeDddPatterns(ktFiles: List<KtFile>): DddPatternAnalysis {
    val newPatterns = mutableListOf<NewArchitecturePattern>()
    
    for (ktFile in ktFiles) {
        for (classOrObject in ktFile.declarations.filterIsInstance<KtClassOrObject>()) {
            NewPatternDetector.detectPattern(classOrObject)?.let { pattern ->
                newPatterns.add(pattern)
            }
        }
    }
    
    // ... rest of analysis
}
```

### 4. Extending Report Generation

#### Adding New Tab to HTML Report
```kotlin
// In HtmlReportGenerator.kt
private fun generateNewAnalysisTab(analyses: List<ClassAnalysis>): String {
    return """
        <div class="tab-pane fade" id="new-analysis" role="tabpanel">
            <div class="mt-4">
                <h4>🆕 New Analysis</h4>
                <div class="card">
                    <div class="card-header">
                        <h5>New Metric Analysis</h5>
                    </div>
                    <div class="card-body">
                        <!-- Chart container -->
                        <canvas id="newMetricChart" class="chart-container"></canvas>
                        
                        <!-- Data table -->
                        <table class="table table-striped">
                            <thead>
                                <tr>
                                    <th>Class</th>
                                    <th>New Metric</th>
                                    <th>Quality</th>
                                </tr>
                            </thead>
                            <tbody>
                                ${analyses.joinToString("") { analysis ->
                                    """
                                    <tr>
                                        <td>${analysis.className}</td>
                                        <td>${analysis.ckMetrics.newMetric}</td>
                                        <td>${getQualityBadge(analysis.ckMetrics.newMetric)}</td>
                                    </tr>
                                    """
                                }}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    """
}
```

#### Adding Interactive Features
```kotlin
// In SimpleJavaScriptGenerator.kt
fun generateNewMetricChart(analyses: List<ClassAnalysis>): String {
    return """
        // New metric chart
        new Chart(document.getElementById('newMetricChart'), {
            type: 'bar',
            data: {
                labels: [${analyses.map { "\"${it.className}\"" }.joinToString(",")}],
                datasets: [{
                    label: 'New Metric',
                    data: [${analyses.map { it.ckMetrics.newMetric }.joinToString(",")}],
                    backgroundColor: 'rgba(54, 162, 235, 0.6)'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: { beginAtZero: true }
                }
            }
        });
    """
}
```

## Testing Strategy

### 1. Unit Tests
```kotlin
class WmcCalculatorTest {
    @Test
    fun `should calculate WMC correctly for simple class`() {
        val kotlinCode = """
            class TestClass {
                fun simpleMethod() = 42
                fun methodWithIf(x: Int) = if (x > 0) x else 0
            }
        """.trimIndent()
        
        val ktFile = parseKotlinCode(kotlinCode)
        val classOrObject = ktFile.declarations.filterIsInstance<KtClassOrObject>().first()
        
        val wmc = WmcCalculator.calculateWmc(classOrObject)
        
        assertEquals(3, wmc) // 1 + 2 (method with if)
    }
}
```

### 2. Integration Tests
```kotlin
class FullAnalysisIntegrationTest {
    @Test
    fun `should analyze mixed Kotlin and Java project correctly`() {
        val kotlinFiles = listOf(createKotlinTestFile())
        val javaFiles = listOf(createJavaTestFile())
        
        val analyses = analyzeProject(kotlinFiles, javaFiles)
        
        assertEquals(2, analyses.size)
        assertTrue(analyses.all { it.ckMetrics.wmc > 0 })
        assertTrue(analyses.all { it.qualityScore.overall in 0.0..10.0 })
    }
}
```

### 3. Performance Tests
```kotlin
@Test
fun `should analyze large codebase efficiently`() {
    val startTime = System.currentTimeMillis()
    
    val analyses = analyzeLargeCodebase() // 100+ classes
    
    val duration = System.currentTimeMillis() - startTime
    assertTrue(duration < 30_000) // Should complete in under 30 seconds
    
    assertTrue(analyses.size >= 100)
}
```

## Performance Considerations

### 1. Memory Management
- **Streaming Analysis**: Process files one at a time to avoid memory issues
- **Lazy Evaluation**: Calculate metrics only when needed
- **Early Cleanup**: Dispose of AST objects after analysis

### 2. Processing Optimization
- **Parallel Processing**: Analyze independent files in parallel
- **Caching**: Cache expensive calculations (inheritance trees, dependency graphs)
- **Incremental Analysis**: Only re-analyze changed files

```kotlin
// Example: Parallel file processing
fun analyzeFilesInParallel(files: List<File>): List<ClassAnalysis> {
    return files.parallelStream()
        .map { file -> analyzeFile(file) }
        .collect(Collectors.toList())
}
```

### 3. Scalability Patterns
- **Bounded Analysis**: Set limits on analysis depth for very large hierarchies
- **Progressive Enhancement**: Basic metrics first, detailed analysis on demand
- **Resource Monitoring**: Track memory and CPU usage during analysis

## Contributing Guidelines

### 1. Code Style
- Follow Kotlin coding conventions
- Use meaningful names for classes and methods
- Add KDoc comments for public APIs
- Keep functions focused and small

### 2. Testing Requirements
- Unit tests for all calculator classes
- Integration tests for analysis workflows
- Performance tests for large codebases
- Regression tests for bug fixes

### 3. Documentation
- Update relevant .md files for new features
- Include code examples in documentation
- Maintain changelog for releases

### 4. Pull Request Process
1. Create feature branch from main
2. Implement feature with tests
3. Update documentation
4. Ensure all tests pass
5. Create pull request with description
6. Address review feedback

## Troubleshooting

### Common Issues

#### 1. OutOfMemoryError
```bash
# Increase JVM heap size
java -Xmx4g -jar kotlin-metrics-all-1.0.0.jar
```

#### 2. Compilation Errors in Analysis
- Check Kotlin/Java source file encoding
- Verify source code compiles with standard compilers
- Update to latest Kotlin compiler version

#### 3. Missing Dependencies
```bash
# Rebuild fat JAR to include all dependencies
./gradlew clean fatJar
```

#### 4. Performance Issues
- Profile large codebases to identify bottlenecks
- Consider filtering out test files or generated code
- Use parallel analysis options

This development guide provides the foundation for understanding, extending, and contributing to the Kotlin & Java Metrics Analyzer.