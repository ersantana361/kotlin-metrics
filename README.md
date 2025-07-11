# Kotlin Metrics Analyzer

A comprehensive command-line tool for analyzing Kotlin code quality with multiple metrics including LCOM (Lack of Cohesion of Methods), Cyclomatic Complexity, and **Architecture Analysis** with DDD pattern detection.

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

### Architecture Analysis
Comprehensive analysis of software architecture patterns and design quality:
- **DDD Pattern Detection**: Identifies Entities, Value Objects, Services, Repositories, Aggregates, and Domain Events
- **Layered Architecture**: Analyzes presentation, application, domain, data, and infrastructure layers
- **Dependency Graph**: Maps class relationships and detects circular dependencies
- **Architecture Patterns**: Detects Layered, Hexagonal, Clean, and Onion architectures
- **Violation Detection**: Identifies layer violations and architectural anti-patterns

## ✨ Features

- **Triple Analysis**: Structural (LCOM), complexity (CC), and architecture metrics
- **Interactive HTML Reports**: Professional dashboards with charts and filtering
- **Architecture Visualization**: Layer diagrams, DDD patterns, and dependency graphs
- **Method-Level Analysis**: Detailed breakdown of each method's complexity
- **Smart Suggestions**: Actionable recommendations with tooltips
- **Terminal Summary**: Clean overview with quality distribution and architecture patterns
- **Sortable Tables**: Click-to-sort functionality with visual indicators
- **Quality Filtering**: Filter by cohesion/complexity levels
- **Violation Detection**: Identifies architecture violations with suggested fixes
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

🏗️ ARCHITECTURE ANALYSIS
   Pattern: LAYERED
   Layers: 4
   Dependencies: 12
   Violations: 2

📐 DDD PATTERNS DETECTED
   Entities: 8
   Value Objects: 5
   Services: 12
   Repositories: 4
   Aggregates: 3

🌐 DEPENDENCY GRAPH
   Nodes: 45
   Edges: 67
   Cycles: 1
   Packages: 8

📄 Interactive report: kotlin-metrics-report.html
   Open in browser for detailed analysis, charts, and architecture visualization
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

### 🏗️ Architecture Analysis Tab
- **Architecture Pattern Detection**: Identifies Layered, Hexagonal, Clean, and Onion patterns
- **Layer Visualization**: Bar charts showing classes per architectural layer
- **DDD Patterns Chart**: Distribution of Domain-Driven Design patterns
- **Dependency Graph**: Interactive visualization of class relationships
- **Violation Reports**: Detailed tables of architecture violations with suggestions
- **Pattern Details**: Confidence scores for detected entities, services, and repositories

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
// Domain Service with Payment Processing
class PaymentService {                                  // DDD: Service (80%)
    private val paymentRepository: PaymentRepository    // DDD: Repository
    private val logger: Logger
    
    fun processPayment(request: PaymentRequest): Result { // CC: 4
        if (request.amount <= 0) return Error("Invalid")   // +1
        
        val payment = Payment(                              // DDD: Entity
            id = UUID.randomUUID(),
            amount = Money(request.amount, request.currency), // DDD: Value Object
            status = PaymentStatus.PENDING
        )
        
        return when (request.method) {                      // +2 (2 branches)
            PaymentMethod.CARD -> processCard(payment)
            PaymentMethod.BANK -> processBank(payment)
        } ?: Error("Unknown method")                        // +1
    }
    
    private fun processCard(payment: Payment): Result { ... } // CC: 1
    private fun processBank(payment: Payment): Result { ... } // CC: 1
}

data class Money(val amount: BigDecimal, val currency: String) // DDD: Value Object (90%)
data class Payment(val id: UUID, val amount: Money, val status: PaymentStatus) // DDD: Entity (85%)
```

**Analysis Results:**
- **LCOM**: 0 (all methods share dependencies)
- **Average CC**: 2.0 (simple complexity)
- **Quality**: ✅ Excellent
- **Architecture**: Service layer with proper DDD patterns
- **DDD Patterns**: 1 Service, 1 Entity, 1 Value Object, 1 Repository
- **Layer**: Application layer (service package)

## License

This project is open source and available under the MIT License.