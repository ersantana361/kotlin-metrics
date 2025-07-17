# Kotlin & Java Metrics Analyzer

A comprehensive command-line tool for analyzing **Kotlin and Java** code quality with **complete CK (Chidamber and Kemerer) metrics suite**, architecture analysis, and DDD pattern detection. Supports mixed-language codebases with unified reporting and interactive visualizations.

## 📊 Supported Metrics

### Complete CK Metrics Suite
The tool implements all 9 Chidamber and Kemerer object-oriented metrics:

#### **LCOM (Lack of Cohesion of Methods)**
Measures how well the methods of a class are related through shared properties:
- **LCOM = 0**: High cohesion (excellent) - methods share properties effectively
- **LCOM 1-2**: Good cohesion - minor improvements possible
- **LCOM 3-5**: Moderate cohesion - consider refactoring
- **LCOM > 5**: Poor cohesion - strong refactoring recommended

#### **WMC (Weighted Methods per Class)**
Sum of cyclomatic complexities of all methods in a class:
- **WMC 1-10**: Simple class - easy to understand
- **WMC 11-20**: Moderate complexity - acceptable
- **WMC 21-50**: Complex class - consider decomposition
- **WMC > 50**: Very complex - critical refactoring needed

#### **DIT (Depth of Inheritance Tree)**
Measures inheritance depth from the root of hierarchy:
- **DIT 0-2**: Shallow hierarchy - good for understanding
- **DIT 3-5**: Moderate depth - acceptable complexity
- **DIT 6+**: Deep hierarchy - potential maintenance issues

#### **NOC (Number of Children)**
Count of direct subclasses:
- **NOC 0-5**: Appropriate abstraction level
- **NOC 6-10**: Consider if abstraction is too broad
- **NOC > 10**: Overly broad abstraction - refactor hierarchy

#### **CBO (Coupling Between Objects)**
Number of classes this class is coupled to:
- **CBO 0-5**: Low coupling - good design
- **CBO 6-15**: Moderate coupling - acceptable
- **CBO 16+**: High coupling - reduce dependencies

#### **RFC (Response For a Class)**
Number of methods that can be invoked in response to a message:
- **RFC 1-20**: Simple interface - easy to use
- **RFC 21-50**: Moderate interface complexity
- **RFC > 50**: Complex interface - consider decomposition

#### **CA (Afferent Coupling)**
Number of classes that depend on this class:
- **High CA**: Stable component (many dependents)
- **Monitor**: Changes impact many classes

#### **CE (Efferent Coupling)**
Number of classes this class depends on:
- **High CE**: Unstable component (many dependencies)
- **Monitor**: Sensitive to external changes

#### **Cyclomatic Complexity (CC)**
Measures code complexity based on decision points and control flow:
- **CC 1-5**: Simple - easy to understand and test
- **CC 6-10**: Moderate - acceptable complexity
- **CC 11-20**: Complex - consider refactoring
- **CC 21+**: Very complex - critical refactoring needed

### Quality Scoring System
Each class receives a comprehensive quality score (0-10 scale) based on:
- **CK Metrics Analysis**: All 9 metrics weighted by importance
- **Risk Assessment**: Critical/High/Medium/Low risk classification
- **Correlation Analysis**: Inter-metric relationships and patterns
- **Best Practice Compliance**: Industry standard thresholds

### Architecture Analysis
Comprehensive analysis of software architecture patterns and design quality:
- **DDD Pattern Detection**: Identifies Entities, Value Objects, Services, Repositories, Aggregates, and Domain Events
- **Layered Architecture**: Analyzes presentation, application, domain, data, and infrastructure layers
- **Dependency Graph**: Maps class relationships and detects circular dependencies
- **Architecture Patterns**: Detects Layered, Hexagonal, Clean, and Onion architectures
- **Violation Detection**: Identifies layer violations and architectural anti-patterns

## ✨ Features

- **🌐 Multi-Language Support**: Analyzes Kotlin and Java files in mixed codebases
- **📊 Complete CK Metrics**: All 9 Chidamber and Kemerer metrics with quality scoring
- **🎨 Interactive HTML Reports**: Professional 6-tab dashboard with comprehensive analysis
- **📝 Flexible Output Formats**: Choose console, HTML, or markdown reports with `--console`, `--html`, `--markdown`
- **📄 Single File Analysis**: Dedicated analysis for individual Kotlin/Java files with detailed metrics
- **📈 Advanced Analytics**: Quality correlation analysis, risk assessment, and trend identification
- **🏗️ Architecture Visualization**: Layer diagrams, DDD patterns, and dependency graphs
- **💡 Smart Interpretation Guides**: Detailed tooltips explaining metric importance and thresholds
- **🔍 Method-Level Analysis**: Detailed breakdown of each method's complexity across languages
- **📱 Responsive Dashboard**: Modern Bootstrap 5 interface with Chart.js visualizations
- **📊 Real-time Filtering**: Filter by quality levels, complexity, and architectural patterns
- **🚨 Violation Detection**: Identifies architecture violations with suggested fixes
- **📦 Standalone JAR**: No dependencies, works across projects
- **🔄 Backward Compatible**: Existing projects work unchanged with enhanced features

