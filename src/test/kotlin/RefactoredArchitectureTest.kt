import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import com.metrics.model.analysis.*
import com.metrics.model.architecture.*
import com.metrics.model.common.*
import com.metrics.parser.MultiLanguageParser
import com.metrics.analyzer.core.KotlinCodeAnalyzer
import com.metrics.analyzer.core.JavaCodeAnalyzer
import com.metrics.report.console.ConsoleReportGenerator
import com.metrics.report.html.HtmlReportGenerator
import com.metrics.util.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Test to verify the refactored architecture works correctly.
 * This demonstrates how the new modular structure should be used.
 */
class RefactoredArchitectureTest {

    @Test
    fun `should create and use all main components without errors`() {
        // Test that all main components can be instantiated
        val parser = MultiLanguageParser()
        val kotlinAnalyzer = KotlinCodeAnalyzer()
        val javaAnalyzer = JavaCodeAnalyzer()
        val consoleReporter = ConsoleReportGenerator()
        val htmlReporter = HtmlReportGenerator()
        
        assertNotNull(parser)
        assertNotNull(kotlinAnalyzer)
        assertNotNull(javaAnalyzer)
        assertNotNull(consoleReporter)
        assertNotNull(htmlReporter)
        
        // Cleanup
        parser.dispose()
    }
    
    @Test
    fun `utility classes should be accessible and functional`() {
        // Test utility classes can be used
        
        // Test ArchitectureUtils
        val layer = ArchitectureUtils.inferLayer("com.example.controller", "UserController")
        assertEquals("presentation", layer)
        
        val isValidDep = ArchitectureUtils.isValidLayerDependency("presentation", "application")
        assertTrue(isValidDep)
        
        // Test TypeResolutionUtils
        val simpleName = TypeResolutionUtils.getSimpleClassName("com.example.User")
        assertEquals("User", simpleName)
        
        val packageName = TypeResolutionUtils.getPackageName("com.example.User")
        assertEquals("com.example", packageName)
        
        // Test ComplexityCalculator
        val level = ComplexityCalculator.getComplexityLevel(5)
        assertEquals("Low", level)
        
        val isComplex = ComplexityCalculator.isComplex(15)
        assertTrue(isComplex)
        
        val recommendation = ComplexityCalculator.getComplexityRecommendation(3)
        assertTrue(recommendation.contains("Good"))
    }
    
