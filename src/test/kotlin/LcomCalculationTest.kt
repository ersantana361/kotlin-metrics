import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
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
import com.metrics.model.analysis.ClassAnalysis
import com.metrics.model.analysis.ComplexityAnalysis
import com.metrics.model.analysis.MethodComplexity
import com.metrics.model.analysis.Suggestion
import com.metrics.util.SuggestionGenerator
import com.metrics.util.ComplexityCalculator
import com.metrics.util.LcomCalculator
import java.io.File

class LcomCalculationTest {
    
    private lateinit var disposable: Disposable
    private lateinit var psiFileFactory: PsiFileFactory
    
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
    fun `should calculate LCOM 0 for perfectly cohesive class`() {
        val kotlinCode = """
            class CohesiveClass {
                private val sharedProperty: String = ""
                
                fun method1(): String {
                    return sharedProperty.uppercase()
                }
                
                fun method2(): Int {
                    return sharedProperty.length
                }
            }
        """.trimIndent()
        
        val analysis = analyzeKotlinClass(kotlinCode)
        assertEquals(0, analysis.lcom)
        assertEquals("✅", getTestCohesionBadge(analysis.lcom))
    }
    
    @Test
    fun `should calculate LCOM greater than 0 for non-cohesive class`() {
        val kotlinCode = """
            class NonCohesiveClass {
                private val property1: String = ""
                private val property2: Int = 0
                
                fun method1(): String {
                    return property1.uppercase()
                }
                
                fun method2(): Int {
                    return property2 * 2
                }
            }
        """.trimIndent()
        
        val analysis = analyzeKotlinClass(kotlinCode)
        assertTrue(analysis.lcom > 0)
        assertEquals("👍", getTestCohesionBadge(analysis.lcom))
    }
    
    @Test
    fun `should handle class with no methods`() {
        val kotlinCode = """
            class EmptyClass {
                private val property: String = ""
            }
        """.trimIndent()
        
        val analysis = analyzeKotlinClass(kotlinCode)
        assertEquals(0, analysis.lcom)
        assertEquals(0, analysis.methodCount)
        assertEquals(1, analysis.propertyCount)
    }
    
    @Test
    fun `should handle class with no properties`() {
        val kotlinCode = """
            class MethodOnlyClass {
                fun method1(): String {
                    return "hello"
                }
                
                fun method2(): Int {
                    return 42
                }
            }
        """.trimIndent()
        
        val analysis = analyzeKotlinClass(kotlinCode)
        assertEquals(1, analysis.lcom)
        assertEquals(2, analysis.methodCount)
        assertEquals(0, analysis.propertyCount)
    }
    
    @Test
    fun `should handle mixed cohesion scenarios`() {
        val kotlinCode = """
            class MixedCohesionClass {
                private val shared: String = ""
                private val isolated1: Int = 0
                private val isolated2: Boolean = false
                
                fun methodShared1(): String {
                    return shared.uppercase()
                }
                
                fun methodShared2(): Int {
                    return shared.length
                }
                
                fun methodIsolated1(): Int {
                    return isolated1 + 1
                }
                
                fun methodIsolated2(): Boolean {
                    return !isolated2
                }
            }
        """.trimIndent()
        
        val analysis = analyzeKotlinClass(kotlinCode)
        
        // Five pairs don't share properties, one pair shares properties  
        // LCOM = 5 - 1 = 4
        assertEquals(4, analysis.lcom)
        assertEquals(4, analysis.methodCount)
        assertEquals(3, analysis.propertyCount)
        // Should have suggestions for high LCOM value
        assertTrue(analysis.suggestions.isNotEmpty())
    }
    
    private fun analyzeKotlinClass(kotlinCode: String): ClassAnalysis {
        val ktFile = psiFileFactory.createFileFromText(
            "Test.kt",
            KotlinLanguage.INSTANCE,
            kotlinCode
        ) as KtFile
        
        val classOrObject = ktFile.declarations.filterIsInstance<KtClassOrObject>().first()
        return analyzeClassWithNewStructure(classOrObject, "Test.kt")
    }
    
    private fun analyzeClassWithNewStructure(classOrObject: KtClassOrObject, fileName: String): ClassAnalysis {
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
    
    private fun getTestCohesionBadge(lcom: Int): String {
        return when (lcom) {
            0 -> "✅"
            in 1..2 -> "👍"
            in 3..5 -> "⚠️"
            else -> "❌"
        }
    }
    
    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
        Disposer.dispose(disposable)
    }
}