## Installation

### Building from Source

1. Clone the repository
2. Build the fat JAR:
   ```bash
   ./gradlew fatJar
   ```

### Setting up Global Alias

Add this to your `~/.bashrc`:
```bash
alias kotlin-metrics="java -jar /path/to/kotlin-metrics/build/libs/kotlin-metrics-all-1.0.0.jar"
```

Then reload your shell:
```bash
source ~/.bashrc
```

## Usage

### Command Line Interface

The tool provides flexible output options to generate exactly what you need:

```bash
kotlin-metrics [OPTIONS]

Options:
  -f, --file <file>       Analyze a single file
  -o, --output <file>     Output file for report (default: stdout)
  -d, --directory <dir>   Directory to analyze (default: current directory)
  --pr-diff <file>        Analyze PR diff file with full context
  
Output Formats:
  --console               Generate console output (default for projects)
  --html                  Generate HTML report (default for projects)
  --markdown              Generate markdown report (default for single files)
  --json                  Generate JSON report (for CI/CD integration)
  
Other:
  -h, --help              Show this help message
```

### Project Analysis Examples

```bash
# Default: console summary + interactive HTML report
kotlin-metrics

# Console output only
kotlin-metrics --console

# HTML report only
kotlin-metrics --html

# Markdown report only
kotlin-metrics --markdown

# All three formats
kotlin-metrics --console --html --markdown

# Markdown report to file
kotlin-metrics --markdown -o project_metrics.md

# Analyze specific directory
kotlin-metrics -d /path/to/project --html
```

### Single File Analysis Examples

```bash
# Default: detailed markdown to stdout
kotlin-metrics -f MyClass.kt

# Console summary of single file
kotlin-metrics -f MyClass.kt --console

# HTML report for single file
kotlin-metrics -f MyClass.kt --html

# Multiple formats for single file
kotlin-metrics -f MyClass.kt --console --html --markdown

# Markdown report to file
kotlin-metrics -f MyClass.kt --markdown -o class_report.md

# Both Java and Kotlin files supported
kotlin-metrics -f User.java --html
kotlin-metrics -f UserService.kt --markdown
```

### Output Format Details

| Format | Project Analysis | Single File | Best Use Cases |
|--------|------------------|-------------|----------------|
| **Console** | Comprehensive terminal summary with key metrics and priorities | Concise summary with quality score and main metrics | Quick overview, CI/CD integration, terminal workflows |
| **HTML** | Full 6-tab interactive dashboard with visualizations | Complete analysis in professional web format | Detailed analysis, presentations, team reviews |
| **Markdown** | Structured project overview with class summaries | Complete metrics breakdown with interpretations | Documentation, code reviews, version control |

### Quick Reference

| Want to... | Command |
|------------|---------|
| Quick project overview | `kotlin-metrics --console` |
| Full interactive analysis | `kotlin-metrics --html` |
| Documentation-ready report | `kotlin-metrics --markdown` |
| All formats | `kotlin-metrics --console --html --markdown` |
| Single file analysis | `kotlin-metrics -f MyClass.kt` |
| Single file to HTML | `kotlin-metrics -f MyClass.kt --html` |
| Save markdown report | `kotlin-metrics --markdown -o report.md` |
| **PR diff analysis** | `kotlin-metrics --pr-diff changes.diff` |
| **PR diff HTML report** | `kotlin-metrics --pr-diff pr.patch --html` |
| **PR diff for GitHub** | `kotlin-metrics --pr-diff pr.diff --markdown -o pr-report.md` |
| **PR diff for CI/CD** | `kotlin-metrics --pr-diff changes.patch --json` |
| Help | `kotlin-metrics --help` |

### Supported Project Types
- **Kotlin-only projects**: Analyzes all `.kt` files with complete CK metrics
- **Java-only projects**: Analyzes all `.java` files with same metrics
- **Mixed codebases**: Unified analysis of both `.kt` and `.java` files