    @Test
    fun `should create valid project report structure`() {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        
        // Create sample data structures
        val methodComplexity = MethodComplexity("testMethod", 3, 10)
        val complexityAnalysis = ComplexityAnalysis(
            methods = listOf(methodComplexity),
            totalComplexity = 3,
            averageComplexity = 3.0,
            maxComplexity = 3,
            complexMethods = emptyList()
        )
        
        val suggestion = Suggestion("✅", "Good design", "This is a well-designed class")
        
        val classAnalysis = ClassAnalysis(
            className = "TestClass",
            fileName = "TestClass.kt",
            lcom = 0,
            methodCount = 1,
            propertyCount = 2,
            methodDetails = mapOf("testMethod" to setOf("property1", "property2")),
            suggestions = listOf(suggestion),
            complexity = complexityAnalysis,
            ckMetrics = CkMetrics(
                wmc = 6,
                cyclomaticComplexity = 6,
                cbo = 3,
                rfc = 8,
                ca = 1,
                ce = 3,
                dit = 0,
                noc = 0,
                lcom = 2
            ),
            qualityScore = QualityScore(
                cohesion = 7.0,
                complexity = 8.0,
                coupling = 6.0,
                inheritance = 9.0,
                architecture = 7.0,
                overall = 7.4
            ),
            riskAssessment = RiskAssessment(
                level = RiskLevel.LOW,
                reasons = emptyList(),
                impact = "Minimal impact on code quality",
                priority = 1
            )
        )
        
        val architectureAnalysis = ArchitectureAnalysis(
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
        
        val projectReport = ProjectReport(
            timestamp = timestamp,
            classes = listOf(classAnalysis),
            summary = "Test summary",
            architectureAnalysis = architectureAnalysis,
            projectQualityScore = QualityScore(
                cohesion = 7.0,
                complexity = 8.0,
                coupling = 6.0,
                inheritance = 9.0,
                architecture = 7.0,
                overall = 7.4
            ),
            packageMetrics = emptyList(),
            couplingMatrix = emptyList(),
            riskAssessments = emptyList()
        )
        
        // Verify structure
        assertNotNull(projectReport)
        assertEquals(1, projectReport.classes.size)
        assertEquals("TestClass", projectReport.classes.first().className)
        assertEquals(ArchitecturePattern.LAYERED, projectReport.architectureAnalysis.layeredArchitecture.pattern)
        assertTrue(projectReport.timestamp.isNotEmpty())
    }
    
    @Test
    fun `suggestion generator should work with sample data`() {
        val methodComplexity = MethodComplexity("complexMethod", 15, 50)
        val complexityAnalysis = ComplexityAnalysis(
            methods = listOf(methodComplexity),
            totalComplexity = 15,
            averageComplexity = 15.0,
            maxComplexity = 15,
            complexMethods = listOf(methodComplexity)
        )
        
        val methodProps = mapOf(
            "method1" to setOf("prop1", "prop2"),
            "method2" to setOf("prop3")
        )
        val props = listOf("prop1", "prop2", "prop3", "unusedProp")
        
        val suggestions = SuggestionGenerator.generateSuggestions(
            lcom = 8,
            methodProps = methodProps,
            props = props,
            complexity = complexityAnalysis
        )
        
        assertFalse(suggestions.isEmpty())
        assertTrue(suggestions.any { it.message.contains("refactoring", ignoreCase = true) })
        assertTrue(suggestions.any { it.message.contains("complex", ignoreCase = true) })
    }
    
    @Test
    fun `pattern matching utils should detect patterns correctly`() {
        // Test pattern detection
        assertTrue(PatternMatchingUtils.isTestClass("UserServiceTest"))
        assertTrue(PatternMatchingUtils.isTestClass("PaymentSpec"))
        assertFalse(PatternMatchingUtils.isTestClass("UserService"))
        
        assertTrue(PatternMatchingUtils.isUtilityClass("StringUtils"))
        assertTrue(PatternMatchingUtils.isUtilityClass("DateHelper"))
        assertFalse(PatternMatchingUtils.isUtilityClass("UserService"))
        
        assertTrue(PatternMatchingUtils.isExceptionClass("UserNotFoundException"))
        assertTrue(PatternMatchingUtils.isExceptionClass("ValidationError"))
        assertFalse(PatternMatchingUtils.isExceptionClass("UserService"))
        
        assertTrue(PatternMatchingUtils.isControllerClass("UserController"))
        assertTrue(PatternMatchingUtils.isServiceClass("UserService"))
        assertTrue(PatternMatchingUtils.isRepositoryClass("UserRepository"))
        assertTrue(PatternMatchingUtils.isEntityClass("UserEntity"))
    }
    
    @Test
    fun `cycle detection utils should work with empty data`() {
        val emptyNodes = emptyList<DependencyNode>()
        val emptyEdges = emptyList<DependencyEdge>()
        
        val cycles = CycleDetectionUtils.detectCycles(emptyNodes, emptyEdges)
        assertTrue(cycles.isEmpty())
        
        val cohesion = CycleDetectionUtils.calculatePackageCohesion("com.example", emptyEdges, emptyNodes)
        assertEquals(1.0, cohesion, 0.01)
    }
    
    @Test
    fun `architecture pattern detection should work`() {
        val emptyLayers = emptyList<ArchitectureLayer>()
        val emptyDependencies = emptyList<LayerDependency>()
        
        val pattern = ArchitectureUtils.determineArchitecturePattern(emptyLayers, emptyDependencies)
        assertEquals(ArchitecturePattern.LAYERED, pattern) // Default for empty data
    }
}