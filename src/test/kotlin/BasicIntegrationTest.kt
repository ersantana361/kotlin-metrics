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
        val analysis = analyzeClass(classOrObject, "SimpleKotlinClass.kt")
        
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
        val analysis = analyzeJavaClass(classDecl, "SimpleJavaClass.java")
        
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
        val analysis = analyzeClass(classOrObject, "ComplexKotlinClass.kt")
        
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
    fun `should build dependency graph for mixed languages`() {
        // Create Kotlin file
        val kotlinCode = """
            package com.example.service
            class UserService {
                fun processUser(): String {
                    return "processed"
                }
            }
        """.trimIndent()
        
        // Create Java file
        val javaCode = """
            package com.example.domain;
            public class User {
                private String name;
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
        """.trimIndent()
        
        val ktFile = psiFileFactory.createFileFromText(
            "UserService.kt",
            KotlinLanguage.INSTANCE,
            kotlinCode
        ) as KtFile
        
        val javaFile = tempDir.resolve("User.java").toFile()
        javaFile.writeText(javaCode)
        
        // Build dependency graph
        val dependencyGraph = buildMixedDependencyGraph(listOf(ktFile), listOf(javaFile))
        
        // Verify graph structure
        assertTrue(dependencyGraph.nodes.size >= 2)
        assertTrue(dependencyGraph.nodes.any { it.className == "UserService" && it.language == "Kotlin" })
        assertTrue(dependencyGraph.nodes.any { it.className == "User" && it.language == "Java" })
        assertTrue(dependencyGraph.packages.isNotEmpty())
        assertNotNull(dependencyGraph.edges)
        assertNotNull(dependencyGraph.cycles)
    }
    
    @Test
    fun `should detect DDD patterns in mixed languages`() {
        // Kotlin Service
        val kotlinServiceCode = """
            package com.example.service
            class PaymentService {
                fun processPayment(amount: Double): Boolean {
                    return amount > 0
                }
            }
        """.trimIndent()
        
        // Java Entity
        val javaEntityCode = """
            package com.example.domain;
            import javax.persistence.*;
            
            @Entity
            public class Payment {
                @Id
                private Long id;
                private Double amount;
                
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
                public Double getAmount() { return amount; }
                public void setAmount(Double amount) { this.amount = amount; }
            }
        """.trimIndent()
        
        val serviceFile = psiFileFactory.createFileFromText(
            "PaymentService.kt",
            KotlinLanguage.INSTANCE,
            kotlinServiceCode
        ) as KtFile
        
        val entityFile = tempDir.resolve("Payment.java").toFile()
        entityFile.writeText(javaEntityCode)
        
        // Analyze DDD patterns
        val dddPatterns = analyzeMixedDddPatterns(listOf(serviceFile), listOf(entityFile), emptyList())
        
        // Verify pattern detection
        assertNotNull(dddPatterns.services)
        assertNotNull(dddPatterns.entities)
        assertNotNull(dddPatterns.valueObjects)
        assertNotNull(dddPatterns.repositories)
        assertNotNull(dddPatterns.aggregates)
        assertNotNull(dddPatterns.domainEvents)
        
        // Should detect at least the service and entity
        assertTrue(dddPatterns.services.size + dddPatterns.entities.size > 0)
    }
    
    @Test
    fun `should handle empty files gracefully`() {
        val emptyKotlinCode = "// Empty file"
        val emptyJavaCode = "// Empty Java file"
        
        val ktFile = psiFileFactory.createFileFromText(
            "Empty.kt",
            KotlinLanguage.INSTANCE,
            emptyKotlinCode
        ) as KtFile
        
        val javaFile = tempDir.resolve("Empty.java").toFile()
        javaFile.writeText(emptyJavaCode)
        
        // Should not throw exceptions
        val dependencyGraph = buildMixedDependencyGraph(listOf(ktFile), listOf(javaFile))
        val dddPatterns = analyzeMixedDddPatterns(listOf(ktFile), listOf(javaFile), emptyList())
        
        assertNotNull(dependencyGraph)
        assertNotNull(dddPatterns)
    }
    
    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
        Disposer.dispose(disposable)
    }
}