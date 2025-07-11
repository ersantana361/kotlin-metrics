# Sample Project - DDD Architecture Reference

This sample project demonstrates a well-structured Domain-Driven Design (DDD) implementation that showcases various architectural patterns detectable by the Kotlin Metrics Analyzer.

## Project Structure

```
src/main/kotlin/com/example/
├── domain/              # Domain Layer
│   ├── User.kt         # Entity
│   ├── Email.kt        # Value Object
│   ├── UserRepository.kt        # Repository Interface
│   └── UserCreatedEvent.kt     # Domain Event
├── application/        # Application Layer
│   └── UserService.kt  # Application Service
├── infrastructure/     # Infrastructure Layer
│   └── DatabaseUserRepository.kt  # Repository Implementation
└── presentation/       # Presentation Layer
    └── UserController.kt       # API Controller
```

## Architecture Analysis Results

When analyzed by the Kotlin Metrics tool, this project demonstrates:

### 🏗️ Architecture Pattern: CLEAN
- **Layers**: 5 layers detected (presentation, application, domain, infrastructure, unknown)
- **Dependencies**: 3 layer dependencies with 0 violations
- **Pattern Confidence**: High confidence for Clean Architecture

### 📐 DDD Patterns Detected
- **Entities**: 0 (User could be detected with more ID-focused implementation)
- **Value Objects**: 1 (Email)
- **Services**: 9 (including UserService and various response classes)
- **Repositories**: 2 (UserRepository interface and DatabaseUserRepository)
- **Aggregates**: 0 (simple domain model)
- **Domain Events**: 1 (UserCreatedEvent)

### 🌐 Dependency Graph
- **Nodes**: 10 classes/interfaces
- **Edges**: 15 dependencies
- **Cycles**: 1 (minor cycle in the dependency graph)
- **Packages**: 4 packages with good separation

### ⚠️ Quality Issues Detected
- **LCOM Issues**: UserRepository (LCOM: 21) and UserService (LCOM: 6)
- **Suggestions**: Interface segregation for repository, service method grouping

## Key DDD Patterns Demonstrated

### 1. Entity Pattern (User.kt)
```kotlin
data class User(
    val id: UUID,           // Unique identifier
    val email: String,
    val name: String,
    var isActive: Boolean   // Mutable state
) {
    fun activate() { ... }  // Domain behavior
    fun deactivate() { ... }
}
```

### 2. Value Object Pattern (Email.kt)
```kotlin
data class Email(val value: String) {
    init {
        require(value.contains("@")) { "Invalid email format" }
    }
    // Immutable, value-based equality
}
```

### 3. Repository Pattern (UserRepository.kt)
```kotlin
interface UserRepository {
    fun findById(id: UUID): User?
    fun save(user: User): User
    // ... other CRUD operations
}
```

### 4. Application Service Pattern (UserService.kt)
```kotlin
class UserService(private val userRepository: UserRepository) {
    fun createUser(email: String, name: String): User {
        // Orchestrates domain operations
        // Validates input, creates entity, persists
    }
}
```

### 5. Domain Event Pattern (UserCreatedEvent.kt)
```kotlin
data class UserCreatedEvent(
    val userId: UUID,
    val email: String,
    val name: String,
    val occurredAt: LocalDateTime = LocalDateTime.now()
)
```

## Layer Dependencies

The project follows proper dependency rules:

- **Presentation** → **Application** ✅
- **Application** → **Domain** ✅  
- **Infrastructure** → **Domain** ✅
- **Domain** → *No dependencies* ✅

## Testing the Analysis

To analyze this project:

```bash
cd test-project
java -jar ../build/libs/kotlin-metrics-all-1.0.0.jar
```

Expected output will show:
- Clean architecture pattern detected
- Multiple DDD patterns identified
- Layer dependency validation
- Quality metrics and suggestions

## Educational Value

This sample project serves as:

1. **Reference Implementation**: Shows how to structure DDD applications
2. **Testing Ground**: Validates the architecture analysis algorithms
3. **Documentation Example**: Demonstrates expected analysis results
4. **Best Practices**: Illustrates proper layer separation and dependency management

## Extension Ideas

To further test the analysis tool, consider adding:

- **Aggregates**: Multi-entity domain models
- **Domain Services**: Cross-entity business logic
- **Specifications**: Domain rule validation
- **Factories**: Complex object creation logic
- **Anti-Corruption Layers**: External system integration

This sample project provides a solid foundation for understanding both DDD principles and the architecture analysis capabilities of the Kotlin Metrics tool.