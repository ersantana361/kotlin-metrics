# PR Diff Analysis Feature

## Overview

The PR Diff Analysis feature extends the kotlin-metrics tool to automatically analyze code quality improvements and regressions from Git pull request diffs. This feature provides objective metrics to evaluate the impact of code changes during code review processes.

## Feature Description

### Core Functionality

The feature adds a new command-line option `--pr-diff` that accepts a path to a file containing a Git diff (typically from a pull request). The tool then:

1. **Parses the diff** to identify modified files and extract before/after code snippets
2. **Analyzes both versions** using the existing kotlin-metrics CK suite and quality assessment algorithms
3. **Generates comparative reports** showing metrics improvements/regressions
4. **Provides actionable insights** for code review and refactoring decisions

### Supported Diff Formats

- **Git unified diff format** (output from `git diff`)
- **GitHub PR diff format** (downloaded from GitHub API)
- **GitLab merge request diffs**
- **Standard unified diff format** from other VCS systems
- **Multi-language support** - Kotlin (.kt) and Java (.java) files
- **Mixed codebases** - Handles projects with both Kotlin and Java

### Analysis Scope

The feature analyzes the following metrics across diff changes:

#### **Complexity Metrics**
- **Cyclomatic Complexity** - Control flow complexity measurement
- **Cognitive Complexity** - Human perception of code complexity
- **Method Length** - Lines of code per method
- **Nesting Depth** - Maximum control structure nesting levels

#### **CK Metrics Suite**
- **WMC (Weighted Methods per Class)** - Sum of method complexities
- **CBO (Coupling Between Objects)** - Inter-class dependencies
- **RFC (Response For a Class)** - Method invocation response set
- **LCOM (Lack of Cohesion of Methods)** - Class cohesion measurement
- **DIT (Depth of Inheritance Tree)** - Inheritance hierarchy depth
- **NOC (Number of Children)** - Direct subclass count

#### **Quality Metrics**
- **Maintainability Index** - Overall code maintainability score
- **Technical Debt Index** - Accumulated technical debt measurement
- **Code Duplication** - Duplicate code detection
- **Test Coverage Impact** - Effect on test coverage metrics

#### **Connascence Analysis**
- **Connascence of Position** - Parameter order dependencies
- **Connascence of Algorithm** - Shared algorithm dependencies
- **Connascence of Meaning** - Shared value meaning dependencies
- **Connascence of Identity** - Shared object identity dependencies

## Usage

### Command Line Interface

```bash
# Analyze a PR diff file
kotlin-metrics --pr-diff path/to/pr-diff.patch

# Analyze with specific output format
kotlin-metrics --pr-diff pr-changes.diff --html --output pr-report.html

# Analyze with baseline comparison
kotlin-metrics --pr-diff changes.patch --baseline-branch main --html

# Generate markdown report for GitHub comments
kotlin-metrics --pr-diff pr.diff --markdown --output pr-comment.md
```

### Configuration Options

```bash
# Analysis options
--pr-diff <file>              Path to PR diff file (required)
--baseline-branch <branch>    Git branch to use as baseline (default: main)
--context-lines <number>      Number of context lines to include (default: 3)
--ignore-whitespace          Ignore whitespace-only changes
--include-tests              Include test files in analysis (default: false)

# Output options
--output <file>              Output file path
--html                       Generate HTML report
--markdown                   Generate markdown report
--json                       Generate JSON report for CI/CD integration
--console                    Display results in console (default)

# Filtering options
--min-improvement <percent>   Only report improvements above threshold
--show-regressions-only      Only show metrics that got worse
--focus-complexity           Focus on complexity-related metrics
--focus-coupling             Focus on coupling-related metrics
```

### Input File Format

The feature accepts standard Git diff format for both Java and Kotlin files:

#### Java Example:
```diff
diff --git a/src/main/java/com/example/Service.java b/src/main/java/com/example/Service.java
index 1234567..abcdefg 100644
--- a/src/main/java/com/example/Service.java
+++ b/src/main/java/com/example/Service.java
@@ -10,15 +10,8 @@ public class Service {
     public Result processData(String input) {
-        if (input != null) {
-            if (input.length() > 0) {
-                if (isValid(input)) {
-                    return processValidInput(input);
-                } else {
-                    throw new IllegalArgumentException("Invalid input");
-                }
-            }
-        }
-        throw new IllegalArgumentException("Input cannot be null or empty");
+        if (input == null || input.isEmpty()) {
+            throw new IllegalArgumentException("Input cannot be null or empty");
+        }
+        if (!isValid(input)) {
+            throw new IllegalArgumentException("Invalid input");
+        }
+        return processValidInput(input);
     }
 }
```

