# Kotlin Metrics Project

## Project Overview
A comprehensive Kotlin code quality analyzer that provides multiple metrics including LCOM (Lack of Cohesion of Methods), Cyclomatic Complexity, and **Architecture Analysis**. The tool generates both terminal summaries and interactive HTML reports with detailed analysis, DDD pattern detection, and architectural visualization.

## Architecture Overview

### Core Components
- **PSI Analysis Engine**: Uses Kotlin compiler's AST for deep code analysis
- **Metrics Calculators**: Separate calculators for LCOM and Cyclomatic Complexity
- **Architecture Analyzer**: DDD pattern detection and layered architecture analysis
- **Dependency Graph Builder**: Maps class relationships and detects cycles
- **Report Generation**: Dual output system (terminal + HTML)
- **Interactive Frontend**: Bootstrap-based dashboard with Chart.js visualizations

### Key Metrics Implemented
1. **LCOM (Lack of Cohesion of Methods)**
   - Measures method-property relationships within classes
   - Formula: `LCOM = P - Q` (clamped to minimum 0)
   - Identifies classes violating Single Responsibility Principle

2. **Cyclomatic Complexity**
   - AST-based analysis of control flow complexity
   - Detects: if/else, when, loops, try/catch, logical operators
   - Method-level granularity with aggregation to class level

3. **Architecture Analysis**
   - **DDD Pattern Detection**: Identifies Entities, Value Objects, Services, Repositories, Aggregates, and Domain Events
   - **Layered Architecture**: Analyzes presentation, application, domain, data, and infrastructure layers
   - **Dependency Graph**: Maps class relationships and detects circular dependencies
   - **Architecture Patterns**: Detects Layered, Hexagonal, Clean, and Onion architectures
   - **Violation Detection**: Identifies layer violations and architectural anti-patterns

## Build System
- **Build Tool**: Gradle 8.5 with Kotlin DSL
- **Main Task**: `./gradlew fatJar` - Creates standalone executable JAR
- **Output**: `build/libs/kotlin-metrics-all-1.0.0.jar` (~66MB with all dependencies)
- **Duplicate Handling**: `DuplicatesStrategy.EXCLUDE` for META-INF conflicts

## Key Dependencies
- `kotlin-compiler-embeddable:1.9.22` - PSI parsing and AST traversal
- `kotlin-stdlib:1.9.22` - Standard library support
- `kotlin-reflect:1.9.22` - Reflection capabilities

## Data Structures

### Core Analysis Types
```kotlin
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
```

### Architecture Analysis Types
```kotlin
data class ArchitectureAnalysis(
    val dddPatterns: DddPatternAnalysis,
    val layeredArchitecture: LayeredArchitectureAnalysis,
    val dependencyGraph: DependencyGraph
)

data class DddPatternAnalysis(
    val entities: List<DddEntity>,
    val valueObjects: List<DddValueObject>,
    val services: List<DddService>,
    val repositories: List<DddRepository>,
    val aggregates: List<DddAggregate>,
    val domainEvents: List<DddDomainEvent>
)

data class LayeredArchitectureAnalysis(
    val layers: List<ArchitectureLayer>,
    val dependencies: List<LayerDependency>,
    val violations: List<ArchitectureViolation>,
    val pattern: ArchitecturePattern // LAYERED, HEXAGONAL, CLEAN, ONION
)

data class DependencyGraph(
    val nodes: List<DependencyNode>,
    val edges: List<DependencyEdge>,
    val cycles: List<DependencyCycle>,
    val packages: List<PackageAnalysis>
)
```

## HTML Report Architecture

### Frontend Stack
- **Bootstrap 5.1.3**: Responsive UI framework
- **Chart.js**: Interactive data visualizations
- **Vanilla JavaScript**: Table sorting, filtering, and tooltips

### Report Features
- **Tabbed Interface**: LCOM, Complexity, and Architecture analysis in separate tabs
- **Interactive Charts**: Bar charts, doughnut charts, scatter plots
- **Architecture Visualization**: Layer diagrams, DDD pattern distribution, dependency graphs
- **Sortable Tables**: Click-to-sort with visual indicators
- **Quality Filtering**: Filter by cohesion/complexity levels
- **Smart Tooltips**: Contextual help and detailed explanations
- **Violation Reports**: Detailed architecture violation analysis with suggestions

