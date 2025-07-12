# Complete CK Metrics Guide

## Overview

The Chidamber and Kemerer (CK) metrics suite is a comprehensive set of object-oriented software metrics designed to measure software quality, maintainability, and design complexity. This guide provides detailed explanations of all 9 metrics implemented in the Kotlin & Java Metrics Analyzer.

## The Complete CK Metrics Suite

### 1. LCOM (Lack of Cohesion of Methods)

**Purpose**: Measures how well the methods of a class are related through shared properties.

**Calculation**:
```
LCOM = P - Q (minimum 0)
Where:
- P = Number of method pairs with no shared properties
- Q = Number of method pairs with shared properties
```

**Interpretation**:
- **LCOM = 0**: Perfect cohesion - all methods share properties
- **LCOM 1-2**: Good cohesion - minor improvements possible
- **LCOM 3-5**: Moderate cohesion - consider refactoring
- **LCOM > 5**: Poor cohesion - strong candidate for class decomposition

**Example**:
```kotlin
class UserService {  // LCOM = 0 (Good)
    private val userRepository: UserRepository
    private val emailService: EmailService
    
    fun createUser(data: UserData) {
        // Uses both userRepository and emailService
    }
    
    fun updateUser(user: User) {
        // Uses both userRepository and emailService
    }
}

class MixedResponsibilities {  // LCOM = 4 (Poor)
    private val database: Database
    private val logger: Logger
    private val cache: Cache
    
    fun saveData() { /* uses database */ }
    fun logMessage() { /* uses logger */ }  
    fun clearCache() { /* uses cache */ }
}
```

### 2. WMC (Weighted Methods per Class)

**Purpose**: Measures the complexity of a class by summing the cyclomatic complexities of all its methods.

**Calculation**:
```
WMC = Σ(Cyclomatic Complexity of all methods)
```

**Interpretation**:
- **WMC 1-10**: Simple class - easy to understand and maintain
- **WMC 11-20**: Moderate complexity - acceptable for most purposes
- **WMC 21-50**: Complex class - consider decomposition
- **WMC > 50**: Very complex - critical refactoring needed

**Example**:
```kotlin
class SimpleCalculator {  // WMC = 4
    fun add(a: Int, b: Int) = a + b           // CC = 1
    fun subtract(a: Int, b: Int) = a - b      // CC = 1
    fun multiply(a: Int, b: Int) = a * b      // CC = 1
    fun divide(a: Int, b: Int): Double {      // CC = 1
        return a.toDouble() / b
    }
}

class ComplexProcessor {  // WMC = 25+
    fun processData(data: List<String>): Result {  // CC = 8
        if (data.isEmpty()) return Result.empty()
        
        val processed = data.mapNotNull { item ->
            when {
                item.startsWith("prefix") -> processPrefix(item)
                item.contains("pattern") -> processPattern(item)
                item.endsWith("suffix") -> processSuffix(item)
                else -> null
            }
        }
        
        return if (processed.isNotEmpty()) {
            Result.success(processed)
        } else {
            Result.failure("No valid items")
        }
    }
    // ... more complex methods
}
```

### 3. DIT (Depth of Inheritance Tree)

**Purpose**: Measures the depth of inheritance hierarchy from the class to the root.

**Calculation**:
```
DIT = Maximum number of classes from the current class to the root of the hierarchy
```

**Interpretation**:
- **DIT 0-2**: Shallow hierarchy - good for understanding and reuse
- **DIT 3-5**: Moderate depth - acceptable complexity
- **DIT 6+**: Deep hierarchy - potential maintenance and testing issues

**Example**:
```kotlin
open class Vehicle          // DIT = 0
open class Car : Vehicle    // DIT = 1  
open class SportsCar : Car  // DIT = 2
class Ferrari : SportsCar   // DIT = 3 (Acceptable)

// Problematic deep hierarchy:
class VerySpecificFerrariModel : 
    SpecificFerrariSeries : 
    FerrariSubModel : 
    FerrariModel : 
    Ferrari : 
    SportsCar : 
    Car : 
    Vehicle     // DIT = 7 (Too deep!)
```

### 4. NOC (Number of Children)

**Purpose**: Counts the number of direct subclasses.

**Calculation**:
```
NOC = Count of classes that directly inherit from this class
```

