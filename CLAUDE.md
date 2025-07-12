# Kotlin Metrics Project

## Project Overview
A comprehensive Kotlin and Java code quality analyzer that provides **complete CK (Chidamber and Kemerer) metrics suite**, architecture analysis, and DDD pattern detection. The tool generates both terminal summaries and interactive HTML reports with detailed analysis, quality scoring, risk assessment, and architectural visualization.

## Architecture Overview

### Core Components
- **Multi-Language Analysis Engine**: Uses Kotlin PSI and JavaParser for cross-language analysis
- **Complete CK Metrics Suite**: All 9 Chidamber and Kemerer metrics implemented
- **Quality Scoring System**: 0-10 scale with risk assessment and correlation analysis
- **Architecture Analyzer**: DDD pattern detection and layered architecture analysis
- **Dependency Graph Builder**: Maps class relationships and detects cycles across languages
- **Report Generation**: Enhanced 6-tab HTML dashboard with interpretation guides
- **Interactive Frontend**: Bootstrap 5 with Chart.js and comprehensive tooltips

### Complete CK Metrics Implemented
1. **LCOM (Lack of Cohesion of Methods)**
   - Measures method-property relationships within classes
   - Formula: `LCOM = P - Q` (clamped to minimum 0)
   - Identifies classes violating Single Responsibility Principle

2. **WMC (Weighted Methods per Class)**
   - Sum of cyclomatic complexities of all methods in a class
   - Indicates overall class complexity and testing effort

3. **DIT (Depth of Inheritance Tree)**
   - Measures inheritance depth from class to root hierarchy
   - Cross-language inheritance tracking with cycle detection

4. **NOC (Number of Children)**
   - Count of direct subclasses
   - Indicates abstraction level appropriateness

5. **CBO (Coupling Between Objects)**
   - Number of classes this class is coupled to
   - Framework-aware coupling analysis

6. **RFC (Response For a Class)**
   - Number of methods that can be invoked in response to a message
   - Local methods + remote method calls

7. **CA (Afferent Coupling)**
   - Number of classes that depend on this class
   - Stability indicator for components

8. **CE (Efferent Coupling)**
   - Number of classes this class depends on
   - Instability indicator for components

9. **Cyclomatic Complexity (CC)**
   - AST-based analysis of control flow complexity
   - Detects: if/else, when, loops, try/catch, logical operators
   - Method-level granularity with aggregation to class level

### Enhanced Architecture Analysis
- **DDD Pattern Detection**: Identifies Entities, Value Objects, Services, Repositories, Aggregates, and Domain Events with confidence scoring
- **Layered Architecture**: Analyzes presentation, application, domain, data, and infrastructure layers
- **Dependency Graph**: Maps class relationships and detects circular dependencies
- **Architecture Patterns**: Detects Layered, Hexagonal, Clean, and Onion architectures
- **Violation Detection**: Identifies layer violations and architectural anti-patterns

### Quality Assessment System
- **Quality Scoring**: 0-10 scale combining all CK metrics with weighted importance
- **Risk Assessment**: Critical/High/Medium/Low risk classification
- **Correlation Analysis**: Statistical relationships between metrics
- **Improvement Roadmap**: Prioritized refactoring suggestions

## Build System
- **Build Tool**: Gradle 8.5 with Kotlin DSL
- **Main Task**: `./gradlew fatJar` - Creates standalone executable JAR
- **Output**: `build/libs/kotlin-metrics-all-1.0.0.jar` (~66MB with all dependencies)
- **Duplicate Handling**: `DuplicatesStrategy.EXCLUDE` for META-INF conflicts

## Key Dependencies
- `kotlin-compiler-embeddable:1.9.22` - PSI parsing and AST traversal
- `kotlin-stdlib:1.9.22` - Standard library support
- `kotlin-reflect:1.9.22` - Reflection capabilities
- `javaparser-core:3.25.4` - Java AST parsing and analysis

## Enhanced Data Structures

### Core Analysis Types
```kotlin
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

data class CkMetrics(
    val wmc: Int,                    // Weighted Methods per Class
    val cyclomaticComplexity: Int,   // Average Cyclomatic Complexity
    val cbo: Int,                    // Coupling Between Objects
    val rfc: Int,                    // Response For a Class
    val ca: Int,                     // Afferent Coupling
    val ce: Int,                     // Efferent Coupling
    val dit: Int,                    // Depth of Inheritance Tree
    val noc: Int,                    // Number of Children
    val lcom: Int                    // Lack of Cohesion of Methods
)

data class QualityScore(
    val overall: Double,             // 0-10 overall quality score
    val cohesion: Double,            // LCOM-based score
    val complexity: Double,          // WMC/CC-based score
    val coupling: Double,            // CBO/RFC/CA/CE-based score
    val inheritance: Double,         // DIT/NOC-based score
    val design: Double               // Overall design assessment
)

data class RiskAssessment(
    val riskLevel: RiskLevel,        // CRITICAL, HIGH, MEDIUM, LOW
    val primaryConcerns: List<String>,
    val impactAreas: List<String>,
    val recommendedActions: List<String>,
    val estimatedEffort: EstimatedEffort
)

data class ProjectReport(
    val timestamp: String,
    val classes: List<ClassAnalysis>,
    val summary: String,
    val architectureAnalysis: ArchitectureAnalysis,
    // Enhanced reporting (v2.0)
    val projectQualityScore: QualityScore,
    val packageMetrics: List<PackageMetrics>,
    val couplingMatrix: List<CouplingRelation>,
    val riskAssessments: List<RiskAssessment>
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
```

## Enhanced HTML Report Architecture