## Analysis Algorithms

### LCOM Calculation
```kotlin
// For each method pair, check property overlap
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
```

### Cyclomatic Complexity Visitor
```kotlin
method.bodyExpression?.accept(object : KtTreeVisitorVoid() {
    override fun visitIfExpression(expression: KtIfExpression) {
        complexity++ // Each if adds 1
        super.visitIfExpression(expression)
    }
    
    override fun visitWhenExpression(expression: KtWhenExpression) {
        complexity += expression.entries.size // Each branch adds 1
        super.visitWhenExpression(expression)
    }
    // ... other control flow constructs
})
```

### DDD Pattern Detection Algorithms

**Entity Detection:**
```kotlin
fun analyzeEntity(classOrObject: KtClassOrObject): DddEntity {
    var confidence = 0.0
    
    // Check for ID fields
    if (hasIdFields(classOrObject)) confidence += 0.3
    
    // Check for mutability
    if (hasMutableProperties(classOrObject)) confidence += 0.2
    
    // Check for equals/hashCode
    if (hasEqualsHashCode(classOrObject)) confidence += 0.3
    
    // Check naming patterns
    if (className.endsWith("Entity")) confidence += 0.2
    
    return DddEntity(confidence = confidence)
}
```

**Layer Detection:**
```kotlin
fun inferLayer(packageName: String, className: String): String? {
    return when {
        packageName.contains("controller") || packageName.contains("api") -> "presentation"
        packageName.contains("service") || packageName.contains("application") -> "application"
        packageName.contains("domain") || packageName.contains("model") -> "domain"
        packageName.contains("repository") || packageName.contains("data") -> "data"
        className.endsWith("Controller") -> "presentation"
        className.endsWith("Service") -> "application"
        className.endsWith("Repository") -> "data"
        else -> null
    }
}
```

### Dependency Graph Construction
```kotlin
fun buildDependencyGraph(ktFiles: List<KtFile>): DependencyGraph {
    val nodes = mutableListOf<DependencyNode>()
    val edges = mutableListOf<DependencyEdge>()
    
    // Build nodes from class declarations
    for (ktFile in ktFiles) {
        for (classOrObject in ktFile.declarations.filterIsInstance<KtClassOrObject>()) {
            nodes.add(DependencyNode(
                id = "${ktFile.packageFqName}.${classOrObject.name}",
                className = classOrObject.name,
                nodeType = determineNodeType(classOrObject),
                layer = inferLayer(ktFile.packageFqName.asString(), classOrObject.name)
            ))
        }
    }
    
    // Build edges from references
    analyzeReferences(ktFiles, nodes, edges)
    
    // Detect cycles
    val cycles = detectCycles(nodes, edges)
    
    return DependencyGraph(nodes, edges, cycles, packageAnalyses)
}
```

## Usage
```bash
# Build
./gradlew fatJar

# Run from project root (analyzes all .kt files)
kotlin-metrics

# Global alias setup
alias kotlin-metrics="java -jar /path/to/kotlin-metrics-all-1.0.0.jar"
```

## Development Notes

### Key Design Decisions
- **Single Entry Point**: Tool always runs from project root, analyzes all .kt files
- **Dual Output**: Terminal for quick overview, HTML for detailed analysis
- **Non-Redundant Suggestions**: Quality badges show level, suggestions show actions
- **Extensible Architecture**: Easy to add new metrics as tabs

### Technical Considerations
- **Memory Efficiency**: Streams and lazy evaluation for large codebases
- **Error Handling**: Graceful failure with empty reports for invalid projects
- **Cross-Platform**: Pure JVM solution works on Windows/Mac/Linux
- **Performance**: ~66MB JAR loads quickly, analysis is CPU-bound on AST traversal

### Future Extension Points
- **New Metrics**: Add tabs for Lines of Code, Depth of Inheritance, etc.
- **Output Formats**: JSON/XML export for CI/CD integration
- **Threshold Configuration**: Configurable quality thresholds
- **Historical Tracking**: Compare metrics over time
- **Enhanced Visualization**: D3.js for interactive dependency graphs
- **Custom Architecture Rules**: User-defined layer validation rules
- **Microservices Analysis**: Multi-service architecture patterns

## Testing Strategy
- Manual testing on real Kotlin projects
- Edge case validation (empty classes, complex control flow)
- Cross-platform JAR compatibility verification