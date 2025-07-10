# Kotlin Metrics Project

## Project Overview
A comprehensive Kotlin code quality analyzer that provides multiple metrics including LCOM (Lack of Cohesion of Methods) and Cyclomatic Complexity. The tool generates both terminal summaries and interactive HTML reports for detailed analysis.

## Architecture Overview

### Core Components
- **PSI Analysis Engine**: Uses Kotlin compiler's AST for deep code analysis
- **Metrics Calculators**: Separate calculators for LCOM and Cyclomatic Complexity
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

## HTML Report Architecture

### Frontend Stack
- **Bootstrap 5.1.3**: Responsive UI framework
- **Chart.js**: Interactive data visualizations
- **Vanilla JavaScript**: Table sorting, filtering, and tooltips

### Report Features
- **Tabbed Interface**: LCOM and Complexity analysis in separate tabs
- **Interactive Charts**: Bar charts, doughnut charts, scatter plots
- **Sortable Tables**: Click-to-sort with visual indicators
- **Quality Filtering**: Filter by cohesion/complexity levels
- **Smart Tooltips**: Contextual help and detailed explanations

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

## Testing Strategy
- Manual testing on real Kotlin projects
- Edge case validation (empty classes, complex control flow)
- Cross-platform JAR compatibility verification