import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import com.metrics.model.analysis.*
import com.metrics.model.architecture.*
import com.metrics.model.common.*
import com.metrics.util.*

class ArchitectureAnalysisIntegrationTest {
    
    @Test
    fun `should create comprehensive architecture analysis structure`() {
        // Test that we can create a complete architecture analysis structure
        val dddPatterns = DddPatternAnalysis(
            entities = listOf(
                DddEntity(
                    className = "Order",
                    fileName = "Order.kt",
                    hasUniqueId = true,
                    isMutable = true,
                    idFields = listOf("id"),
                    confidence = 0.8
                )
            ),
            valueObjects = listOf(
                DddValueObject(
                    className = "Money",
                    fileName = "Money.kt", 
                    isImmutable = true,
                    hasValueEquality = true,
                    properties = listOf("amount", "currency"),
                    confidence = 0.9
                )
            ),
            services = listOf(
                DddService(
                    className = "OrderService",
                    fileName = "OrderService.kt",
                    isStateless = true,
                    hasDomainLogic = true,
                    methods = listOf("createOrder", "cancelOrder"),
                    confidence = 0.7
                )
            ),
            repositories = listOf(
                DddRepository(
                    className = "OrderRepository",
                    fileName = "OrderRepository.kt",
                    isInterface = true,
                    hasDataAccess = true,
                    crudMethods = listOf("save", "findById", "delete"),
                    confidence = 0.9
                )
            ),
            aggregates = emptyList(),
            domainEvents = emptyList()
        )
        
        val layers = listOf(
            ArchitectureLayer(
                name = "presentation",
                type = LayerType.PRESENTATION,
                packages = listOf("com.example.controller"),
                classes = listOf("OrderController"),
                level = 1
            ),
            ArchitectureLayer(
                name = "application", 
                type = LayerType.APPLICATION,
                packages = listOf("com.example.service"),
                classes = listOf("OrderService"),
                level = 2
            ),
            ArchitectureLayer(
                name = "domain",
                type = LayerType.DOMAIN,
                packages = listOf("com.example.domain"),
                classes = listOf("Order", "Money"),
                level = 3
            ),
            ArchitectureLayer(
                name = "data",
                type = LayerType.DATA,
                packages = listOf("com.example.repository"),
                classes = listOf("OrderRepository"),
                level = 4
            )
        )
        
        val dependencies = listOf(
            LayerDependency(
                fromLayer = "presentation",
                toLayer = "application",
                dependencyCount = 1,
                isValid = true
            ),
            LayerDependency(
                fromLayer = "application",
                toLayer = "domain", 
                dependencyCount = 2,
                isValid = true
            )
        )
        
        val violations = listOf(
            ArchitectureViolation(
                fromClass = "OrderController",
                toClass = "OrderRepository",
                violationType = ViolationType.LAYER_VIOLATION,
                suggestion = "Controller should not directly access repository"
            )
        )
        
        val layeredArchitecture = LayeredArchitectureAnalysis(
            layers = layers,
            dependencies = dependencies,
            violations = violations,
            pattern = ArchitecturePattern.LAYERED
        )
        
        val dependencyGraph = DependencyGraph(
            nodes = listOf(
                DependencyNode(
                    id = "com.example.service.OrderService",
                    className = "OrderService",
                    fileName = "OrderService.kt",
                    packageName = "com.example.service", 
                    nodeType = NodeType.CLASS,
                    layer = "application",
                    language = "Kotlin"
                )
            ),
            edges = emptyList(),
            cycles = emptyList(),
            packages = emptyList()
        )
        
        val architectureAnalysis = ArchitectureAnalysis(
            dddPatterns = dddPatterns,
            layeredArchitecture = layeredArchitecture,
            dependencyGraph = dependencyGraph
        )
        
        // Verify complete structure
        assertNotNull(architectureAnalysis)
        assertEquals(1, architectureAnalysis.dddPatterns.entities.size)
        assertEquals(1, architectureAnalysis.dddPatterns.valueObjects.size)
        assertEquals(1, architectureAnalysis.dddPatterns.services.size)
        assertEquals(1, architectureAnalysis.dddPatterns.repositories.size)
        assertEquals(4, architectureAnalysis.layeredArchitecture.layers.size)
        assertEquals(2, architectureAnalysis.layeredArchitecture.dependencies.size)
        assertEquals(1, architectureAnalysis.layeredArchitecture.violations.size)
        assertEquals(ArchitecturePattern.LAYERED, architectureAnalysis.layeredArchitecture.pattern)
        assertEquals(1, architectureAnalysis.dependencyGraph.nodes.size)
        
        println("✅ Complete architecture analysis structure created successfully!")
    }
    
