# Architecture Analysis Guide

## Overview

The Kotlin & Java Metrics Analyzer provides comprehensive architectural analysis capabilities, including DDD pattern detection, layered architecture analysis, and dependency graph visualization. This guide explains how the tool analyzes and reports on software architecture patterns.

## DDD Pattern Detection

### Domain-Driven Design Patterns

The tool automatically detects common DDD patterns with confidence scoring:

#### 1. Entities

**Detection Criteria**:
- Has unique identifier fields (id, uuid, key)
- Contains mutable state
- Implements equals/hashCode methods
- Follows entity naming conventions

**Kotlin Detection**:
```kotlin
@Entity
data class User(           // Confidence: 95%
    @Id val id: UUID,      // ✅ Unique ID
    var email: String,     // ✅ Mutable state
    var name: String       // ✅ Mutable state
) {
    // ✅ Data class provides equals/hashCode
}

class Order {             // Confidence: 80%
    val id: OrderId       // ✅ Unique ID (custom type)
    var status: OrderStatus = PENDING  // ✅ Mutable
    private val items = mutableListOf<OrderItem>()
    
    override fun equals(other: Any?): Boolean { ... }  // ✅ Custom equals
    override fun hashCode(): Int { ... }               // ✅ Custom hashCode
}
```

**Java Detection**:
```java
@Entity
@Table(name = "users")
public class User {        // Confidence: 95%
    @Id 
    private UUID id;       // ✅ Unique ID with @Id
    private String email;  // ✅ Mutable (has setter)
    private String name;
    
    // ✅ Getters and setters indicate mutability
    public void setEmail(String email) { this.email = email; }
    
    @Override
    public boolean equals(Object obj) { ... }  // ✅ Equals method
    @Override  
    public int hashCode() { ... }              // ✅ HashCode method
}
```

#### 2. Value Objects

**Detection Criteria**:
- Immutable fields/properties
- Value-based equality
- No unique identifier
- Often small and focused

**Kotlin Detection**:
```kotlin
data class Money(          // Confidence: 90%
    val amount: BigDecimal,  // ✅ Immutable
    val currency: String     // ✅ Immutable
) {
    // ✅ Data class provides value equality
    init {
        require(amount >= BigDecimal.ZERO) { "Amount must be positive" }
    }
}

class Address(             // Confidence: 75%
    val street: String,    // ✅ All val (immutable)
    val city: String,      // ✅ All val (immutable)
    val country: String    // ✅ All val (immutable)
) {
    override fun equals(other: Any?): Boolean { ... }  // ✅ Value equality
    override fun hashCode(): Int { ... }
}
```

**Java Detection**:
```java
public final class Money {     // Confidence: 85%
    private final BigDecimal amount;  // ✅ Final fields
    private final String currency;   // ✅ Final fields
    
    public Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }
    
    // ✅ No setters (immutable)
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    
    @Override
    public boolean equals(Object obj) { ... }  // ✅ Value equality
}
```

#### 3. Services

**Detection Criteria**:
- Stateless operations
- Business logic methods
- Dependencies on repositories/other services
- Service naming conventions

**Kotlin Detection**:
```kotlin
@Service
class PaymentService(      // Confidence: 90%
    private val paymentRepository: PaymentRepository,  // ✅ Repository dependency
    private val emailService: EmailService            // ✅ Service dependency
) {
    // ✅ Stateless (no mutable state)
    // ✅ Business logic methods
    fun processPayment(request: PaymentRequest): PaymentResult {
        val payment = Payment.create(request)
        val result = paymentRepository.save(payment)
        emailService.sendConfirmation(payment.userEmail)
        return PaymentResult.success(result)
    }
    
    fun refundPayment(paymentId: PaymentId): RefundResult { ... }
}
```

**Java Detection**:
```java
@Service
@Transactional
public class UserService {     // Confidence: 95%
    @Autowired
    private UserRepository userRepository;     // ✅ Repository dependency
    
    @Autowired  
    private EmailService emailService;        // ✅ Service dependency
    
    // ✅ No instance fields (stateless)
    // ✅ Business logic methods
    public User createUser(CreateUserRequest request) {
        User user = new User(request.getEmail(), request.getName());
        User saved = userRepository.save(user);
        emailService.sendWelcomeEmail(saved);
        return saved;
    }
}
```

#### 4. Repositories

**Detection Criteria**:
- Data access patterns
- CRUD method names
- Repository/DAO naming
- Framework annotations

**Kotlin Detection**:
```kotlin
interface UserRepository : JpaRepository<User, UUID> {  // Confidence: 95%
    // ✅ Extends JPA repository
    // ✅ Repository naming
    fun findByEmail(email: String): User?
    fun findByStatus(status: UserStatus): List<User>
}

@Repository
class CustomOrderRepository(     // Confidence: 85%
    private val entityManager: EntityManager
) {
    // ✅ @Repository annotation
    // ✅ Data access methods
    fun save(order: Order): Order { ... }
    fun findById(id: OrderId): Order? { ... }
    fun findByUserId(userId: UUID): List<Order> { ... }
}
```