#### Kotlin Example:
```diff
diff --git a/src/main/kotlin/com/example/UserService.kt b/src/main/kotlin/com/example/UserService.kt
index 1234567..abcdefg 100644
--- a/src/main/kotlin/com/example/UserService.kt
+++ b/src/main/kotlin/com/example/UserService.kt
@@ -8,12 +8,8 @@ class UserService {
     fun validateUser(user: User): ValidationResult {
-        if (user.name != null) {
-            if (user.name.isNotEmpty()) {
-                if (user.email != null && user.email.contains("@")) {
-                    return ValidationResult.SUCCESS
-                }
-            }
-        }
-        return ValidationResult.FAILURE
+        if (user.name.isNullOrEmpty()) return ValidationResult.FAILURE
+        if (user.email.isNullOrEmpty()) return ValidationResult.FAILURE
+        if (!user.email.contains("@")) return ValidationResult.FAILURE
+        
+        return ValidationResult.SUCCESS
     }
 }
```

## Output Formats

### HTML Report

Professional dashboard-style report with:
- **Executive summary** with key metrics improvements
- **Interactive charts** showing before/after comparisons
- **Tabbed interface** for different metric categories
- **Detailed explanations** of each metric and its impact
- **Code comparison** with syntax highlighting for both Java and Kotlin
- **Proper code formatting** with line breaks and indentation preserved
- **Language-specific analysis** adapted for Java vs Kotlin patterns
- **Actionable recommendations** for further improvements

#### Code Formatting Features:
- **Syntax highlighting** for keywords, strings, comments, and operators
- **Proper indentation** preservation in before/after comparisons
- **Line numbering** for easy reference
- **Language detection** automatic recognition of Java/Kotlin files
- **Side-by-side diff view** with clear visual indicators

### Markdown Report

Optimized for GitHub/GitLab PR comments:
- **Compact summary** of key improvements/regressions
- **Table format** for easy scanning
- **Embedded charts** (using shields.io or similar)
- **Collapsible sections** for detailed analysis
- **Code blocks** with proper language syntax highlighting
- **Action items** and recommendations

#### Example Markdown Output:
```markdown
## 🚀 Code Quality Analysis

### 📊 Summary
- **Cyclomatic Complexity**: 4 → 2 (-50% 🟢)
- **Lines of Code**: 35 → 22 (-37% 🟢)
- **Maintainability Index**: 68 → 82 (+21% 🟢)

### 🔍 Detailed Analysis

<details>
<summary>📈 Java - BookingPayoutService.java</summary>

**Before:**
```java
public BookingVirtualCardDetailDto parsePayoutChargeDetails(Long reservationId, String reservationPayoutJson) {
    if (reservationPayout.data() != null) {
        if (reservationPayout.data().virtualCreditCards() != null) {
            List<VirtualCreditCard> vccs = reservationPayout.data().virtualCreditCards();
            if (!vccs.isEmpty()) {
                // Process...
            }
        }
    }
}
```

**After:**
```java
public BookingVirtualCardDetailDto parsePayoutChargeDetails(Long reservationId, String reservationPayoutJson) {
    ReservationPayoutDto reservationPayout = parseReservationPayout(reservationPayoutJson, reservationId);
    if (reservationPayout.data() == null) {
        throw new RuntimeException("There is no Data at ReservationPayout...");
    }
    return reservationPayout.data().getLatestVirtualCreditCard().toBookingVirtualCardDetail();
}
```

</details>
```

### JSON Report

Machine-readable format for CI/CD integration:
- **Structured metrics data** with before/after values
- **Improvement percentages** for each metric
- **Risk assessment** classifications
- **Violation details** with specific recommendations
- **Trend analysis** when baseline data is available

### Console Output

Quick summary for command-line usage:
- **Color-coded indicators** (green for improvements, red for regressions)
- **Compact metric display** with key values
- **Progress indicators** for long-running analysis
- **Summary statistics** and recommendations

## Integration Examples

### GitHub Actions Workflow