### Example Terminal Output (Enhanced with CK Metrics)
```
============================================================
               📊 KOTLIN METRICS ANALYSIS SUMMARY
                    Generated: 2024-01-15 14:30:22
============================================================

Analyzing Kotlin and Java files in: /path/to/project
Found 7 Kotlin files and 5 Java files

📈 PROJECT OVERVIEW
   Classes analyzed: 15
   Total methods: 87
   Total properties: 45
   Average quality score: 7.2/10 ✅

🎯 CK METRICS SUMMARY
   Average LCOM: 3.20 ⚠️
   Average WMC: 15.40 👍
   Average DIT: 2.10 ✅
   Average NOC: 1.30 ✅
   Average CBO: 8.70 👍
   Average RFC: 18.50 👍
   Average CA: 4.20 ✅
   Average CE: 6.80 👍
   Average CC: 6.80 👍

📊 QUALITY DISTRIBUTION
   ✅ Excellent (8.0-10): 4 classes (26.7%)
   👍 Good (6.0-7.9): 6 classes (40.0%)
   ⚠️ Fair (4.0-5.9): 3 classes (20.0%)
   ❌ Poor (0-3.9): 2 classes (13.3%)

🎯 RISK ASSESSMENT
   🔴 Critical Risk: 1 class
   🟠 High Risk: 2 classes
   🟡 Medium Risk: 5 classes
   🟢 Low Risk: 7 classes

⚠️  PRIORITY REFACTORING TARGETS
   📝 UserService (Quality: 3.2/10, WMC:45, CBO:18)
   📝 DataProcessor (Quality: 4.1/10, LCOM:8, RFC:52)

🏗️ ARCHITECTURE ANALYSIS
   Pattern: LAYERED
   Layers: 4
   Dependencies: 12
   Violations: 2

📐 DDD PATTERNS DETECTED
   Entities: 8 (avg confidence: 78%)
   Value Objects: 5 (avg confidence: 85%)
   Services: 12 (avg confidence: 72%)
   Repositories: 4 (avg confidence: 88%)
   Aggregates: 3 (avg confidence: 65%)

🌐 DEPENDENCY GRAPH
   Nodes: 45
   Edges: 67
   Cycles: 1
   Packages: 8

📄 Interactive report: kotlin-metrics-report.html
   Open in browser for detailed CK metrics analysis, correlation charts, 
   and architectural insights with interpretation guides
============================================================
```

## Requirements

- Java 17 or higher
- Source files: `.kt` (Kotlin) and/or `.java` (Java)

## 📊 Enhanced HTML Report Features

The generated HTML report provides comprehensive interactive analysis across 6 tabs:

### 📈 Overview Tab (Enhanced)
- **CK Metrics Summary**: All 9 metrics with quality indicators
- **Quality Score Distribution**: Visual breakdown with correlation analysis
- **Risk Assessment Matrix**: Critical to low risk classification
- **Project Health Dashboard**: Real-time quality indicators

### 🎯 LCOM Analysis Tab
- **Cohesion Analysis**: Interactive charts and detailed explanations
- **Quality Filtering**: Filter by excellent/good/fair/poor cohesion
- **Smart Suggestions**: Tooltip guides for interpretation
- **Refactoring Recommendations**: Specific actionable advice

### 🌀 Complexity (WMC/CC) Tab
- **Complexity Distribution**: Method and class-level analysis
- **Complexity vs Size Correlation**: Scatter plots with trend lines
- **Risk Level Classification**: Color-coded complexity indicators
- **Filtering by Complexity**: Simple/Moderate/Complex/Critical

### 📊 CK Metrics Tab (New)
- **Complete CK Suite Visualization**: All 9 metrics with benchmarks
- **Correlation Matrix**: Interactive heatmap showing metric relationships
- **Quality Scoring Breakdown**: Detailed scoring explanation
- **Threshold Guidance**: Industry standard benchmarks with tooltips

### 🏗️ Architecture Analysis Tab
- **Pattern Detection**: Automated architecture pattern identification
- **Layer Visualization**: Interactive layer dependency diagrams
- **DDD Pattern Distribution**: Confidence-weighted pattern analysis
- **Violation Reports**: Detailed architecture anti-pattern detection

### 🔍 Quality Analytics Tab (New)
- **Risk Assessment**: Comprehensive risk analysis with recommendations
- **Quality Trends**: Historical comparison and improvement tracking
- **Correlation Analysis**: Statistical relationships between metrics
- **Improvement Roadmap**: Prioritized refactoring suggestions

### ⚡ Interactive Features
- **Real-time Filtering**: Filter by any metric or quality level
- **Click-to-Sort**: Advanced sorting with multi-column support
- **Responsive Design**: Optimized for desktop, tablet, and mobile
- **Export Capabilities**: Save filtered results and analysis reports
- **Interpretation Guides**: Comprehensive tooltips and help system

