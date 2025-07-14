# Quality Scoring Guide

## Overview

The Kotlin & Java Metrics Analyzer implements a comprehensive quality scoring system that combines CK metrics, architecture analysis, and best practices to provide actionable quality assessments. This guide explains the methodology, calculations, and interpretation of quality scores.

## Quality Scoring Framework

### Multi-Dimensional Assessment

The quality scoring system evaluates software quality across five key dimensions:

1. **Cohesion Quality (20% weight)** - Based on LCOM
2. **Complexity Management (20% weight)** - Based on WMC and CC
3. **Coupling Design (25% weight)** - Based on CBO, RFC, CA, CE
4. **Inheritance Balance (15% weight)** - Based on DIT and NOC
5. **Overall Design (20% weight)** - Holistic assessment

### Score Scale

All scores use a **0-10 scale** with consistent interpretation:

- **8.0-10.0**: 🟢 **Excellent** - Exemplary design quality
- **6.0-7.9**: 🟡 **Good** - Solid design with minor improvements possible
- **4.0-5.9**: 🟠 **Fair** - Acceptable but refactoring recommended
- **0.0-3.9**: 🔴 **Poor** - Significant design issues requiring attention

## Detailed Scoring Algorithms

### 1. Cohesion Quality Score (LCOM-based)

**Purpose**: Measures how well class methods work together.

**Calculation**:
```kotlin
fun calculateCohesionScore(lcom: Int): Double {
    return when (lcom) {
        0 -> 10.0        // Perfect cohesion
        1 -> 8.5         // Excellent cohesion
        2 -> 7.0         // Good cohesion
        3 -> 5.5         // Fair cohesion
        4 -> 4.0         // Moderate cohesion
        5 -> 2.5         // Poor cohesion
        else -> max(0.0, 2.5 - (lcom - 5) * 0.3)  // Very poor, declining
    }
}
```

**Interpretation**:
```kotlin
class HighCohesionService {  // LCOM = 0 → Score: 10.0
    private val repository: Repository
    private val validator: Validator
    
    fun process(data: Data) {
        val validated = validator.validate(data)  // Uses validator
        repository.save(validated)               // Uses repository
    }
    
    fun update(data: Data) {
        val validated = validator.validate(data)  // Uses validator
        repository.update(validated)             // Uses repository  
    }
    // Both methods use both fields → Perfect cohesion
}

class LowCohesionClass {  // LCOM = 6 → Score: 1.9
    private val dbConnection: Connection
    private val logger: Logger
    private val cache: Cache
    private val emailService: EmailService
    
    fun saveData() { /* uses only dbConnection */ }
    fun logMessage() { /* uses only logger */ }
    fun clearCache() { /* uses only cache */ }
    fun sendEmail() { /* uses only emailService */ }
    // Methods don't share fields → Poor cohesion
}
```

### 2. Complexity Management Score (WMC/CC-based)

**Purpose**: Evaluates how well complexity is managed within classes.

**Calculation**:
```kotlin
fun calculateComplexityScore(wmc: Int, avgCC: Double): Double {
    val wmcScore = when {
        wmc <= 10 -> 10.0
        wmc <= 20 -> 8.0 - (wmc - 10) * 0.2      // 8.0 to 6.0
        wmc <= 50 -> 6.0 - (wmc - 20) * 0.1      // 6.0 to 3.0
        else -> max(0.0, 3.0 - (wmc - 50) * 0.06) // 3.0 declining
    }
    
    val ccScore = when {
        avgCC <= 2.0 -> 10.0
        avgCC <= 5.0 -> 10.0 - (avgCC - 2.0) * 0.67  // 10.0 to 8.0
        avgCC <= 10.0 -> 8.0 - (avgCC - 5.0) * 0.8   // 8.0 to 4.0
        avgCC <= 20.0 -> 4.0 - (avgCC - 10.0) * 0.3  // 4.0 to 1.0
        else -> max(0.0, 1.0 - (avgCC - 20.0) * 0.05)
    }
    
    return (wmcScore + ccScore) / 2.0
}
```