```yaml
name: PR Code Quality Analysis
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

### GitLab CI/CD Pipeline

```yaml
code-quality-analysis:
  stage: test
  script:
    - git diff origin/$CI_MERGE_REQUEST_TARGET_BRANCH_NAME...HEAD > mr-changes.diff
    - kotlin-metrics --pr-diff mr-changes.diff --html --output quality-report.html
    - kotlin-metrics --pr-diff mr-changes.diff --json --output quality-metrics.json
  artifacts:
    reports:
      junit: quality-metrics.json
    paths:
      - quality-report.html
  only:
    - merge_requests
```

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    stages {
        stage('Quality Analysis') {
            when {
                changeRequest()
            }
            steps {
                sh '''
                    git diff origin/${CHANGE_TARGET}...HEAD > pr-changes.diff
                    kotlin-metrics --pr-diff pr-changes.diff --html --output quality-report.html
                '''
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: '.',
                    reportFiles: 'quality-report.html',
                    reportName: 'Code Quality Analysis'
                ])
            }
        }
    }
}
```

## Implementation Details

### Technical Architecture

```kotlin
class PRDiffAnalyzer {
    fun analyzeDiff(diffFile: File, options: AnalysisOptions): DiffAnalysisResult {
        val diffParser = DiffParser()
        val parsedDiff = diffParser.parse(diffFile)
        
        val beforeAnalysis = analyzeCodeVersion(parsedDiff.beforeVersion)
        val afterAnalysis = analyzeCodeVersion(parsedDiff.afterVersion)
        
        return DiffAnalysisResult(
            improvements = calculateImprovements(beforeAnalysis, afterAnalysis),
            regressions = calculateRegressions(beforeAnalysis, afterAnalysis),
            summary = generateSummary(beforeAnalysis, afterAnalysis)
        )
    }
    
    private fun analyzeCodeVersion(codeVersion: CodeVersion): List<ClassAnalysis> {
        val analyses = mutableListOf<ClassAnalysis>()
        
        codeVersion.files.forEach { file ->
            when (file.extension) {
                "kt" -> {
                    val kotlinAnalyzer = KotlinCodeAnalyzer()
                    val kotlinFile = kotlinParser.parse(file)
                    analyses.addAll(kotlinAnalyzer.analyze(kotlinFile))
                }
                "java" -> {
                    val javaAnalyzer = JavaCodeAnalyzer()
                    val javaFile = javaParser.parse(file)
                    analyses.addAll(javaAnalyzer.analyze(javaFile))
                }
            }
        }
        
        return analyses
    }
}

class CodeFormatter {
    fun formatForHTML(code: String, language: String): String {
        return when (language.lowercase()) {
            "kotlin" -> formatKotlinCode(code)
            "java" -> formatJavaCode(code)
            else -> formatGenericCode(code)
        }
    }
    
    private fun formatKotlinCode(code: String): String {
        return code
            .replace(Regex("\\b(fun|val|var|class|object|interface|enum|when|if|else|for|while|return|null|true|false|is|as|in|out|try|catch|finally|throw|data|sealed|abstract|open|override|private|protected|internal|public)\\b"), 
                    "<span class=\"keyword\">$1</span>")
            .replace(Regex("\"([^\"]*)\"|'([^']*)'"), 
                    "<span class=\"string\">\"$1$2\"</span>")
            .replace(Regex("//.*"), 
                    "<span class=\"comment\">$0</span>")
            .replace(Regex("/\\*[\\s\\S]*?\\*/"), 
                    "<span class=\"comment\">$0</span>")
    }
    
    private fun formatJavaCode(code: String): String {
        return code
            .replace(Regex("\\b(public|private|protected|static|final|abstract|synchronized|volatile|transient|native|strictfp|class|interface|enum|extends|implements|import|package|if|else|for|while|do|switch|case|default|break|continue|return|try|catch|finally|throw|throws|new|this|super|null|true|false|instanceof)\\b"), 
                    "<span class=\"keyword\">$1</span>")
            .replace(Regex("\"([^\"]*)\"|'([^']*)'"), 
                    "<span class=\"string\">\"$1$2\"</span>")
            .replace(Regex("//.*"), 
                    "<span class=\"comment\">$0</span>")
            .replace(Regex("/\\*[\\s\\S]*?\\*/"), 
                    "<span class=\"comment\">$0</span>")
    }
}
```

### Key Components

1. **DiffParser** - Parses Git diff format and extracts code changes
2. **VersionAnalyzer** - Analyzes before/after code versions independently
3. **MetricsComparator** - Compares metrics between versions
4. **ReportGenerator** - Generates output in various formats
5. **ChangeClassifier** - Categorizes changes (improvement/regression/neutral)
6. **CodeFormatter** - Handles syntax highlighting and formatting for Java/Kotlin
7. **LanguageDetector** - Automatically detects file language from extension and content
8. **MultiLanguageParser** - Unified parser supporting both Java and Kotlin syntax