    @Test
    fun `should integrate utility classes correctly`() {
        // Test LCOM calculation with sample data
        val methodProperties = mapOf(
            "getUserName" to setOf("name"),
            "getUserEmail" to setOf("email"), 
            "setUserActive" to setOf("active"),
            "getUserProfile" to setOf("name", "email", "active")
        )
        
        val lcom = LcomCalculator.calculateLcom(methodProperties)
        assertTrue(lcom >= 0)
        assertEquals("Good", LcomCalculator.getCohesionLevel(1))
        assertEquals("✅", LcomCalculator.getCohesionBadge(0))
        
        // Test architecture utilities
        assertEquals("application", ArchitectureUtils.inferLayer("com.example.service", "UserService"))
        assertEquals("domain", ArchitectureUtils.inferLayer("com.example.domain", "User"))
        assertEquals("data", ArchitectureUtils.inferLayer("com.example.repository", "UserRepository"))
        assertEquals("presentation", ArchitectureUtils.inferLayer("com.example.controller", "UserController"))
        
        // Test layer dependency validation
        assertTrue(ArchitectureUtils.isValidLayerDependency("presentation", "application"))
        assertTrue(ArchitectureUtils.isValidLayerDependency("application", "domain"))
        assertFalse(ArchitectureUtils.isValidLayerDependency("domain", "application"))
        
        // Test complexity utilities
        assertEquals("Simple", ComplexityCalculator.getComplexityLevel(1))
        assertEquals("Low", ComplexityCalculator.getComplexityLevel(3))
        assertEquals("High", ComplexityCalculator.getComplexityLevel(15))
        assertTrue(ComplexityCalculator.isComplex(15))
        assertFalse(ComplexityCalculator.isComplex(5))
        
        println("✅ All utility classes integrated successfully!")
    }
    
