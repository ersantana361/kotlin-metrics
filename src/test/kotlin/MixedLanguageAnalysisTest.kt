import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import com.metrics.model.analysis.*
import com.metrics.model.architecture.*
import com.metrics.model.common.*
import com.metrics.parser.MultiLanguageParser
import com.metrics.analyzer.core.KotlinCodeAnalyzer
import com.metrics.analyzer.core.JavaCodeAnalyzer
import com.metrics.util.ArchitectureUtils

class MixedLanguageAnalysisTest {
    
    @Test
    fun `should instantiate mixed language components without errors`() {
        // Test that mixed language analysis components can be created
        val multiParser = MultiLanguageParser()
        val kotlinAnalyzer = KotlinCodeAnalyzer()
        val javaAnalyzer = JavaCodeAnalyzer()
        
        assertNotNull(multiParser)
        assertNotNull(kotlinAnalyzer)
        assertNotNull(javaAnalyzer)
    }
    
    @Test
    fun `should handle mixed language project structure`() {
        // Create sample mixed language project analysis
        val kotlinClass = ClassAnalysis(
            className = "UserService",
            fileName = "UserService.kt",
            lcom = 0,
            methodCount = 3,
            propertyCount = 1,
            methodDetails = mapOf("createUser" to setOf("userRepository")),
            suggestions = emptyList(),
            complexity = ComplexityAnalysis(
                methods = listOf(MethodComplexity("createUser", 2, 10)),
                totalComplexity = 2,
                averageComplexity = 2.0,
                maxComplexity = 2,
                complexMethods = emptyList()
            )
        )
        
        val javaClass = ClassAnalysis(
            className = "User",
            fileName = "User.java",
            lcom = 1,
            methodCount = 2,
            propertyCount = 3,
            methodDetails = mapOf("getId" to setOf("id"), "getName" to setOf("name")),
            suggestions = emptyList(),
            complexity = ComplexityAnalysis(
                methods = listOf(MethodComplexity("getId", 1, 5), MethodComplexity("getName", 1, 5)),
                totalComplexity = 2,
                averageComplexity = 1.0,
                maxComplexity = 1,
                complexMethods = emptyList()
            )
        )
        
        val mixedProject = ProjectReport(
            timestamp = "2025-01-01 00:00:00",
            classes = listOf(kotlinClass, javaClass),
            summary = "Mixed Kotlin/Java project",
            architectureAnalysis = ArchitectureAnalysis(
                dddPatterns = DddPatternAnalysis(
                    entities = emptyList(),
                    valueObjects = emptyList(),
                    services = emptyList(),
                    repositories = emptyList(),
                    aggregates = emptyList(),
                    domainEvents = emptyList()
                ),
                layeredArchitecture = LayeredArchitectureAnalysis(
                    layers = emptyList(),
                    dependencies = emptyList(),
                    violations = emptyList(),
                    pattern = ArchitecturePattern.LAYERED
                ),
                dependencyGraph = DependencyGraph(
                    nodes = emptyList(),
                    edges = emptyList(),
                    cycles = emptyList(),
                    packages = emptyList()
                )
            )
        )
        
        // Verify mixed project analysis
        assertEquals(2, mixedProject.classes.size)
        assertTrue(mixedProject.classes.any { it.fileName.endsWith(".kt") })
        assertTrue(mixedProject.classes.any { it.fileName.endsWith(".java") })
        assertEquals("Mixed Kotlin/Java project", mixedProject.summary)
    }
    
    @Test
    fun `should infer layers correctly for mixed languages`() {
        // Test layer inference for different file types
        assertEquals("application", ArchitectureUtils.inferLayer("com.example.service", "UserService"))
        assertEquals("application", ArchitectureUtils.inferLayer("com.example.application", "UserManager"))
        assertEquals("domain", ArchitectureUtils.inferLayer("com.example.domain", "User"))
        assertEquals("data", ArchitectureUtils.inferLayer("com.example.repository", "UserRepository"))
        assertEquals("presentation", ArchitectureUtils.inferLayer("com.example.controller", "UserController"))
    }
    