### Error Handling

- **Invalid diff format** - Clear error messages with format requirements
- **Missing baseline** - Graceful degradation with warnings
- **Parse errors** - Detailed error reporting with line numbers
- **Large diffs** - Performance optimization and progress indicators
- **Binary files** - Automatic exclusion with informational messages
- **Mixed language projects** - Seamless handling of Java/Kotlin combinations
- **Unsupported file types** - Graceful skipping with informational messages
- **Syntax errors** - Robust parsing that handles incomplete code snippets
- **Encoding issues** - UTF-8 support with fallback handling

## Benefits

### For Development Teams

- **Objective code review** - Data-driven feedback on code changes
- **Quality trend tracking** - Monitor code quality over time
- **Refactoring validation** - Confirm that refactoring actually improves quality
- **Technical debt management** - Quantify debt reduction efforts
- **Learning tool** - Understand impact of different coding patterns

### For Project Management

- **Quality metrics** - Track project health and technical debt
- **Risk assessment** - Identify high-risk changes early
- **Resource planning** - Prioritize refactoring efforts based on impact
- **Progress tracking** - Measure improvement over time
- **Stakeholder communication** - Communicate quality improvements to business

### For CI/CD Integration

- **Automated quality gates** - Fail builds on quality regressions
- **Trend analysis** - Historical quality data for decision making
- **Integration flexibility** - Support for various CI/CD platforms
- **Customizable thresholds** - Configure quality gates per project
- **Rich reporting** - Multiple output formats for different audiences

## Future Enhancements

### Planned Features

- **Baseline comparison** - Compare against historical baselines
- **Team collaboration** - Multi-reviewer analysis and consensus
- **Custom metrics** - Plugin system for domain-specific metrics
- **AI-powered insights** - Machine learning for pattern recognition
- **Integration APIs** - REST API for custom integrations

### Advanced Analysis

- **Semantic analysis** - Understand code meaning, not just structure
- **Performance impact** - Predict performance implications of changes
- **Security impact** - Identify security-related quality changes
- **Architecture analysis** - Evaluate architectural pattern adherence
- **Cross-language support** - Extend beyond Kotlin/Java to other languages

## Configuration

### Default Settings

```yaml
# kotlin-metrics-pr.yml
analysis:
  include_tests: false
  context_lines: 3
  ignore_whitespace: true
  min_improvement_threshold: 5.0
  supported_languages:
    - kotlin
    - java
  
language_specific:
  kotlin:
    analyze_data_classes: true
    analyze_extension_functions: true
    analyze_lambda_expressions: true
    analyze_coroutines: true
  
  java:
    analyze_anonymous_classes: true
    analyze_lambda_expressions: true
    analyze_stream_operations: true
    analyze_generics: true
  
metrics:
  complexity:
    cyclomatic_complexity: true
    cognitive_complexity: true
    nesting_depth: true
  
  ck_suite:
    wmc: true
    cbo: true
    rfc: true
    lcom: true
    dit: true
    noc: true
  
  quality:
    maintainability_index: true
    technical_debt_index: true
    code_duplication: true

output:
  default_format: "console"
  show_progress: true
  color_output: true
  detailed_explanations: true
  code_formatting:
    syntax_highlighting: true
    preserve_indentation: true
    show_line_numbers: true
    max_line_length: 120
```

### Custom Configuration

```kotlin
// Custom metric configuration
val config = PRAnalysisConfig(
    complexityWeights = mapOf(
        "cyclomatic" to 0.4,
        "cognitive" to 0.3,
        "nesting" to 0.3
    ),
    qualityThresholds = QualityThresholds(
        excellent = 8.0,
        good = 6.0,
        moderate = 4.0
    ),
    reportingOptions = ReportingOptions(
        includeCodeSnippets = true,
        maxSnippetLines = 20,
        includeRecommendations = true
    )
)
```

## Conclusion

The PR Diff Analysis feature transforms the kotlin-metrics tool into a comprehensive code review assistant, providing objective, data-driven insights into code quality changes. By integrating seamlessly with existing development workflows, it enables teams to make informed decisions about code changes and continuously improve their codebase quality.

This feature bridges the gap between static code analysis and practical code review, making quality metrics accessible and actionable for development teams of all sizes.