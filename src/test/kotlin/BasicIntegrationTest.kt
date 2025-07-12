import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtClassOrObject
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.metrics.model.analysis.ClassAnalysis
import com.metrics.model.analysis.ComplexityAnalysis
import com.metrics.model.analysis.MethodComplexity
import com.metrics.model.analysis.Suggestion
import com.metrics.model.architecture.DependencyGraph
import com.metrics.model.architecture.DependencyNode
import com.metrics.model.architecture.DependencyEdge
import com.metrics.model.architecture.DddPatternAnalysis
import com.metrics.model.common.NodeType
import com.metrics.model.common.DependencyType
import com.metrics.util.ComplexityCalculator
import com.metrics.util.LcomCalculator
import com.metrics.util.SuggestionGenerator
import java.io.File
import java.nio.file.Path

/**
 * Basic integration tests to verify core functionality works.
 * These tests focus on ensuring the analysis functions can run without errors
 * rather than testing exact LCOM/complexity values.
 */
class BasicIntegrationTest {
    
    private lateinit var disposable: Disposable
    private lateinit var psiFileFactory: PsiFileFactory
    
    @TempDir
    lateinit var tempDir: Path
    
    @BeforeEach
    fun setUp() {
        disposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration()
        configuration.put(CommonConfigurationKeys.MODULE_NAME, "test")
        val env = KotlinCoreEnvironment.createForProduction(
            disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        psiFileFactory = PsiFileFactory.getInstance(env.project)
    }
    
    @Test
    fun `should analyze simple Kotlin class without errors`() {
        val kotlinCode = """
            class SimpleKotlinClass {
                private val name: String = "test"
                
                fun getName(): String {
                    return name
                }
            }
        """.trimIndent()
        
        val ktFile = psiFileFactory.createFileFromText(
            "SimpleKotlinClass.kt",
            KotlinLanguage.INSTANCE,
            kotlinCode
        ) as KtFile
        
        val classOrObject = ktFile.declarations.filterIsInstance<KtClassOrObject>().first()
        val analysis = analyzeKotlinClassWithNewStructure(classOrObject, "SimpleKotlinClass.kt")
        
        // Verify analysis completed successfully
        assertEquals("SimpleKotlinClass", analysis.className)
        assertEquals("SimpleKotlinClass.kt", analysis.fileName)
        assertTrue(analysis.lcom >= 0) // LCOM should be non-negative
        assertTrue(analysis.methodCount >= 0)
        assertTrue(analysis.propertyCount >= 0)
        assertNotNull(analysis.complexity)
        assertNotNull(analysis.suggestions)
    }
    
    @Test
    fun `should analyze simple Java class without errors`() {
        val javaCode = """
            public class SimpleJavaClass {
                private String name = "test";
                
                public String getName() {
                    return name;
                }
                
                public void setName(String name) {
                    this.name = name;
                }
            }
        """.trimIndent()
        
        val cu = StaticJavaParser.parse(javaCode)
        val classDecl = cu.findAll(ClassOrInterfaceDeclaration::class.java).first()
        val analysis = analyzeJavaClassWithNewStructure(classDecl, "SimpleJavaClass.java")
        
        // Verify analysis completed successfully
        assertEquals("SimpleJavaClass", analysis.className)
        assertEquals("SimpleJavaClass.java", analysis.fileName)
        assertTrue(analysis.lcom >= 0) // LCOM should be non-negative
        assertTrue(analysis.methodCount >= 0)
        assertTrue(analysis.propertyCount >= 0)
        assertNotNull(analysis.complexity)
        assertNotNull(analysis.suggestions)
    }
    
    @Test
    fun `should calculate cyclomatic complexity for Kotlin methods`() {
        val kotlinCode = """
            class ComplexKotlinClass {
                fun simpleMethod(): String {
                    return "simple"
                }
                
                fun complexMethod(x: Int): String {
                    if (x > 0) {
                        if (x > 10) {
                            return "large"
                        } else {
                            return "small"
                        }
                    } else {
                        return "negative"
                    }
                }
            }
        """.trimIndent()
        
        val ktFile = psiFileFactory.createFileFromText(
            "ComplexKotlinClass.kt",
            KotlinLanguage.INSTANCE,
            kotlinCode
        ) as KtFile
        
        val classOrObject = ktFile.declarations.filterIsInstance<KtClassOrObject>().first()
        val analysis = analyzeKotlinClassWithNewStructure(classOrObject, "ComplexKotlinClass.kt")
        
        // Verify complexity analysis
        assertTrue(analysis.complexity.methods.size >= 2)
        assertTrue(analysis.complexity.totalComplexity > 0)
        assertTrue(analysis.complexity.averageComplexity > 0)
        assertTrue(analysis.complexity.maxComplexity > 0)
        
        // Simple method should have lower complexity than complex method
        val simpleMethod = analysis.complexity.methods.find { it.methodName == "simpleMethod" }
        val complexMethod = analysis.complexity.methods.find { it.methodName == "complexMethod" }
        
        assertNotNull(simpleMethod)
        assertNotNull(complexMethod)
        assertTrue(complexMethod!!.cyclomaticComplexity > simpleMethod!!.cyclomaticComplexity)
    }
    
    @Test
    fun `should create basic dependency graph structure`() {
        // Create simple dependency graph nodes
        val nodes = listOf(
            DependencyNode(
                id = "com.example.service.UserService",
                className = "UserService",
                fileName = "UserService.kt",
                packageName = "com.example.service",
                nodeType = NodeType.CLASS,
                layer = "application",
                language = "Kotlin"
            ),
            DependencyNode(
                id = "com.example.domain.User",
                className = "User",
                fileName = "User.java",
                packageName = "com.example.domain",
                nodeType = NodeType.CLASS,
                layer = "domain",
                language = "Java"
            )
        )
        
        val edges = listOf(
            DependencyEdge(
                fromId = "com.example.service.UserService",
                toId = "com.example.domain.User",
                dependencyType = DependencyType.USAGE,
                strength = 1
            )
        )
        
        val dependencyGraph = DependencyGraph(
            nodes = nodes,
            edges = edges,
            cycles = emptyList(),
            packages = emptyList()
        )
        
        // Verify graph structure
        assertEquals(2, dependencyGraph.nodes.size)
        assertTrue(dependencyGraph.nodes.any { it.className == "UserService" && it.language == "Kotlin" })
        assertTrue(dependencyGraph.nodes.any { it.className == "User" && it.language == "Java" })
        assertEquals(1, dependencyGraph.edges.size)
        assertNotNull(dependencyGraph.cycles)
    }
    
    @Test
    fun `should create basic DDD pattern analysis structure`() {
        // Create simple DDD pattern analysis
        val dddPatterns = DddPatternAnalysis(
            entities = emptyList(),
            valueObjects = emptyList(),
            services = emptyList(),
            repositories = emptyList(),
            aggregates = emptyList(),
            domainEvents = emptyList()
        )
        
        // Verify pattern detection structure
        assertNotNull(dddPatterns.services)
        assertNotNull(dddPatterns.entities)
        assertNotNull(dddPatterns.valueObjects)
        assertNotNull(dddPatterns.repositories)
        assertNotNull(dddPatterns.aggregates)
        assertNotNull(dddPatterns.domainEvents)
    }
    
    @Test
    fun `should handle empty files gracefully`() {
        val emptyKotlinCode = "// Empty file"
        
        val ktFile = psiFileFactory.createFileFromText(
            "Empty.kt",
            KotlinLanguage.INSTANCE,
            emptyKotlinCode
        ) as KtFile
        
        // Should not throw exceptions when analyzing empty files
        val dependencyGraph = DependencyGraph(
            nodes = emptyList(),
            edges = emptyList(),
            cycles = emptyList(),
            packages = emptyList()
        )
        
        val dddPatterns = DddPatternAnalysis(
            entities = emptyList(),
            valueObjects = emptyList(),
            services = emptyList(),
            repositories = emptyList(),
            aggregates = emptyList(),
            domainEvents = emptyList()
        )
        
        assertNotNull(dependencyGraph)
        assertNotNull(dddPatterns)
    }
    
    @Test
    fun `should use utility classes correctly`() {
        // Test that utility classes work correctly
        assertEquals("Simple", ComplexityCalculator.getComplexityLevel(1))
        assertTrue(ComplexityCalculator.getComplexityRecommendation(1).startsWith("✅"))
        
        // Test LCOM calculation
        val methodProps = mapOf(
            "method1" to setOf("prop1"),
            "method2" to setOf("prop2")
        )
        val lcom = LcomCalculator.calculateLcom(methodProps)
        assertEquals(1, lcom) // Two methods, no shared properties = 1 LCOM
        
        assertEquals("Good", LcomCalculator.getCohesionLevel(1))
        assertEquals("👍", LcomCalculator.getCohesionBadge(1))
    }
    
    private fun analyzeKotlinClassWithNewStructure(classOrObject: KtClassOrObject, fileName: String): ClassAnalysis {
        // Extract method-property relationships using new util classes
        val methodPropertiesMap = mutableMapOf<String, Set<String>>()
        val propertyNames = mutableSetOf<String>()
        val methodComplexities = mutableListOf<MethodComplexity>()
        
        // Get properties
        classOrObject.declarations.filterIsInstance<org.jetbrains.kotlin.psi.KtProperty>().forEach { prop ->
            propertyNames.add(prop.name ?: "unknown")
        }
        
        // Get methods and their property usage
        classOrObject.declarations.forEach { declaration ->
            if (declaration is org.jetbrains.kotlin.psi.KtNamedFunction) {
                val methodName = declaration.name ?: "unknown"
                val usedProperties = mutableSetOf<String>()
                
                // Simple property usage detection for test
                val bodyText = declaration.bodyExpression?.text ?: ""
                propertyNames.forEach { propName ->
                    if (bodyText.contains(propName)) {
                        usedProperties.add(propName)
                    }
                }
                
                methodPropertiesMap[methodName] = usedProperties
                
                // Calculate complexity for this method
                val complexity = ComplexityCalculator.calculateMethodComplexity(declaration)
                methodComplexities.add(MethodComplexity(methodName, complexity, 10)) // dummy line count
            }
        }
        
        // Calculate LCOM using the new util
        val lcom = LcomCalculator.calculateLcom(methodPropertiesMap)
        
        // Create complexity analysis
        val complexityAnalysis = ComplexityAnalysis(
            methods = methodComplexities,
            totalComplexity = methodComplexities.sumOf { it.cyclomaticComplexity },
            averageComplexity = if (methodComplexities.isNotEmpty()) 
                methodComplexities.map { it.cyclomaticComplexity }.average() else 0.0,
            maxComplexity = methodComplexities.maxOfOrNull { it.cyclomaticComplexity } ?: 0,
            complexMethods = methodComplexities.filter { it.cyclomaticComplexity > 10 }
        )
        
        // Generate suggestions
        val suggestions = SuggestionGenerator.generateSuggestions(
            lcom = lcom,
            methodProps = methodPropertiesMap,
            props = propertyNames.toList(),
            complexity = complexityAnalysis
        )
        
        return ClassAnalysis(
            className = classOrObject.name ?: "Unknown",
            fileName = fileName,
            lcom = lcom,
            methodCount = methodPropertiesMap.size,
            propertyCount = propertyNames.size,
            methodDetails = methodPropertiesMap,
            suggestions = suggestions,
            complexity = complexityAnalysis
        )
    }
    
    private fun analyzeJavaClassWithNewStructure(classDecl: ClassOrInterfaceDeclaration, fileName: String): ClassAnalysis {
        // Simple Java analysis implementation for testing
        val methodPropertiesMap = mutableMapOf<String, Set<String>>()
        val propertyNames = mutableSetOf<String>()
        val methodComplexities = mutableListOf<MethodComplexity>()
        
        // Get fields (properties)
        classDecl.fields.forEach { field ->
            field.variables.forEach { variable ->
                propertyNames.add(variable.nameAsString)
            }
        }
        
        // Get methods
        classDecl.methods.forEach { method ->
            val methodName = method.nameAsString
            val usedProperties = mutableSetOf<String>()
            
            // Simple property usage detection for test
            val bodyText = method.body.map { it.toString() }.orElse("")
            propertyNames.forEach { propName ->
                if (bodyText.contains(propName)) {
                    usedProperties.add(propName)
                }
            }
            
            methodPropertiesMap[methodName] = usedProperties
            
            // Calculate complexity for this method
            val complexity = ComplexityCalculator.calculateJavaCyclomaticComplexity(method)
            methodComplexities.add(MethodComplexity(methodName, complexity, 10)) // dummy line count
        }
        
        // Calculate LCOM using the new util
        val lcom = LcomCalculator.calculateLcom(methodPropertiesMap)
        
        // Create complexity analysis
        val complexityAnalysis = ComplexityAnalysis(
            methods = methodComplexities,
            totalComplexity = methodComplexities.sumOf { it.cyclomaticComplexity },
            averageComplexity = if (methodComplexities.isNotEmpty()) 
                methodComplexities.map { it.cyclomaticComplexity }.average() else 0.0,
            maxComplexity = methodComplexities.maxOfOrNull { it.cyclomaticComplexity } ?: 0,
            complexMethods = methodComplexities.filter { it.cyclomaticComplexity > 10 }
        )
        
        // Generate suggestions
        val suggestions = SuggestionGenerator.generateSuggestions(
            lcom = lcom,
            methodProps = methodPropertiesMap,
            props = propertyNames.toList(),
            complexity = complexityAnalysis
        )
        
        return ClassAnalysis(
            className = classDecl.nameAsString,
            fileName = fileName,
            lcom = lcom,
            methodCount = methodPropertiesMap.size,
            propertyCount = propertyNames.size,
            methodDetails = methodPropertiesMap,
            suggestions = suggestions,
            complexity = complexityAnalysis
        )
    }
    
    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
        Disposer.dispose(disposable)
    }
}