import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import com.metrics.model.architecture.*
import com.metrics.model.common.*
import com.metrics.util.ArchitectureUtils

class LayeredArchitectureTest {
    
    @Test
    fun `should validate layer dependencies correctly`() {
        // Test valid dependencies
        assertTrue(ArchitectureUtils.isValidLayerDependency("presentation", "application"))
        assertTrue(ArchitectureUtils.isValidLayerDependency("application", "domain"))
        assertTrue(ArchitectureUtils.isValidLayerDependency("application", "data"))
        assertTrue(ArchitectureUtils.isValidLayerDependency("domain", "infrastructure"))
        
        // Test invalid dependencies (violate layer rules)
        assertFalse(ArchitectureUtils.isValidLayerDependency("domain", "application"))
        assertFalse(ArchitectureUtils.isValidLayerDependency("domain", "presentation"))
        assertFalse(ArchitectureUtils.isValidLayerDependency("data", "presentation"))
    }
    
    @Test
    fun `should determine architecture patterns correctly`() {
        // Test with empty data (should default to LAYERED)
        val emptyLayers = emptyList<ArchitectureLayer>()
        val emptyDependencies = emptyList<LayerDependency>()
        
        val pattern = ArchitectureUtils.determineArchitecturePattern(emptyLayers, emptyDependencies)
        assertEquals(ArchitecturePattern.LAYERED, pattern)
    }
    
    @Test
    fun `should create layered architecture analysis structure`() {
        // Create sample architecture analysis
        val layers = listOf(
            ArchitectureLayer("presentation", LayerType.PRESENTATION, listOf("com.example.controller"), listOf("UserController"), 1),
            ArchitectureLayer("application", LayerType.APPLICATION, listOf("com.example.service"), listOf("UserService"), 2),
            ArchitectureLayer("domain", LayerType.DOMAIN, listOf("com.example.domain"), listOf("User"), 3),
            ArchitectureLayer("data", LayerType.DATA, listOf("com.example.repository"), listOf("UserRepository"), 4)
        )
        
        val dependencies = listOf(
            LayerDependency("presentation", "application", 1, true),
            LayerDependency("application", "domain", 2, true),
            LayerDependency("application", "data", 1, true)
        )
        
        val violations = listOf(
            ArchitectureViolation(
                fromClass = "User",
                toClass = "UserService", 
                violationType = ViolationType.LAYER_VIOLATION,
                suggestion = "Domain should not depend on application layer"
            )
        )
        
        val analysis = LayeredArchitectureAnalysis(
            layers = layers,
            dependencies = dependencies,
            violations = violations,
            pattern = ArchitecturePattern.LAYERED
        )
        
        // Verify structure
        assertEquals(4, analysis.layers.size)
        assertEquals(3, analysis.dependencies.size)
        assertEquals(1, analysis.violations.size)
        assertEquals(ArchitecturePattern.LAYERED, analysis.pattern)
        
        assertTrue(analysis.layers.any { it.name == "presentation" })
        assertTrue(analysis.layers.any { it.name == "application" })
        assertTrue(analysis.layers.any { it.name == "domain" })
        assertTrue(analysis.layers.any { it.name == "data" })
    }
    
    @Test
    fun `should handle different architecture patterns`() {
        // Test different architecture pattern enums
        val patterns = ArchitecturePattern.values()
        
        assertTrue(patterns.contains(ArchitecturePattern.LAYERED))
        assertTrue(patterns.contains(ArchitecturePattern.HEXAGONAL))
        assertTrue(patterns.contains(ArchitecturePattern.CLEAN))
        assertTrue(patterns.contains(ArchitecturePattern.ONION))
        assertTrue(patterns.contains(ArchitecturePattern.UNKNOWN))
    }
    
    @Test
    fun `should handle violation types correctly`() {
        val violationTypes = ViolationType.values()
        
        assertTrue(violationTypes.contains(ViolationType.LAYER_VIOLATION))
        assertTrue(violationTypes.contains(ViolationType.CIRCULAR_DEPENDENCY))
        assertTrue(violationTypes.contains(ViolationType.DEPENDENCY_INVERSION))
    }
    
    @Test
    fun `should create architecture layer correctly`() {
        val layer = ArchitectureLayer(
            name = "test-layer",
            type = LayerType.APPLICATION,
            packages = listOf("com.example.test"),
            classes = listOf("TestClass1", "TestClass2"),
            level = 2
        )
        
        assertEquals("test-layer", layer.name)
        assertEquals(LayerType.APPLICATION, layer.type)
        assertEquals(1, layer.packages.size)
        assertEquals(2, layer.classes.size)
        assertEquals(2, layer.level)
        assertTrue(layer.classes.contains("TestClass1"))
        assertTrue(layer.classes.contains("TestClass2"))
    }
    
    @Test
    fun `should create layer dependency correctly`() {
        val dependency = LayerDependency(
            fromLayer = "source",
            toLayer = "target", 
            dependencyCount = 5,
            isValid = true
        )
        
        assertEquals("source", dependency.fromLayer)
        assertEquals("target", dependency.toLayer)
        assertEquals(5, dependency.dependencyCount)
        assertTrue(dependency.isValid)
    }
    
    @Test
    fun `should create architecture violation correctly`() {
        val violation = ArchitectureViolation(
            fromClass = "ViolatingClass",
            toClass = "TargetClass",
            violationType = ViolationType.LAYER_VIOLATION,
            suggestion = "Invalid dependency detected - use proper abstraction"
        )
        
        assertEquals("ViolatingClass", violation.fromClass)
        assertEquals("TargetClass", violation.toClass)
        assertEquals(ViolationType.LAYER_VIOLATION, violation.violationType)
        assertEquals("Invalid dependency detected - use proper abstraction", violation.suggestion)
    }
}