    @Test
    fun `should create complete project report structure`() {
        // Test that we can create a complete project report
        val classAnalysis = ClassAnalysis(
            className = "UserService",
            fileName = "UserService.kt",
            lcom = 2,
            methodCount = 5,
            propertyCount = 2,
            methodDetails = mapOf(
                "createUser" to setOf("userRepository", "validator"),
                "deleteUser" to setOf("userRepository"),
                "findUser" to setOf("userRepository"),
                "validateUser" to setOf("validator"),
                "logAction" to setOf("logger")
            ),
            suggestions = listOf(
                Suggestion(
                    icon = "⚠️",
                    message = "Consider separating validation and logging concerns",
                    tooltip = "High LCOM indicates low cohesion"
                )
            ),
            complexity = ComplexityAnalysis(
                methods = listOf(
                    MethodComplexity("createUser", 3, 15),
                    MethodComplexity("deleteUser", 2, 8),
                    MethodComplexity("findUser", 1, 5)
                ),
                totalComplexity = 6,
                averageComplexity = 2.0,
                maxComplexity = 3,
                complexMethods = emptyList()
            )
        )
        
        val architectureAnalysis = ArchitectureAnalysis(
            dddPatterns = DddPatternAnalysis(
                entities = emptyList(),
                valueObjects = emptyList(),
                services = listOf(
                    DddService(
                        className = "UserService",
                        fileName = "UserService.kt",
                        isStateless = true,
                        hasDomainLogic = true,
                        methods = listOf("createUser", "deleteUser", "findUser"),
                        confidence = 0.8
                    )
                ),
                repositories = emptyList(),
                aggregates = emptyList(),
                domainEvents = emptyList()
            ),
            layeredArchitecture = LayeredArchitectureAnalysis(
                layers = listOf(
                    ArchitectureLayer(
                        name = "application",
                        type = LayerType.APPLICATION,
                        packages = listOf("com.example.service"),
                        classes = listOf("UserService"),
                        level = 2
                    )
                ),
                dependencies = emptyList(),
                violations = emptyList(),
                pattern = ArchitecturePattern.LAYERED
            ),
            dependencyGraph = DependencyGraph(
                nodes = listOf(
                    DependencyNode(
                        id = "com.example.service.UserService",
                        className = "UserService",
                        fileName = "UserService.kt",
                        packageName = "com.example.service",
                        nodeType = NodeType.CLASS,
                        layer = "application",
                        language = "Kotlin"
                    )
                ),
                edges = emptyList(),
                cycles = emptyList(),
                packages = emptyList()
            )
        )
        
        val projectReport = ProjectReport(
            timestamp = "2025-01-01 12:00:00",
            classes = listOf(classAnalysis),
            summary = "Integration test project analysis",
            architectureAnalysis = architectureAnalysis
        )
        
        // Verify complete report structure
        assertNotNull(projectReport)
        assertEquals(1, projectReport.classes.size)
        assertEquals("UserService", projectReport.classes[0].className)
        assertEquals(2, projectReport.classes[0].lcom)
        assertEquals(5, projectReport.classes[0].methodCount)
        assertEquals(1, projectReport.classes[0].suggestions.size)
        assertEquals(6, projectReport.classes[0].complexity.totalComplexity)
        
        assertEquals(1, projectReport.architectureAnalysis.dddPatterns.services.size)
        assertEquals(1, projectReport.architectureAnalysis.layeredArchitecture.layers.size)
        assertEquals(1, projectReport.architectureAnalysis.dependencyGraph.nodes.size)
        assertEquals(ArchitecturePattern.LAYERED, projectReport.architectureAnalysis.layeredArchitecture.pattern)
        
        println("✅ Complete project report structure created successfully!")
    }
    
    @Test
    fun `should handle different architecture patterns`() {
        // Test all architecture pattern enum values
        val patterns = ArchitecturePattern.values()
        assertTrue(patterns.contains(ArchitecturePattern.LAYERED))
        assertTrue(patterns.contains(ArchitecturePattern.HEXAGONAL))
        assertTrue(patterns.contains(ArchitecturePattern.CLEAN))
        assertTrue(patterns.contains(ArchitecturePattern.ONION))
        assertTrue(patterns.contains(ArchitecturePattern.UNKNOWN))
        
        // Test layer types
        val layerTypes = LayerType.values()
        assertTrue(layerTypes.contains(LayerType.PRESENTATION))
        assertTrue(layerTypes.contains(LayerType.APPLICATION))
        assertTrue(layerTypes.contains(LayerType.DOMAIN))
        assertTrue(layerTypes.contains(LayerType.DATA))
        assertTrue(layerTypes.contains(LayerType.INFRASTRUCTURE))
        
        // Test violation types
        val violationTypes = ViolationType.values()
        assertTrue(violationTypes.contains(ViolationType.LAYER_VIOLATION))
        assertTrue(violationTypes.contains(ViolationType.CIRCULAR_DEPENDENCY))
        assertTrue(violationTypes.contains(ViolationType.DEPENDENCY_INVERSION))
        
        println("✅ All architecture patterns and types verified!")
    }
}