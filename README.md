# Kotlin & Java Metrics Analyzer

A comprehensive command-line tool for analyzing **Kotlin and Java** code quality with multiple metrics including LCOM (Lack of Cohesion of Methods), Cyclomatic Complexity, and **Architecture Analysis** with DDD pattern detection. Supports mixed-language codebases with unified reporting.

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

- **🌐 Multi-Language Support**: Analyzes Kotlin and Java files in mixed codebases
- **📊 Triple Analysis**: Structural (LCOM), complexity (CC), and architecture metrics
- **🎨 Interactive HTML Reports**: Professional dashboards with charts and filtering
- **🏗️ Architecture Visualization**: Layer diagrams, DDD patterns, and dependency graphs
- **🔍 Method-Level Analysis**: Detailed breakdown of each method's complexity across languages
- **💡 Smart Suggestions**: Language-specific actionable recommendations with tooltips
- **📱 Terminal Summary**: Clean overview with quality distribution and architecture patterns
- **🔄 Sortable Tables**: Click-to-sort functionality with visual indicators
- **⚡ Quality Filtering**: Filter by cohesion/complexity levels
- **🚨 Violation Detection**: Identifies architecture violations with suggested fixes
- **📦 Standalone JAR**: No dependencies, works across projects
- **🔄 Backward Compatible**: Existing Kotlin-only projects work unchanged

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
# Navigate to your project root (Kotlin, Java, or mixed)
cd /path/to/your/project

# Run the analyzer - automatically detects .kt and .java files
kotlin-metrics
```

### Supported Project Types
- **Kotlin-only projects**: Analyzes all `.kt` files (original functionality)
- **Java-only projects**: Analyzes all `.java` files with same metrics
- **Mixed codebases**: Unified analysis of both `.kt` and `.java` files

### Example Terminal Output (Mixed Project)
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
- Source files: `.kt` (Kotlin) and/or `.java` (Java)

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
The tool analyzes constructs in both languages:

**Kotlin:**
- **Control Flow**: `if/else`, `when` expressions, loops
- **Exception Handling**: `try/catch` blocks  
- **Logical Operators**: `&&`, `||`, `?:`
- **Branching**: Each decision point adds +1 to complexity

**Java:**
- **Control Flow**: `if/else`, `switch` statements, loops (`for`, `while`, `do-while`)
- **Exception Handling**: `try/catch` blocks
- **Logical Operators**: `&&`, `||`, ternary operator `? :`
- **Branching**: Each decision point adds +1 to complexity

### Example Analysis (Mixed Language)

**Kotlin Service:**
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
}

data class Money(val amount: BigDecimal, val currency: String) // DDD: Value Object (90%)
```

**Java Entity:**
```java
@Entity                                                 // DDD: Entity (95%)
public class Payment {                                  
    @Id private UUID id;                                // ID field detected
    private Money amount;                               // Mutable field
    private PaymentStatus status;
    
    public Result validate() {                          // CC: 3
        if (amount == null) return Error.invalid();        // +1
        if (amount.getValue().compareTo(BigDecimal.ZERO) <= 0) { // +1
            return Error.invalid();
        }
        return Result.success();                            // Base: +1
    }
    
    @Override
    public boolean equals(Object obj) { ... }           // CC: 1
    @Override  
    public int hashCode() { ... }                       // CC: 1
}
```

**Cross-Language Analysis Results:**
- **Kotlin Classes**: PaymentService (LCOM: 0, CC: 2.0) ✅ Excellent
- **Java Classes**: Payment (LCOM: 2, CC: 1.7) ✅ Excellent  
- **Architecture**: Service + Entity pattern across languages
- **DDD Patterns**: 1 Service (Kotlin), 1 Entity (Java), 1 Value Object (Kotlin)
- **Dependencies**: Java Entity ↔ Kotlin Service (cross-language)

## 🔍 Java-Specific Features

### Enhanced DDD Pattern Detection
- **JPA Annotations**: `@Entity`, `@Table`, `@Id` for entity detection
- **Spring Annotations**: `@Service`, `@Component`, `@Repository` for pattern recognition
- **Immutability Analysis**: `final` fields and setter detection for value objects
- **Enterprise Patterns**: Supports Java enterprise patterns and frameworks

### Java Code Quality Analysis
- **Field-Method Relationships**: Tracks field usage in methods via JavaParser AST
- **Access Modifier Analysis**: Considers `private`, `protected`, `public` in cohesion calculation
- **Exception Handling**: Analyzes `try-catch-finally` blocks for complexity
- **Control Flow**: Full support for Java control structures (`switch`, enhanced for-loops)

### Cross-Language Architecture
- **Unified Dependency Graphs**: Shows relationships between Kotlin and Java classes
- **Mixed Layer Analysis**: Detects layers regardless of implementation language
- **Framework Integration**: Supports Spring Boot projects with mixed Kotlin/Java code
- **Package Structure**: Analyzes layer patterns across both language ecosystems

## 🚀 Migration Guide

### From Kotlin-Only to Mixed Projects
1. **No Changes Required**: Existing Kotlin projects continue working identically
2. **Add Java Files**: Simply add `.java` files to your project
3. **Run Analysis**: Same command automatically detects both languages
4. **Unified Reports**: HTML reports show both languages with language indicators

### Performance Notes
- **Memory Efficient**: Both Kotlin PSI and JavaParser use streaming analysis
- **Incremental**: Only analyzes files that exist (graceful handling of empty language sets)
- **Scalable**: Tested on enterprise codebases with 100+ classes across languages

## License

This project is open source and available under the MIT License.