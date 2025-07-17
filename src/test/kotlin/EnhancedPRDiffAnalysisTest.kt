import com.metrics.analyzer.EnhancedPRDiffAnalyzer
import com.metrics.analyzer.PRDiffAnalysisOptions
import com.metrics.util.DiffParser
import com.metrics.util.FileResolver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test suite for enhanced PR diff analysis functionality.
 */
class EnhancedPRDiffAnalysisTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var projectRoot: File
    private lateinit var analyzer: EnhancedPRDiffAnalyzer
    
    @BeforeEach
    fun setup() {
        projectRoot = tempDir.toFile()
        analyzer = EnhancedPRDiffAnalyzer(projectRoot)
    }
    
    @Test
    fun `test diff parser with basic kotlin file change`() {
        val diffContent = """
            diff --git a/src/main/kotlin/TestClass.kt b/src/main/kotlin/TestClass.kt
            index 1234567..abcdefg 100644
            --- a/src/main/kotlin/TestClass.kt
            +++ b/src/main/kotlin/TestClass.kt
            @@ -1,8 +1,5 @@
             class TestClass {
            -    fun oldMethod() {
            -        if (condition) {
            -            doSomething()
            -        }
            -    }
            +    fun newMethod() = doSomething()
             }
        """.trimIndent()
        
        val diffParser = DiffParser()
        val parsedDiff = diffParser.parse(diffContent)
        
        assertEquals(1, parsedDiff.fileChanges.size)
        val fileChange = parsedDiff.fileChanges[0]
        assertEquals("src/main/kotlin/TestClass.kt", fileChange.originalPath)
        assertEquals("src/main/kotlin/TestClass.kt", fileChange.newPath)
        assertTrue(fileChange.isKotlinFile())
        assertEquals(1, fileChange.hunks.size)
    }
    
    @Test
    fun `test file resolver with kotlin file`() {
        // Create a test Kotlin file
        val testFile = File(projectRoot, "src/main/kotlin/TestClass.kt")
        testFile.parentFile.mkdirs()
        testFile.writeText("""
            class TestClass {
                fun testMethod() {
                    println("Hello, World!")
                }
            }
        """.trimIndent())
        
        val fileResolver = FileResolver(projectRoot.toPath())
        val resolvedFile = fileResolver.resolveFile("src/main/kotlin/TestClass.kt")
        
        assertNotNull(resolvedFile)
        assertTrue(resolvedFile.exists())
        assertEquals("TestClass.kt", resolvedFile.name)
    }
    
    @Test
    fun `test file resolver finds source files`() {
        // Create test files
        val kotlinFile = File(projectRoot, "src/main/kotlin/KotlinClass.kt")
        kotlinFile.parentFile.mkdirs()
        kotlinFile.writeText("class KotlinClass")
        
        val javaFile = File(projectRoot, "src/main/java/JavaClass.java")
        javaFile.parentFile.mkdirs()
        javaFile.writeText("public class JavaClass {}")
        
        val fileResolver = FileResolver(projectRoot.toPath())
        val sourceFiles = fileResolver.findSourceFiles(listOf("kt", "java"))
        
        assertEquals(2, sourceFiles.size)
        assertTrue(sourceFiles.any { it.name == "KotlinClass.kt" })
        assertTrue(sourceFiles.any { it.name == "JavaClass.java" })
    }
    
    @Test
    fun `test enhanced PR diff analysis with basic change`() {
        // Create source files
        val originalFile = File(projectRoot, "src/main/kotlin/TestService.kt")
        originalFile.parentFile.mkdirs()
        originalFile.writeText("""
            class TestService {
                fun complexMethod() {
                    if (condition1) {
                        if (condition2) {
                            if (condition3) {
                                doSomething()
                            }
                        }
                    }
                }
            }
        """.trimIndent())
        
        // Create diff file
        val diffFile = File(projectRoot, "test.diff")
        diffFile.writeText("""
            diff --git a/src/main/kotlin/TestService.kt b/src/main/kotlin/TestService.kt
            index 1234567..abcdefg 100644
            --- a/src/main/kotlin/TestService.kt
            +++ b/src/main/kotlin/TestService.kt
            @@ -1,10 +1,5 @@
             class TestService {
            -    fun complexMethod() {
            -        if (condition1) {
            -            if (condition2) {
            -                if (condition3) {
            -                    doSomething()
            -                }
            -            }
            -        }
            -    }
            +    fun simplifiedMethod() = doSomething()
             }
        """.trimIndent())
        
        val options = PRDiffAnalysisOptions(
            includeTests = false,
            contextLines = 3,
            ignoreWhitespace = true
        )
        
        val result = analyzer.analyzePRDiff(diffFile, options)
        
        assertNotNull(result)
        assertTrue(result.resolvedFiles.isNotEmpty())
        assertNotNull(result.afterAnalysis)
        assertNotNull(result.semanticChanges)
        assertNotNull(result.metricsComparison)
    }
    
    @Test
    fun `test analysis options are respected`() {
        val options = PRDiffAnalysisOptions(
            includeTests = true,
            contextLines = 5,
            ignoreWhitespace = false,
            focusOnComplexity = true,
            minImprovementThreshold = 10.0
        )
        
        assertEquals(true, options.includeTests)
        assertEquals(5, options.contextLines)
        assertEquals(false, options.ignoreWhitespace)
        assertEquals(true, options.focusOnComplexity)
        assertEquals(10.0, options.minImprovementThreshold)
    }
    
    @Test
    fun `test performance with large diff`() {
        // Create a large diff file to test performance
        val diffContent = StringBuilder()
        diffContent.append("diff --git a/src/main/kotlin/LargeFile.kt b/src/main/kotlin/LargeFile.kt\n")
        diffContent.append("index 1234567..abcdefg 100644\n")
        diffContent.append("--- a/src/main/kotlin/LargeFile.kt\n")
        diffContent.append("+++ b/src/main/kotlin/LargeFile.kt\n")
        
        // Generate a large number of changes
        for (i in 1..1000) {
            diffContent.append("@@ -$i,3 +$i,3 @@\n")
            diffContent.append(" fun method$i() {\n")
            diffContent.append("-    oldImplementation$i()\n")
            diffContent.append("+    newImplementation$i()\n")
            diffContent.append(" }\n")
        }
        
        val diffFile = File(projectRoot, "large.diff")
        diffFile.writeText(diffContent.toString())
        
        val startTime = System.currentTimeMillis()
        val diffParser = DiffParser()
        val parsedDiff = diffParser.parse(diffFile)
        val endTime = System.currentTimeMillis()
        
        // Should parse within reasonable time (less than 1 second)
        assertTrue(endTime - startTime < 1000)
        assertTrue(parsedDiff.fileChanges.isNotEmpty())
    }
}