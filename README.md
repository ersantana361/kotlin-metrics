# Kotlin Metrics Analyzer

A comprehensive command-line tool for analyzing Kotlin code quality with multiple metrics including LCOM (Lack of Cohesion of Methods) and Cyclomatic Complexity.

## 📊 Supported Metrics

### LCOM (Lack of Cohesion of Methods)
Measures how well the methods of a class are related through shared properties:
- **LCOM = 0**: High cohesion (excellent) - methods share properties effectively
- **LCOM 1-2**: Good cohesion - minor improvements possible
- **LCOM 3-5**: Moderate cohesion - consider refactoring
- **LCOM > 5**: Poor cohesion - strong refactoring recommended

### Cyclomatic Complexity (CC)
Measures code complexity based on decision points and control flow:
- **CC 1-5**: Simple - easy to understand and test
- **CC 6-10**: Moderate - acceptable complexity
- **CC 11-20**: Complex - consider refactoring
- **CC 21+**: Very complex - critical refactoring needed

## ✨ Features

- **Dual Analysis**: Both structural (LCOM) and complexity (CC) metrics
- **Interactive HTML Reports**: Professional dashboards with charts and filtering
- **Method-Level Analysis**: Detailed breakdown of each method's complexity
- **Smart Suggestions**: Actionable recommendations with tooltips
- **Terminal Summary**: Clean overview with quality distribution
- **Sortable Tables**: Click-to-sort functionality with visual indicators
- **Quality Filtering**: Filter by cohesion/complexity levels
- **Standalone JAR**: No dependencies, works across projects

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

### Basic Usage
```bash
# Navigate to your Kotlin project root
cd /path/to/your/kotlin/project

# Run the analyzer
kotlin-metrics
```

### Example Terminal Output
```
============================================================
               📊 KOTLIN METRICS ANALYSIS SUMMARY
                    Generated: 2024-01-15 14:30:22
============================================================

📈 PROJECT OVERVIEW
   Classes analyzed: 15
   Total methods: 87
   Total properties: 45

🎯 KEY METRICS
   Average LCOM: 3.20 ⚠️
   Average Complexity: 6.80 👍

📊 QUALITY DISTRIBUTION
   ✅ Excellent: 4 classes (26.7%)
   👍 Good: 6 classes (40.0%)
   ⚠️ Moderate: 3 classes (20.0%)
   ❌ Poor: 2 classes (13.3%)

⚠️  ISSUES DETECTED
   📊 2 classes have poor cohesion (LCOM > 5)
   🧠 8 methods are complex (CC > 10)

🎯 PRIORITY REFACTORING TARGETS
   📝 UserService (LCOM:8 CC:12.5)
   📝 DataProcessor (LCOM:6 CC:9.2)

📄 Interactive report: kotlin-metrics-report.html
   Open in browser for detailed analysis, charts, and suggestions
============================================================
```

## Requirements

- Java 17 or higher
- Kotlin source files (.kt)

## 📊 HTML Report Features

The generated HTML report provides comprehensive interactive analysis:

### 📈 Dashboard Overview
- **Summary Cards**: Total classes, average metrics, quality indicators
- **Visual Charts**: Distribution graphs and scatter plots
- **Quality Breakdown**: Percentage distribution across quality levels

### 🎯 LCOM Analysis Tab
- **Cohesion Distribution**: Bar chart showing LCOM value spread
- **Quality Pie Chart**: Visual breakdown of cohesion quality
- **Class Details Table**: Sortable table with filtering by quality level
- **Smart Suggestions**: Hover tooltips with actionable advice

### 🧠 Cyclomatic Complexity Tab
- **Complexity Distribution**: Method complexity histogram
- **Complexity vs Size**: Scatter plot correlating CC with lines of code
- **Method Details Table**: Every method with CC, lines, and recommendations
- **Filter by Complexity**: Simple/Moderate/Complex/Very Complex

### ⚡ Interactive Features
- **Click-to-Sort**: Any column header to sort data
- **Quality Filters**: Show only classes/methods of specific quality levels
- **Responsive Design**: Works on desktop, tablet, and mobile
- **Professional Styling**: Clean, modern interface with Bootstrap

## 🔍 Analysis Details

### LCOM Calculation
```
LCOM = P - Q (minimum 0)
- P = method pairs with no shared properties
- Q = method pairs with shared properties
```

### Cyclomatic Complexity Detection
The tool analyzes these Kotlin constructs:
- **Control Flow**: `if/else`, `when` expressions, loops
- **Exception Handling**: `try/catch` blocks
- **Logical Operators**: `&&`, `||`, `?:`
- **Branching**: Each decision point adds +1 to complexity

### Example Analysis
```kotlin
class PaymentProcessor {
    private val config: Config = Config()
    private val logger: Logger = Logger()
    
    fun processPayment(amount: Double): Result {  // CC: 4
        if (amount <= 0) return Error("Invalid amount")     // +1
        
        return when (config.paymentMethod) {                // +2 (2 branches)
            "CARD" -> processCard(amount)
            "BANK" -> processBank(amount)
        } ?: Error("Unknown method")                        // +1
    }
    
    private fun processCard(amount: Double): Result { ... } // CC: 1
    private fun processBank(amount: Double): Result { ... } // CC: 1
}
```

**Analysis Results:**
- **LCOM**: 0 (all methods share `config` or `logger`)
- **Average CC**: 2.0 (simple complexity)
- **Quality**: ✅ Excellent

## License

This project is open source and available under the MIT License.