## 🚀 Enhanced PR Diff Analysis

The tool now includes **enhanced PR diff analysis** that provides comprehensive insights into code changes with full source context. This goes beyond simple diff parsing to analyze the complete impact of changes across your entire codebase.

### ✨ Key Features

- **🔍 Complete Source Context**: Analyzes entire files, not just diff snippets
- **📊 Full CK Metrics Comparison**: Before/after analysis with all 9 CK metrics
- **🎯 Cross-File Impact Analysis**: Identifies ripple effects across the codebase
- **🔧 Semantic Change Detection**: Detects method signatures, API changes, and behavioral modifications
- **📈 Multiple Output Formats**: Console, HTML, Markdown, and JSON reports
- **⚡ CI/CD Integration**: JSON output for automated quality gates

### 🛠️ Usage

```bash
# Basic PR diff analysis (console output)
kotlin-metrics --pr-diff changes.diff

# Generate HTML report for detailed analysis
kotlin-metrics --pr-diff pr.patch --html

# Generate markdown report for GitHub/GitLab PR comments
kotlin-metrics --pr-diff pr.diff --markdown -o pr-report.md

# Generate JSON report for CI/CD integration
kotlin-metrics --pr-diff changes.patch --json
```

### 📋 Creating PR Diff Files

```bash
# From current changes
git diff > my-changes.diff

# From a specific commit range
git diff HEAD~3..HEAD > recent-changes.diff

# From branch comparison
git diff origin/main...HEAD > pr-changes.diff

# From GitHub PR (using gh CLI)
gh pr diff 123 > pr-123.diff
```

### 📊 Analysis Capabilities

#### **Metrics Comparison**
- **Before/After CK Metrics**: Complete comparison of all 9 CK metrics
- **Quality Score Changes**: Overall quality impact assessment
- **Complexity Analysis**: Method and class-level complexity changes
- **Coupling Analysis**: Dependency changes and their impact

#### **Impact Analysis**
- **Directly Affected Files**: Files modified in the PR
- **Indirectly Affected Files**: Files that depend on changed code
- **Dependency Graph Analysis**: Cross-file relationship mapping
- **Risk Assessment**: Critical/High/Medium/Low risk classification

#### **Semantic Analysis**
- **Method Changes**: Added, removed, or modified methods
- **API Changes**: Public interface modifications with breaking change detection
- **Signature Changes**: Parameter, return type, and visibility changes
- **Behavioral Changes**: Logic flow and complexity modifications

### 📈 Output Formats

#### **Console Output**
```
========================================
Enhanced PR Diff Analysis Report
========================================
Generated: 2024-01-15 14:30:22

SUMMARY:
--------
Improvements: 3
Regressions: 1
Total Changes: 4
Affected Files: 2
Overall Impact: MEDIUM

METRICS COMPARISON:
------------------
Improvements:
  ✅ TestService.LCOM: 5.00 → 2.00 (-60.0%)
  ✅ TestService.Quality Score: 4.2 → 7.1 (+69.0%)
  ✅ TestService.Cyclomatic Complexity: 4 → 1 (-75.0%)

Regressions:
  ❌ UserService.Method Count: 3 → 2 (-33.3%)

IMPACT ANALYSIS:
---------------
Risk Level: HIGH
Impact Percentage: 15.5%
Directly Affected Files: 2
Indirectly Affected Files: 3
```

#### **HTML Report**
- **Interactive Dashboard**: Bootstrap-based UI with charts and visualizations
- **Before/After Comparison**: Side-by-side source code comparison
- **Impact Visualization**: Dependency graphs and affected file lists
- **Metrics Charts**: Interactive charts showing metric changes

#### **Markdown Report**
```markdown
# 🚀 Enhanced PR Diff Analysis Report

## 📊 Executive Summary

| Metric | Count |
|--------|-------|
| 🟢 Improvements | 3 |
| 🔴 Regressions | 1 |
| 🔄 Total Changes | 4 |
| 📁 Affected Files | 2 |

**Overall Impact**: MEDIUM (2 net improvements)

## 📈 Metrics Comparison

### 🟢 Improvements
- **TestService.LCOM**: 5.00 → 2.00 (-60.0% 🟢)
- **TestService.Quality Score**: 4.2 → 7.1 (+69.0% 🟢)
```