**Interpretation**:
- **NOC 0-5**: Appropriate abstraction level
- **NOC 6-10**: Consider if the abstraction is too broad
- **NOC > 10**: Overly broad abstraction - refactor hierarchy

**Example**:
```kotlin
abstract class Shape {     // NOC = 3 (Good)
    abstract fun area(): Double
}

class Circle : Shape() { ... }
class Rectangle : Shape() { ... }  
class Triangle : Shape() { ... }

abstract class UIComponent {  // NOC = 15+ (Too many!)
    // Too many direct subclasses indicates
    // the abstraction might be too broad
}
```

### 5. CBO (Coupling Between Objects)

**Purpose**: Measures the number of classes this class is coupled to.

**Calculation**:
```
CBO = Count of unique classes this class depends on or uses
```

**Interpretation**:
- **CBO 0-5**: Low coupling - good design
- **CBO 6-15**: Moderate coupling - acceptable
- **CBO 16+**: High coupling - reduce dependencies

**Example**:
```kotlin
class OrderService {  // CBO = 4 (Good)
    private val orderRepository: OrderRepository      // +1
    private val paymentService: PaymentService        // +1  
    private val inventoryService: InventoryService    // +1
    private val emailService: EmailService           // +1
    
    fun processOrder(order: Order) { ... }
}

class HighlyCoupledService {  // CBO = 20+ (Poor)
    // Dependencies on many different classes
    private val serviceA: ServiceA
    private val serviceB: ServiceB
    // ... 18+ more dependencies
    // This class is doing too much!
}
```

### 6. RFC (Response For a Class)

**Purpose**: Measures the number of methods that can be invoked in response to a message.

**Calculation**:
```
RFC = Number of local methods + Number of remote methods called
```

**Interpretation**:
- **RFC 1-20**: Simple interface - easy to use and understand
- **RFC 21-50**: Moderate interface complexity
- **RFC > 50**: Complex interface - consider decomposition

**Example**:
```kotlin
class UserController {  // RFC = 8 (Good)
    // Local methods (4)
    fun createUser() { ... }
    fun updateUser() { ... }
    fun deleteUser() { ... }
    fun getUser() { ... }
    
    // Remote methods called (4)
    // userService.save(), userService.find()
    // emailService.send(), auditService.log()
}

class OverloadedController {  // RFC = 60+ (Poor)
    // Too many methods and external calls
    // This indicates the class has too many responsibilities
}
```

### 7. CA (Afferent Coupling)

**Purpose**: Measures the number of classes that depend on this class.

**Calculation**:
```
CA = Count of classes that use/depend on this class
```

**Interpretation**:
- **High CA**: Stable component - many classes depend on it
- **Low CA**: Less stable - fewer dependencies
- **Monitor CA**: High CA classes require careful change management

**Example**:
```kotlin
class Logger {  // High CA (e.g., CA = 25)
    // Many classes across the system use Logger
    // This makes Logger a stable, core component
    // Changes to Logger affect many classes
}

class SpecificUtility {  // Low CA (e.g., CA = 2)
    // Only used by a few classes
    // Changes have limited impact
}
```

### 8. CE (Efferent Coupling)

**Purpose**: Measures the number of classes this class depends on.

**Calculation**:
```
CE = Count of classes this class depends on
```

**Interpretation**:
- **High CE**: Unstable component - depends on many others
- **Low CE**: More stable - fewer external dependencies
- **Monitor CE**: High CE classes are sensitive to external changes

**Example**:
```kotlin
class CoreEntity {  // Low CE (e.g., CE = 1)
    // Minimal dependencies - very stable
    private val id: UUID
}

class IntegrationService {  // High CE (e.g., CE = 15)
    // Depends on many external services and APIs
    // Changes in any dependency can break this class
}
```

### 9. Cyclomatic Complexity (CC)

**Purpose**: Measures the complexity of control flow within methods.

**Calculation**:
```
CC = Number of decision points + 1
Decision points: if, else, while, for, case, catch, &&, ||, ?:
```

**Interpretation**:
- **CC 1-5**: Simple - easy to understand and test
- **CC 6-10**: Moderate - acceptable complexity
- **CC 11-20**: Complex - consider refactoring
- **CC 21+**: Very complex - critical refactoring needed