**Java Detection**:
```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Confidence: 95%
    // ✅ @Repository + JPA interface
    Optional<User> findByEmail(String email);
    List<User> findByStatusAndCreatedAfter(UserStatus status, LocalDateTime date);
}

@Repository
@Transactional
public class OrderDAOImpl implements OrderDAO {  // Confidence: 90%
    @PersistenceContext
    private EntityManager em;  // ✅ JPA EntityManager
    
    @Override
    public Order save(Order order) { ... }        // ✅ CRUD methods
    @Override
    public Optional<Order> findById(Long id) { ... }
}
```

#### 5. Aggregates

**Detection Criteria**:
- High-confidence entities
- References to other entities
- Complex business operations
- Aggregate naming patterns

**Kotlin Detection**:
```kotlin
class ShoppingCart(         // Confidence: 70%
    val id: CartId,         // ✅ Entity-like (has ID)
    val userId: UserId      // ✅ References other entities
) {
    private val items = mutableListOf<CartItem>()  // ✅ Contains other entities
    
    // ✅ Complex business operations
    fun addItem(productId: ProductId, quantity: Int): CartItemAddResult {
        val existingItem = items.find { it.productId == productId }
        return if (existingItem != null) {
            existingItem.increaseQuantity(quantity)
            CartItemAddResult.Updated(existingItem)
        } else {
            val newItem = CartItem(productId, quantity)
            items.add(newItem)
            CartItemAddResult.Added(newItem)
        }
    }
    
    fun calculateTotal(): Money { ... }
    fun clear() { items.clear() }
}
```

#### 6. Domain Events

**Detection Criteria**:
- Event naming patterns
- Immutable data structures
- Timestamp fields
- Event-specific annotations

**Kotlin Detection**:
```kotlin
data class UserRegisteredEvent(     // Confidence: 85%
    val userId: UUID,               // ✅ Event data
    val email: String,              // ✅ Immutable (data class)
    val registeredAt: Instant      // ✅ Timestamp
) {
    // ✅ Event naming pattern
    // ✅ Immutable structure
}

@DomainEvent
data class OrderCompletedEvent(     // Confidence: 95%
    val orderId: OrderId,           // ✅ Domain event annotation
    val userId: UUID,               // ✅ Event data
    val totalAmount: Money,         // ✅ Value objects
    val completedAt: Instant = Instant.now()  // ✅ Timestamp
)
```

## Layered Architecture Analysis

### Layer Detection

The tool automatically identifies architectural layers based on:

#### 1. Package Structure Analysis
```
com.example.ecommerce.
├── presentation/          → Presentation Layer
│   ├── controller/
│   ├── dto/
│   └── api/
├── application/           → Application Layer  
│   ├── service/
│   ├── usecase/
│   └── command/
├── domain/               → Domain Layer
│   ├── model/
│   ├── repository/
│   └── service/
└── infrastructure/       → Infrastructure Layer
    ├── persistence/
    ├── messaging/
    └── external/
```

#### 2. Class Naming Conventions
- **Controllers**: `*Controller`, `*RestController`, `*Endpoint`
- **Services**: `*Service`, `*UseCase`, `*Handler`
- **Repositories**: `*Repository`, `*DAO`, `*Store`
- **Entities**: `*Entity`, `*Model`, `*Domain`

#### 3. Framework Annotations
```kotlin
// Presentation Layer
@RestController
@Controller
@RequestMapping

// Application Layer  
@Service
@Component
@UseCase

// Infrastructure Layer
@Repository
@Configuration
@Entity (JPA)
```

### Architecture Pattern Detection

#### Layered Architecture
```
Presentation Layer (Controllers, DTOs)
        ↓
Application Layer (Services, Use Cases)
        ↓  
Domain Layer (Entities, Domain Services)
        ↓
Infrastructure Layer (Repositories, External APIs)
```

**Detection Criteria**:
- Clear layer separation
- Downward dependencies only
- No layer skipping

#### Hexagonal Architecture (Ports & Adapters)
```
    External APIs → Adapters → Application Core ← Adapters ← Database
                                   ↓
                              Domain Logic
```

**Detection Criteria**:
- Port interfaces in domain
- Adapter implementations in infrastructure
- Dependency inversion patterns

#### Clean Architecture
```
External → Interface Adapters → Application Business Rules → Enterprise Business Rules
```

**Detection Criteria**:
- Dependency inversion
- Framework independence
- Clear boundaries between layers

#### Onion Architecture
```
Infrastructure → Application Services → Domain Services → Domain Model
```

**Detection Criteria**:
- Concentric layer dependencies
- Domain at the center
- All dependencies point inward

### Layer Violation Detection

