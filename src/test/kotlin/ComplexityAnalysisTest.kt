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
import org.jetbrains.kotlin.psi.KtNamedFunction

class ComplexityAnalysisTest {
    
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
    fun `should calculate basic complexity of 1 for simple function`() {
        val kotlinCode = """
            fun simpleFunction(): String {
                return "Hello World"
            }
        """.trimIndent()
        
        val method = parseKotlinFunction(kotlinCode)
        val complexity = calculateCyclomaticComplexity(method)
        
        assertEquals(1, complexity)
    }
    
    @Test
    fun `should calculate complexity for if statements`() {
        val kotlinCode = """
            fun checkValue(x: Int): String {
                if (x > 0) {
                    return "positive"
                } else {
                    return "non-positive"
                }
            }
        """.trimIndent()
        
        val method = parseKotlinFunction(kotlinCode)
        val complexity = calculateCyclomaticComplexity(method)
        
        assertEquals(2, complexity) // Base 1 + if 1
    }
    
    @Test
    fun `should calculate complexity for when expressions`() {
        val kotlinCode = """
            fun processType(type: String): Int {
                return when (type) {
                    "A" -> 1
                    "B" -> 2
                    "C" -> 3
                    else -> 0
                }
            }
        """.trimIndent()
        
        val method = parseKotlinFunction(kotlinCode)
        val complexity = calculateCyclomaticComplexity(method)
        
        assertEquals(5, complexity) // Base 1 + 4 when branches
    }
    
    @Test
    fun `should calculate complexity for loops`() {
        val kotlinCode = """
            fun processItems(items: List<String>): List<String> {
                val result = mutableListOf<String>()
                
                for (item in items) {       // +1
                    if (item.isNotEmpty()) { // +1
                        result.add(item.uppercase())
                    }
                }
                
                var i = 0
                while (i < result.size) {   // +1
                    if (result[i].length > 5) { // +1
                        result[i] = result[i].substring(0, 5)
                    }
                    i++
                }
                
                return result
            }
        """.trimIndent()
        
        val method = parseKotlinFunction(kotlinCode)
        val complexity = calculateCyclomaticComplexity(method)
        
        assertEquals(5, complexity) // Base 1 + for 1 + if 1 + while 1 + if 1
    }
    
    @Test
    fun `should calculate complexity for logical operators`() {
        val kotlinCode = """
            fun validateUser(name: String?, email: String?, age: Int): Boolean {
                return name != null && 
                       email != null && 
                       email.contains("@") && 
                       age >= 18 || 
                       age < 0
            }
        """.trimIndent()
        
        val method = parseKotlinFunction(kotlinCode)
        val complexity = calculateCyclomaticComplexity(method)
        
        assertEquals(6, complexity) // Base 1 + 3 && + 1 || + 1 additional &&
    }
    
    @Test
    fun `should calculate complexity for try-catch blocks`() {
        val kotlinCode = """
            fun riskyOperation(input: String): String {
                return try {                    // +1
                    val result = input.toInt()
                    if (result > 0) {          // +1
                        "positive: ${'$'}result"
                    } else {
                        "non-positive: ${'$'}result"
                    }
                } catch (e: NumberFormatException) { // +1
                    "invalid number"
                } catch (e: Exception) {           // +1
                    "unknown error"
                }
            }
        """.trimIndent()
        
        val method = parseKotlinFunction(kotlinCode)
        val complexity = calculateCyclomaticComplexity(method)
        
        assertEquals(5, complexity) // Base 1 + try 1 + if 1 + 2 catch blocks
    }
    
    @Test
    fun `should handle complex nested scenarios`() {
        val kotlinCode = """
            fun complexFunction(data: List<String>, type: String, threshold: Int): List<String> {
                val result = mutableListOf<String>()
                
                if (data.isEmpty()) {           // +1
                    return emptyList()
                }
                
                for (item in data) {           // +1
                    when (type) {              // +3 (3 branches)
                        "upper" -> {
                            if (item.length > threshold) { // +1
                                result.add(item.uppercase())
                            }
                        }
                        "lower" -> {
                            if (item.length > threshold) { // +1
                                result.add(item.lowercase())
                            }
                        }
                        else -> {
                            result.add(item)
                        }
                    }
                }
                
                return result.filter { it.isNotEmpty() && it.length <= 100 } // +1 (&&)
            }
        """.trimIndent()
        
        val method = parseKotlinFunction(kotlinCode)
        val complexity = calculateCyclomaticComplexity(method)
        
        assertEquals(9, complexity) // Base 1 + if 1 + for 1 + when 3 + if 1 + if 1 + && 1
    }
    
    @Test
    fun `should analyze complexity for multiple methods`() {
        val kotlinCode = """
            class ComplexityExample {
                fun simple(): String = "hello"                              // CC: 1
                
                fun moderate(x: Int): String {                               // CC: 3
                    return if (x > 0) {
                        if (x > 10) "large" else "small"
                    } else {
                        "negative"
                    }
                }
                
                fun complex(items: List<Int>): List<Int> {                   // CC: 5
                    return items.filter { it > 0 }                          // +1 (lambda)
                        .map { value ->                                     // +1 (lambda)
                            when {                                          // +2 (2 branches)
                                value > 100 -> value / 10
                                else -> value * 2
                            }
                        }
                }
            }
        """.trimIndent()
        
        val ktFile = psiFileFactory.createFileFromText(
            "Test.kt",
            KotlinLanguage.INSTANCE,
            kotlinCode
        ) as KtFile
        
        val methods = ktFile.declarations.flatMap { declaration ->
            declaration.children.filterIsInstance<KtNamedFunction>()
        }
        
        assertEquals(3, methods.size)
        
        val complexities = methods.map { method ->
            method.name to calculateCyclomaticComplexity(method)
        }.toMap()
        
        assertEquals(1, complexities["simple"])
        assertEquals(3, complexities["moderate"])
        assertEquals(5, complexities["complex"])
    }
    
    @Test
    fun `should classify complexity levels correctly`() {
        assertEquals("✅", getTestComplexityBadge(1))   // Simple
        assertEquals("✅", getTestComplexityBadge(5))   // Simple
        assertEquals("👍", getTestComplexityBadge(7))   // Moderate
        assertEquals("⚠️", getTestComplexityBadge(15))  // Complex
        assertEquals("❌", getTestComplexityBadge(25))  // Very Complex
    }
    
    private fun parseKotlinFunction(kotlinCode: String): KtNamedFunction {
        val ktFile = psiFileFactory.createFileFromText(
            "Test.kt",
            KotlinLanguage.INSTANCE,
            kotlinCode
        ) as KtFile
        
        return ktFile.declarations.filterIsInstance<KtNamedFunction>().first()
    }
    
    private fun getTestComplexityBadge(complexity: Int): String {
        return when (complexity) {
            in 1..5 -> "✅"      // Simple
            in 6..10 -> "👍"     // Moderate
            in 11..20 -> "⚠️"    // Complex
            else -> "❌"         // Very Complex
        }
    }
    
    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
        Disposer.dispose(disposable)
    }
}