**Example**:
```kotlin
fun simpleValidation(user: User): Boolean {  // CC = 2
    return if (user.email.isNotEmpty()) {        // +1
        user.age >= 18                           // Base = 1
    } else {
        false
    }
}

fun complexBusinessLogic(data: Data): Result {  // CC = 8
    if (data.isEmpty()) return Result.empty()      // +1
    
    val result = when (data.type) {                 // +3 (3 branches)
        Type.A -> processTypeA(data)
        Type.B -> processTypeB(data)  
        Type.C -> processTypeC(data)
    }
    
    return if (result.isValid() && result.hasContent()) {  // +2 (&&)
        Result.success(result)
    } else {                                              // +1
        Result.failure("Invalid result")
    }                                                     // Base = 1
}
```

## Quality Scoring Algorithm

The tool combines all CK metrics into a comprehensive quality score:

```kotlin
Quality Score (0-10) = Weighted Average of:
- Cohesion Score (20%): Based on LCOM
- Complexity Score (20%): Based on WMC and CC  
- Coupling Score (25%): Based on CBO, RFC, CA, CE
- Inheritance Score (15%): Based on DIT and NOC
- Overall Design Score (20%): Holistic assessment
```

### Score Interpretation:
- **8.0-10**: Excellent - exemplary design quality
- **6.0-7.9**: Good - solid design with minor improvements possible
- **4.0-5.9**: Fair - acceptable but refactoring recommended
- **0-3.9**: Poor - significant design issues requiring attention

## Best Practices by Metric

### LCOM Optimization
- **Group related methods**: Methods that work with the same data should be together
- **Split large classes**: High LCOM often indicates multiple responsibilities
- **Use composition**: Break complex classes into smaller, focused components

### WMC Management
- **Limit method complexity**: Keep individual methods simple
- **Extract methods**: Break complex methods into smaller ones
- **Consider class size**: Large WMC might indicate too many responsibilities

### DIT Balance
- **Favor composition over inheritance**: Deep hierarchies are hard to maintain
- **Limit inheritance depth**: Keep hierarchies shallow and focused
- **Use interfaces**: Abstract contracts instead of concrete inheritance

### NOC Control
- **Review broad abstractions**: Many children might indicate over-generalization
- **Consider alternative patterns**: Strategy, Factory, or other patterns
- **Split large hierarchies**: Break broad abstractions into focused ones

### Coupling Reduction
- **Dependency injection**: Use DI to manage dependencies
- **Interface segregation**: Depend on abstractions, not concretions
- **Service locator pattern**: Centralize dependency management

### RFC Simplification
- **Single responsibility**: Each class should have one clear purpose
- **Facade pattern**: Hide complex subsystem interactions
- **Method extraction**: Break large methods with many calls

## Tool-Specific Features

### Cross-Language Analysis
The tool provides unified CK metrics across Kotlin and Java:

```kotlin
// Kotlin class with Spring annotations
@Service
class KotlinUserService @Autowired constructor(
    private val userRepository: UserRepository  // CBO +1
) {
    fun processUser(user: User): Result { ... } // RFC +1, WMC +CC
}

// Java class calling Kotlin service
@Controller
public class JavaUserController {
    @Autowired
    private KotlinUserService userService;  // CBO +1 (cross-language)
    
    public ResponseEntity<User> getUser(Long id) {
        return userService.processUser(...);   // RFC +1 (remote call)
    }
}
```

### Framework Integration
- **Spring Framework**: Recognizes @Service, @Component, @Repository annotations
- **JPA/Hibernate**: Identifies @Entity, @Table for architectural analysis
- **Kotlin Features**: Handles data classes, extension functions, coroutines

### Correlation Analysis
The tool identifies relationships between metrics:
- **High WMC + High CBO**: Often indicates "God classes"
- **High DIT + High NOC**: Suggests complex inheritance patterns
- **High RFC + High CA**: Indicates central system components

## Actionable Recommendations

### For High LCOM:
1. Identify method groups that don't share properties
2. Extract unrelated methods into separate classes
3. Consider if the class has multiple responsibilities

### For High WMC:
1. Break complex methods into smaller ones
2. Extract helper classes for complex operations
3. Consider if the class is doing too much

### For High Coupling (CBO/RFC):
1. Use dependency injection
2. Introduce interfaces to reduce concrete dependencies
3. Apply facade or adapter patterns

### For Deep Inheritance (DIT):
1. Favor composition over inheritance
2. Use interfaces for contracts
3. Consider flattening the hierarchy

This comprehensive approach to CK metrics helps identify design issues early and guides refactoring efforts for better software quality.