#### Common Violations

**1. Layer Skipping**:
```kotlin
@RestController
class UserController(
    private val userRepository: UserRepository  // ❌ Controller directly accessing Repository
) {
    // Should use UserService instead
}
```

**2. Reverse Dependencies**:
```kotlin
// Domain layer
class User(
    private val userController: UserController  // ❌ Domain depending on Presentation
) {
    // Domain should not know about controllers
}
```

**3. Circular Dependencies**:
```kotlin
class ServiceA(private val serviceB: ServiceB)
class ServiceB(private val serviceA: ServiceA)  // ❌ Circular dependency
```

## Dependency Graph Analysis

### Graph Construction

The tool builds a comprehensive dependency graph including:

#### 1. Class-Level Dependencies
```kotlin
class OrderService(
    private val orderRepository: OrderRepository,    // Dependency edge
    private val paymentService: PaymentService,      // Dependency edge
    private val emailService: EmailService          // Dependency edge
)
```

#### 2. Cross-Language Dependencies
```kotlin
// Kotlin service
class KotlinPaymentService

// Java controller using Kotlin service
@Controller
public class JavaOrderController {
    @Autowired
    private KotlinPaymentService paymentService;  // Cross-language edge
}
```

#### 3. Package-Level Analysis
```
Package Dependencies:
com.example.controller → com.example.service
com.example.service → com.example.repository
com.example.service → com.example.domain
```

### Cycle Detection

#### Method: Depth-First Search
```kotlin
fun detectCycles(nodes: List<Node>, edges: List<Edge>): List<Cycle> {
    val visited = mutableSetOf<String>()
    val recursionStack = mutableSetOf<String>()
    val cycles = mutableListOf<Cycle>()
    
    for (node in nodes) {
        if (node.id !in visited) {
            dfs(node.id, visited, recursionStack, cycles)
        }
    }
    
    return cycles
}
```

#### Cycle Severity Classification
- **Low Severity**: 2-3 classes in cycle
- **Medium Severity**: 4-6 classes in cycle  
- **High Severity**: 7+ classes in cycle

### Visualization Features

#### Interactive Dependency Graph
- **D3.js Integration**: Interactive force-directed graph
- **Color Coding**: Different colors for different layer types
- **Node Sizing**: Based on coupling metrics (CBO/CA)
- **Edge Weights**: Dependency strength visualization

#### Layer Diagrams
- **Hierarchical Layout**: Shows proper layer ordering
- **Violation Highlighting**: Red edges for layer violations
- **Package Grouping**: Classes grouped by package/layer

## Architecture Quality Assessment

### Metrics Integration

The architecture analysis integrates with CK metrics:

#### Architecture Quality Score
```kotlin
Architecture Quality = Weighted Average of:
- Layer Adherence (30%): Proper layer separation
- Coupling Quality (25%): Inter-layer coupling metrics
- Pattern Compliance (20%): Architecture pattern adherence  
- Dependency Health (15%): Cycle count and severity
- DDD Pattern Quality (10%): DDD pattern confidence scores
```

#### Violation Impact Analysis
- **Critical Violations**: Break fundamental architecture rules
- **Major Violations**: Reduce maintainability significantly
- **Minor Violations**: Style or convention issues

### Recommendations Engine

#### Automated Suggestions

**For Layer Violations**:
```
❌ UserController directly accesses UserRepository
✅ Suggestion: Introduce UserService in Application layer
   - Create UserService class
   - Move business logic from Controller to Service
   - Inject UserRepository into Service instead
```

**For Circular Dependencies**:
```
❌ Circular dependency: ServiceA ↔ ServiceB ↔ ServiceC
✅ Suggestions:
   1. Extract common interface/abstract class
   2. Use event-driven communication
   3. Introduce mediator pattern
```

**For High Coupling**:
```
❌ OrderService has CBO = 18 (too many dependencies)
✅ Suggestions:
   1. Apply Single Responsibility Principle
   2. Extract specialized services (PaymentHandler, InventoryManager)
   3. Use Facade pattern to hide complexity
```

## Best Practices

### DDD Implementation
1. **Clear Boundaries**: Separate bounded contexts
2. **Rich Domain Models**: Behavior-focused entities
3. **Repository Abstractions**: Clean data access interfaces
4. **Domain Events**: Decouple bounded contexts

### Layer Design  
1. **Dependency Direction**: Always point toward domain
2. **Interface Segregation**: Small, focused interfaces
3. **Single Responsibility**: Each layer has clear purpose
4. **Testability**: Easy to mock and test in isolation

### Dependency Management
1. **Dependency Injection**: Use DI containers
2. **Interface Abstractions**: Depend on abstractions
3. **Minimal Coupling**: Keep dependencies to minimum
4. **Cycle Prevention**: Regular architecture validation

This comprehensive architecture analysis helps maintain clean, maintainable, and well-structured software systems.