**Interpretation**:
```kotlin
class SimpleCalculator {  // WMC = 8, Avg CC = 1.5 → Score: 10.0
    fun add(a: Int, b: Int) = a + b          // CC = 1
    fun subtract(a: Int, b: Int) = a - b     // CC = 1
    fun multiply(a: Int, b: Int) = a * b     // CC = 1
    fun divide(a: Int, b: Int) = a / b       // CC = 1
    // Simple methods, low total complexity
}

class ComplexBusinessLogic {  // WMC = 45, Avg CC = 12.3 → Score: 2.1
    fun processOrder(order: Order): Result { /* CC = 15 */ }
    fun validatePayment(payment: Payment): Boolean { /* CC = 18 */ }
    fun calculateShipping(address: Address): Money { /* CC = 12 */ }
    // High complexity methods requiring refactoring
}
```

### 3. Coupling Design Score (CBO/RFC/CA/CE-based)

**Purpose**: Assesses the quality of class dependencies and interfaces.

**Calculation**:
```kotlin
fun calculateCouplingScore(cbo: Int, rfc: Int, ca: Int, ce: Int): Double {
    val cboScore = when {
        cbo <= 5 -> 10.0
        cbo <= 15 -> 10.0 - (cbo - 5) * 0.5      // 10.0 to 5.0
        cbo <= 25 -> 5.0 - (cbo - 15) * 0.3      // 5.0 to 2.0
        else -> max(0.0, 2.0 - (cbo - 25) * 0.1)
    }
    
    val rfcScore = when {
        rfc <= 20 -> 10.0
        rfc <= 50 -> 10.0 - (rfc - 20) * 0.2     // 10.0 to 4.0
        rfc <= 100 -> 4.0 - (rfc - 50) * 0.06    // 4.0 to 1.0
        else -> max(0.0, 1.0 - (rfc - 100) * 0.01)
    }
    
    // Stability metrics (CA/CE) - balanced is good
    val stability = if (ca + ce == 0) 10.0 else {
        val instability = ce.toDouble() / (ca + ce)
        when {
            instability in 0.2..0.8 -> 10.0      // Balanced
            instability < 0.2 || instability > 0.8 -> 7.0  // Too stable/unstable
            else -> 5.0
        }
    }
    
    return (cboScore * 0.4 + rfcScore * 0.4 + stability * 0.2)
}
```

**Interpretation Examples**:
```kotlin
class WellCoupledService {  // CBO=4, RFC=12, CA=6, CE=3 → Score: 9.2
    private val repository: Repository          // +1 CBO
    private val validator: Validator            // +1 CBO
    private val mapper: Mapper                  // +1 CBO
    private val logger: Logger                  // +1 CBO
    
    // 8 local methods + 4 remote calls = RFC 12
    // Well balanced coupling
}

class HighlyCoupledService {  // CBO=22, RFC=85, CA=2, CE=20 → Score: 1.4
    // 22 different class dependencies
    // 85 method calls (local + remote)
    // High efferent coupling, low afferent coupling
    // Indicates "feature envy" anti-pattern
}
```

### 4. Inheritance Balance Score (DIT/NOC-based)

**Purpose**: Evaluates inheritance hierarchy design quality.

**Calculation**:
```kotlin
fun calculateInheritanceScore(dit: Int, noc: Int): Double {
    val ditScore = when {
        dit <= 2 -> 10.0
        dit <= 5 -> 10.0 - (dit - 2) * 1.5       // 10.0 to 5.5
        dit <= 8 -> 5.5 - (dit - 5) * 1.0        // 5.5 to 2.5
        else -> max(0.0, 2.5 - (dit - 8) * 0.5)
    }
    
    val nocScore = when {
        noc <= 5 -> 10.0
        noc <= 10 -> 10.0 - (noc - 5) * 0.6      // 10.0 to 7.0
        noc <= 20 -> 7.0 - (noc - 10) * 0.3      // 7.0 to 4.0
        else -> max(0.0, 4.0 - (noc - 20) * 0.2)
    }
    
    return (ditScore + nocScore) / 2.0
}
```

**Interpretation Examples**:
```kotlin
// Good inheritance hierarchy
abstract class Shape         // DIT=0, NOC=3 → Score: 10.0
class Circle : Shape         // DIT=1, NOC=0 → Score: 10.0
class Rectangle : Shape      // DIT=1, NOC=2 → Score: 10.0
class Square : Rectangle     // DIT=2, NOC=0 → Score: 10.0

// Problematic deep hierarchy  
class VerySpecific : Specific : General : Base : Root  // DIT=4 → Score: 4.0

// Overly broad abstraction
abstract class Component {   // NOC=25 → Score: 3.0
    // 25 direct subclasses - too broad
}
```