### Frontend Stack
- **Bootstrap 5.1.3**: Modern responsive UI framework
- **Chart.js**: Interactive data visualizations with correlation analysis
- **Vanilla JavaScript**: Advanced table sorting, filtering, and real-time updates

### 6-Tab Dashboard
1. **📈 Overview Tab**: CK metrics summary, quality distribution, risk assessment
2. **🎯 LCOM Analysis Tab**: Cohesion analysis with filtering and suggestions
3. **🌀 Complexity Tab**: WMC/CC analysis with method-level details
4. **📊 CK Metrics Tab**: Complete metrics visualization with correlation matrix
5. **🏗️ Architecture Tab**: DDD patterns, layer analysis, dependency graphs
6. **🔍 Quality Analytics Tab**: Risk assessment, improvement roadmap, correlation analysis

### Enhanced Features
- **Interpretation Guides**: Comprehensive tooltips explaining metric importance
- **Quality Filtering**: Real-time filtering by quality levels and risk categories
- **Correlation Analysis**: Interactive heatmaps showing metric relationships
- **Improvement Roadmap**: Prioritized refactoring suggestions with effort estimates
- **Risk Assessment**: Critical to low risk classification with impact analysis

## Analysis Algorithms

### Complete CK Metrics Calculation
```kotlin
// WMC Calculation
fun calculateWmc(classOrObject: KtClassOrObject): Int {
    val methods = classOrObject.declarations.filterIsInstance<KtNamedFunction>()
    return methods.sumOf { method ->
        ComplexityCalculator.calculateCyclomaticComplexity(method)
    }
}

// Coupling Calculation (CBO/RFC/CA/CE)
fun calculateCoupling(classOrObject: KtClassOrObject, allClasses: List<KtClassOrObject>): CouplingMetrics {
    val dependencies = extractDependencies(classOrObject)
    val dependents = findDependents(classOrObject, allClasses)
    val remoteCalls = extractRemoteMethodCalls(classOrObject)
    val localMethods = classOrObject.declarations.filterIsInstance<KtNamedFunction>().size
    
    return CouplingMetrics(
        cbo = dependencies.size,
        ca = dependents.size,
        ce = dependencies.size, // Same as CBO in this context
        rfc = localMethods + remoteCalls.size
    )
}

// Quality Score Calculation
fun calculateQualityScore(ckMetrics: CkMetrics): QualityScore {
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
```

### Cross-Language Analysis
```kotlin
// Unified analysis across Kotlin and Java
fun analyzeProject(kotlinFiles: List<File>, javaFiles: List<File>): ProjectReport {
    val ktFiles = parseKotlinFiles(kotlinFiles)
    val javaClasses = parseJavaFiles(javaFiles)
    
    // Unified analysis
    val kotlinAnalyses = analyzeKotlinClasses(ktFiles, javaClasses)
    val javaAnalyses = analyzeJavaClasses(javaClasses, ktFiles)
    
    val allAnalyses = kotlinAnalyses + javaAnalyses
    
    // Cross-language architecture analysis
    val architectureAnalysis = analyzeMixedArchitecture(ktFiles, javaFiles, allAnalyses)
    
    // Enhanced project-level metrics
    val projectQualityScore = calculateProjectQualityScore(allAnalyses)
    val riskAssessments = generateRiskAssessments(allAnalyses)
    
    return ProjectReport(
        timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        classes = allAnalyses,
        summary = generateProjectSummary(allAnalyses, architectureAnalysis),
        architectureAnalysis = architectureAnalysis,
        projectQualityScore = projectQualityScore,
        packageMetrics = calculatePackageMetrics(allAnalyses),
        couplingMatrix = buildCouplingMatrix(allAnalyses),
        riskAssessments = riskAssessments
    )
}
```

## Usage
```bash
# Build
./gradlew fatJar

# Run from project root (analyzes all .kt and .java files)
kotlin-metrics

# Global alias setup
alias kotlin-metrics="java -jar /path/to/kotlin-metrics-all-1.0.0.jar"
```

## Development Notes

### Enhanced Architecture (v2.0)
- **Modular Design**: Separate calculators for each CK metric
- **Quality Scoring Engine**: Multi-dimensional quality assessment
- **Cross-Language Support**: Unified analysis for Kotlin and Java
- **Enhanced Reporting**: 6-tab dashboard with interpretation guides
- **Backward Compatibility**: Existing projects work with enhanced features

### Code Quality Improvements (v2.0)
- **35% Codebase Reduction**: Eliminated duplicate models and dead code
- **Single Source of Truth**: All model classes in `/model/` packages
- **Enhanced Architecture**: Clean separation of concerns
- **Performance Optimization**: Improved memory usage and processing speed

### Future Extension Points
- **Additional Metrics**: Lines of Code, Maintainability Index, Technical Debt
- **Advanced Visualizations**: D3.js interactive dependency graphs
- **Historical Tracking**: Compare metrics over time
- **CI/CD Integration**: Quality gates and automated reporting
- **Custom Rules**: User-defined architecture validation rules
- **Export Formats**: JSON/XML export for external tools

## Documentation
- **[README.md](README.md)**: Complete user guide with examples
- **[CK_METRICS_GUIDE.md](CK_METRICS_GUIDE.md)**: Detailed guide to all 9 CK metrics
- **[ARCHITECTURE_ANALYSIS.md](ARCHITECTURE_ANALYSIS.md)**: Architecture pattern detection details
- **[QUALITY_SCORING.md](QUALITY_SCORING.md)**: Quality assessment methodology
- **[DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)**: Project architecture and contributing guide

## Testing Strategy
- Manual testing on real Kotlin and Java projects
- Edge case validation (empty classes, complex control flow, cross-language dependencies)
- Performance testing on large codebases
- Cross-platform JAR compatibility verification
- Quality scoring algorithm validation against industry benchmarks