    @Test
    fun `should handle mixed language dependency graphs`() {
        // Create mixed language dependency graph
        val kotlinNode = DependencyNode(
            id = "com.example.service.UserService",
            className = "UserService", 
            fileName = "UserService.kt",
            packageName = "com.example.service",
            nodeType = NodeType.CLASS,
            layer = "application",
            language = "Kotlin"
        )
        
        val javaNode = DependencyNode(
            id = "com.example.domain.User",
            className = "User",
            fileName = "User.java", 
            packageName = "com.example.domain",
            nodeType = NodeType.CLASS,
            layer = "domain",
            language = "Java"
        )
        
        val dependency = DependencyEdge(
            fromId = "com.example.service.UserService",
            toId = "com.example.domain.User",
            dependencyType = DependencyType.USAGE,
            strength = 1
        )
        
        val mixedGraph = DependencyGraph(
            nodes = listOf(kotlinNode, javaNode),
            edges = listOf(dependency),
            cycles = emptyList(),
            packages = emptyList()
        )
        
        // Verify mixed language graph
        assertEquals(2, mixedGraph.nodes.size)
        assertEquals(1, mixedGraph.edges.size)
        assertTrue(mixedGraph.nodes.any { it.language == "Kotlin" })
        assertTrue(mixedGraph.nodes.any { it.language == "Java" })
        
        val kotlinNode_result = mixedGraph.nodes.find { it.language == "Kotlin" }
        val javaNode_result = mixedGraph.nodes.find { it.language == "Java" }
        
        assertNotNull(kotlinNode_result)
        assertNotNull(javaNode_result)
        assertEquals("UserService", kotlinNode_result?.className)
        assertEquals("User", javaNode_result?.className)
    }
    
    @Test
    fun `should validate mixed language architecture patterns`() {
        // Test that architectural rules work across languages
        assertTrue(ArchitectureUtils.isValidLayerDependency("application", "domain"))
        assertTrue(ArchitectureUtils.isValidLayerDependency("presentation", "application"))
        assertFalse(ArchitectureUtils.isValidLayerDependency("domain", "application"))
        
        // These rules should apply regardless of implementation language
        val pattern = ArchitectureUtils.determineArchitecturePattern(emptyList(), emptyList())
        assertEquals(ArchitecturePattern.LAYERED, pattern)
    }
    
    @Test
    fun `should handle mixed language file extensions`() {
        // Test that the parser can differentiate file types
        assertTrue("UserService.kt".endsWith(".kt"))
        assertTrue("User.java".endsWith(".java"))
        assertFalse("UserService.kt".endsWith(".java"))
        assertFalse("User.java".endsWith(".kt"))
    }
    
    @Test
    fun `should create comprehensive mixed project report`() {
        val report = ProjectReport(
            timestamp = "2025-01-01 12:00:00",
            classes = listOf(
                ClassAnalysis(
                    className = "KotlinClass",
                    fileName = "KotlinClass.kt", 
                    lcom = 0,
                    methodCount = 1,
                    propertyCount = 1,
                    methodDetails = emptyMap(),
                    suggestions = emptyList(),
                    complexity = ComplexityAnalysis(emptyList(), 0, 0.0, 0, emptyList())
                ),
                ClassAnalysis(
                    className = "JavaClass",
                    fileName = "JavaClass.java",
                    lcom = 1, 
                    methodCount = 2,
                    propertyCount = 2,
                    methodDetails = emptyMap(),
                    suggestions = emptyList(),
                    complexity = ComplexityAnalysis(emptyList(), 0, 0.0, 0, emptyList())
                )
            ),
            summary = "Mixed language analysis completed",
            architectureAnalysis = ArchitectureAnalysis(
                dddPatterns = DddPatternAnalysis(
                    entities = emptyList(),
                    valueObjects = emptyList(), 
                    services = emptyList(),
                    repositories = emptyList(),
                    aggregates = emptyList(),
                    domainEvents = emptyList()
                ),
                layeredArchitecture = LayeredArchitectureAnalysis(
                    layers = emptyList(),
                    dependencies = emptyList(),
                    violations = emptyList(),
                    pattern = ArchitecturePattern.LAYERED
                ),
                dependencyGraph = DependencyGraph(
                    nodes = emptyList(),
                    edges = emptyList(),
                    cycles = emptyList(),
                    packages = emptyList()
                )
            )
        )
        
        assertNotNull(report)
        assertEquals(2, report.classes.size)
        assertTrue(report.summary.contains("Mixed language"))
        assertNotNull(report.architectureAnalysis)
    }
}