#### **JSON Report**
```json
{
  "timestamp": "2024-01-15T14:30:22",
  "summary": {
    "improvements": 3,
    "regressions": 1,
    "totalChanges": 4,
    "affectedFiles": 2,
    "overallImpact": "MEDIUM"
  },
  "metricsComparison": {
    "improvements": [...],
    "regressions": [...]
  },
  "impactAnalysis": {
    "riskLevel": "HIGH",
    "impactPercentage": 15.5,
    "directlyAffectedFiles": [...],
    "indirectlyAffectedFiles": [...]
  }
}
```

### 🔄 CI/CD Integration

#### **GitHub Actions Example**
```yaml
name: PR Quality Analysis
on:
  pull_request:
    types: [opened, synchronize]

jobs:
  quality-analysis:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0
      
      - name: Generate PR diff
        run: |
          git diff origin/${{ github.base_ref }}...HEAD > pr-changes.diff
      
      - name: Analyze code quality changes
        run: |
          kotlin-metrics --pr-diff pr-changes.diff --markdown --output pr-report.md
      
      - name: Comment PR
        uses: actions/github-script@v6
        with:
          script: |
            const fs = require('fs');
            const report = fs.readFileSync('pr-report.md', 'utf8');
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: report
            });
```

#### **Quality Gates**
```bash
# Fail CI if quality decreases significantly
kotlin-metrics --pr-diff pr.diff --json > metrics.json

# Parse results and fail if regressions > improvements
python check_quality_gate.py metrics.json
```

## 🔍 Analysis Details

### CK Metrics Implementation

#### LCOM Calculation
```
LCOM = P - Q (minimum 0)
- P = method pairs with no shared properties
- Q = method pairs with shared properties
```

#### WMC Calculation
```
WMC = Σ(Cyclomatic Complexity of all methods)
- Includes constructor complexity
- Language-specific control flow analysis
```

#### Inheritance Metrics (DIT/NOC)
```
DIT = Maximum depth from class to root hierarchy
NOC = Count of direct subclasses
- Cross-language inheritance tracking
- Cycle detection for robustness
```

#### Coupling Metrics (CBO/RFC/CA/CE)
```
CBO = Count of classes this class depends on
RFC = Count of methods + remote methods called
CA = Count of classes depending on this class
CE = Count of external classes this class uses
- Supports cross-language dependencies
- Framework-aware coupling analysis
```

### Quality Scoring Algorithm
```kotlin
Quality Score = Weighted Average of:
- LCOM Score (20%): Cohesion quality
- WMC Score (20%): Complexity management
- Coupling Score (25%): CBO + RFC + CA + CE combined
- Inheritance Score (15%): DIT + NOC balance
- Complexity Score (20%): Cyclomatic complexity
```

### Cross-Language Analysis
The tool provides unified analysis across Kotlin and Java:

**Kotlin-Specific Features:**
- **Kotlin PSI Analysis**: Native Kotlin compiler integration
- **Extension Functions**: Tracked in coupling analysis
- **Data Classes**: Enhanced value object detection
- **Coroutines**: Complexity analysis for suspend functions

**Java-Specific Features:**
- **JavaParser Integration**: Full AST analysis
- **Annotation Support**: JPA, Spring, framework annotations
- **Enterprise Patterns**: Java-specific design pattern detection
- **Generic Type Analysis**: Advanced coupling detection

## 📚 Additional Documentation

- **[CK_METRICS_GUIDE.md](CK_METRICS_GUIDE.md)**: Complete guide to all 9 CK metrics
- **[ARCHITECTURE_ANALYSIS.md](ARCHITECTURE_ANALYSIS.md)**: Architecture pattern detection details
- **[QUALITY_SCORING.md](QUALITY_SCORING.md)**: Quality assessment methodology
- **[DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)**: Project architecture and contributing guide

## 🚀 What's New in v2.0

### Major Enhancements
- ✅ **Complete CK Metrics Suite**: All 9 metrics implemented
- ✅ **Quality Scoring System**: 0-10 scale with risk assessment
- ✅ **Enhanced HTML Reports**: 6-tab dashboard with interpretation guides
- ✅ **Flexible CLI Interface**: Clear output format control with `--console`, `--html`, `--markdown`
- ✅ **Single File Analysis**: Dedicated analysis for individual Kotlin/Java files
- ✅ **Markdown Reports**: Complete metrics breakdown with interpretations
- ✅ **Correlation Analysis**: Statistical relationships between metrics
- ✅ **Architecture Cleanup**: Eliminated code redundancy (35% smaller codebase)
- ✅ **Backward Compatibility**: Existing projects work with enhanced features

### Performance Improvements
- **35% Codebase Reduction**: Eliminated duplicate models and dead code
- **Enhanced Architecture**: Single source of truth for all data models
- **Optimized Analysis**: Improved memory usage and processing speed

## License

This project is open source and available under the MIT License.