### 5. Overall Design Score

**Purpose**: Holistic assessment considering cross-metric relationships and design patterns.

**Calculation**:
```kotlin
fun calculateOverallDesignScore(
    cohesionScore: Double,
    complexityScore: Double, 
    couplingScore: Double,
    inheritanceScore: Double,
    ckMetrics: CkMetrics
): Double {
    val baseScore = (cohesionScore + complexityScore + couplingScore + inheritanceScore) / 4.0
    
    // Apply design pattern bonuses/penalties
    var designScore = baseScore
    
    // Bonus for good design patterns
    if (isWellDesignedEntity(ckMetrics)) designScore += 0.5
    if (isWellDesignedService(ckMetrics)) designScore += 0.5
    if (isWellDesignedValueObject(ckMetrics)) designScore += 0.5
    
    // Penalty for anti-patterns
    if (isGodClass(ckMetrics)) designScore -= 2.0
    if (isFeatureEnvy(ckMetrics)) designScore -= 1.0
    if (isDataClass(ckMetrics)) designScore -= 0.5
    
    return max(0.0, min(10.0, designScore))
}

fun isGodClass(metrics: CkMetrics): Boolean {
    return metrics.wmc > 50 && metrics.cbo > 20 && metrics.rfc > 80
}

fun isFeatureEnvy(metrics: CkMetrics): Boolean {
    return metrics.cbo > 15 && metrics.lcom > 8
}
```

## Composite Quality Score

### Final Quality Calculation

```kotlin
fun calculateFinalQualityScore(
    cohesionScore: Double,      // 20% weight
    complexityScore: Double,    // 20% weight  
    couplingScore: Double,      // 25% weight
    inheritanceScore: Double,   // 15% weight
    overallScore: Double        // 20% weight
): Double {
    return cohesionScore * 0.20 +
           complexityScore * 0.20 +
           couplingScore * 0.25 +
           inheritanceScore * 0.15 +
           overallScore * 0.20
}
```

### Quality Examples

#### Excellent Quality Example (Score: 9.2)
```kotlin
@Service
class UserService(                    // Cohesion: 10.0 (LCOM = 0)
    private val userRepository: UserRepository,  // Complexity: 9.5 (WMC = 12, CC = 2.1)
    private val emailService: EmailService      // Coupling: 9.0 (CBO = 2, RFC = 15)
) {                                   // Inheritance: 10.0 (DIT = 0, NOC = 0)
    fun createUser(request: CreateUserRequest): User {  // Overall: 8.5
        val user = User(request.email, request.name)
        val saved = userRepository.save(user)
        emailService.sendWelcomeEmail(saved)
        return saved
    }
    
    fun updateUser(userId: UUID, request: UpdateUserRequest): User {
        val user = userRepository.findById(userId)
        user.update(request.name, request.email)
        return userRepository.save(user)
    }
}
```

#### Poor Quality Example (Score: 2.8)
```kotlin
class DataManager {                   // Cohesion: 1.5 (LCOM = 8)
    private val dbConnection: Connection      // Complexity: 2.0 (WMC = 65, CC = 15.2)
    private val fileSystem: FileSystem       // Coupling: 1.8 (CBO = 25, RFC = 95)
    private val cache: Cache                  // Inheritance: 7.0 (DIT = 1, NOC = 0)
    private val logger: Logger                // Overall: 1.5 (God class penalty)
    private val validator: Validator
    private val emailService: EmailService
    private val reportGenerator: ReportGenerator
    private val auditService: AuditService
    
    // 15+ methods doing unrelated things
    fun saveUserData(userData: UserData) { /* complex logic */ }
    fun generateReport(type: ReportType) { /* complex logic */ }
    fun sendEmail(email: Email) { /* complex logic */ }
    fun validateInput(input: Input) { /* complex logic */ }
    // ... many more unrelated methods
}
```

## Risk Assessment Integration

### Risk Classification

Quality scores map to risk levels for prioritization:

```kotlin
fun calculateRiskLevel(qualityScore: Double): RiskLevel {
    return when {
        qualityScore >= 8.0 -> RiskLevel.LOW       // 🟢 Low Risk
        qualityScore >= 6.0 -> RiskLevel.MEDIUM    // 🟡 Medium Risk  
        qualityScore >= 4.0 -> RiskLevel.HIGH      // 🟠 High Risk
        else -> RiskLevel.CRITICAL                 // 🔴 Critical Risk
    }
}

enum class RiskLevel(val priority: Int, val description: String) {
    LOW(1, "Minimal maintenance risk, exemplary design"),
    MEDIUM(2, "Some improvement opportunities, generally solid"),
    HIGH(3, "Refactoring recommended, design issues present"),
    CRITICAL(4, "Immediate attention required, significant issues")
}
```

### Risk Impact Analysis

```kotlin
data class RiskAssessment(
    val riskLevel: RiskLevel,
    val primaryConcerns: List<String>,
    val impactAreas: List<String>,
    val recommendedActions: List<String>,
    val effort: EstimatedEffort
)

fun generateRiskAssessment(qualityScore: Double, ckMetrics: CkMetrics): RiskAssessment {
    val concerns = mutableListOf<String>()
    val impacts = mutableListOf<String>()
    val actions = mutableListOf<String>()
    
    if (ckMetrics.lcom > 5) {
        concerns.add("Poor class cohesion")
        impacts.add("Difficult to understand and maintain")
        actions.add("Extract related methods into separate classes")
    }
    
    if (ckMetrics.wmc > 50) {
        concerns.add("Excessive class complexity")
        impacts.add("High testing effort, error-prone changes")
        actions.add("Break down complex methods, apply SRP")
    }
    
    if (ckMetrics.cbo > 20) {
        concerns.add("High coupling to other classes")
        impacts.add("Changes cascade through system")
        actions.add("Apply dependency injection, introduce interfaces")
    }
    
    return RiskAssessment(
        riskLevel = calculateRiskLevel(qualityScore),
        primaryConcerns = concerns,
        impactAreas = impacts,
        recommendedActions = actions,
        effort = estimateRefactoringEffort(ckMetrics)
    )
}
```

## Correlation Analysis

### Metric Relationships

The tool analyzes relationships between metrics:

```kotlin
data class CorrelationAnalysis(
    val lcomWmcCorrelation: Double,    // High LCOM often correlates with high WMC
    val cboRfcCorrelation: Double,     // CBO and RFC usually correlate strongly
    val ditNocCorrelation: Double,     // DIT and NOC may correlate in deep hierarchies
    val qualityPatterns: List<QualityPattern>
)

enum class QualityPattern {
    GOD_CLASS,           // High WMC + High CBO + High LCOM
    FEATURE_ENVY,        // High CBO + High RFC + Low CA
    DATA_CLASS,          // Low WMC + High CA + Low LCOM
    DEAD_CODE,           // Low CA + Low CE + Low RFC
    PERFECTIONIST,       // Very low all metrics (over-engineered)
    BALANCED_DESIGN      // All metrics in good ranges
}
```

### Quality Trends

```kotlin
data class QualityTrend(
    val improvementOpportunities: List<String>,
    val strengthAreas: List<String>,
    val prioritizedRefactoring: List<RefactoringTask>
)

data class RefactoringTask(
    val className: String,
    val priority: Priority,
    val estimatedEffort: EstimatedEffort,
    val expectedImpact: Double,    // Quality score improvement
    val specificActions: List<String>
)
```

## Best Practices for Quality Improvement

### High-Impact Improvements

1. **Address LCOM > 5**: Extract classes with single responsibilities
2. **Reduce WMC > 30**: Break down complex methods
3. **Lower CBO > 15**: Apply dependency injection and interfaces
4. **Limit DIT > 5**: Favor composition over deep inheritance

### Systematic Improvement Approach

1. **Start with Critical Risk** classes (Score < 4.0)
2. **Focus on High LCOM** (easiest to fix, high impact)
3. **Tackle High WMC** (improves testability significantly)
4. **Address Coupling Issues** (improves system flexibility)

### Quality Gates

Recommended thresholds for continuous integration:

```yaml
quality_gates:
  minimum_quality_score: 6.0
  maximum_critical_risk_classes: 0
  maximum_high_risk_percentage: 10%
  
  ck_metrics_limits:
    lcom_max: 5
    wmc_max: 30
    cbo_max: 15
    rfc_max: 50
    dit_max: 5
    noc_max: 10
```

This comprehensive quality scoring system provides objective, actionable insights for maintaining and improving software quality over time.