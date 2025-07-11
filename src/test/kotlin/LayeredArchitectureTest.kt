import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File

class LayeredArchitectureTest {
    
    private lateinit var disposable: Disposable
    private lateinit var psiFileFactory: PsiFileFactory
    
    @BeforeEach
    fun setup() {
        disposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration()
        configuration.put(CommonConfigurationKeys.MODULE_NAME, "test-module")
        configuration.addJvmClasspathRoot(File("."))
        val env = KotlinCoreEnvironment.createForProduction(
            disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        psiFileFactory = PsiFileFactory.getInstance(env.project)
    }
    
    @AfterEach
    fun teardown() {
        Disposer.dispose(disposable)
    }
    
    private fun createKtFiles(codeMap: Map<String, String>): List<KtFile> {
        return codeMap.map { (fileName, content) ->
            psiFileFactory.createFileFromText(
                fileName,
                KotlinLanguage.INSTANCE,
                content
            ) as KtFile
        }
    }
    
    @Test
    fun `should identify clean architecture pattern`() {
        val codeMap = mapOf(
            "User.kt" to """
                package com.example.domain
                data class User(val id: String, val email: String)
            """.trimIndent(),
            "UserService.kt" to """
                package com.example.application
                import com.example.domain.User
                class UserService {
                    fun createUser(email: String): User {
                        return User("123", email)
                    }
                }
            """.trimIndent(),
            "UserRepository.kt" to """
                package com.example.domain
                interface UserRepository {
                    fun save(user: User): User
                }
            """.trimIndent(),
            "DatabaseUserRepository.kt" to """
                package com.example.infrastructure
                import com.example.domain.User
                import com.example.domain.UserRepository
                class DatabaseUserRepository : UserRepository {
                    override fun save(user: User): User = user
                }
            """.trimIndent(),
            "UserController.kt" to """
                package com.example.presentation
                import com.example.application.UserService
                class UserController(private val userService: UserService)
            """.trimIndent()
        )
        
        val ktFiles = createKtFiles(codeMap)
        val dependencyGraph = buildDependencyGraph(ktFiles)
        val architecture = analyzeLayeredArchitecture(ktFiles, dependencyGraph)
        
        assertEquals(ArchitecturePattern.CLEAN, architecture.pattern)
        assertTrue(architecture.layers.any { it.type == LayerType.DOMAIN })
        assertTrue(architecture.layers.any { it.type == LayerType.APPLICATION })
        assertTrue(architecture.layers.any { it.type == LayerType.INFRASTRUCTURE })
        assertTrue(architecture.layers.any { it.type == LayerType.PRESENTATION })
    }
    
    @Test
    fun `should identify layered architecture pattern`() {
        val codeMap = mapOf(
            "UserController.kt" to """
                package com.example.web
                class UserController
            """.trimIndent(),
            "UserService.kt" to """
                package com.example.service
                class UserService
            """.trimIndent(),
            "UserRepository.kt" to """
                package com.example.repository
                class UserRepository
            """.trimIndent()
        )
        
        val ktFiles = createKtFiles(codeMap)
        val dependencyGraph = buildDependencyGraph(ktFiles)
        val architecture = analyzeLayeredArchitecture(ktFiles, dependencyGraph)
        
        assertEquals(ArchitecturePattern.LAYERED, architecture.pattern)
        assertEquals(3, architecture.layers.size)
    }
    
    @ParameterizedTest
    @CsvSource(
        "presentation, application, true",
        "presentation, domain, true",
        "application, domain, true",
        "domain, infrastructure, true",
        "data, domain, true",
        "infrastructure, domain, true",
        "domain, presentation, false",
        "domain, application, false",
        "application, presentation, false"
    )
    fun `should validate layer dependencies correctly`(fromLayer: String, toLayer: String, expectedValid: Boolean) {
        val isValid = isValidLayerDependency(fromLayer, toLayer)
        assertEquals(expectedValid, isValid, 
            "Dependency from $fromLayer to $toLayer should be ${if (expectedValid) "valid" else "invalid"}")
    }
    
    @Test
    fun `should detect layer violations`() {
        val codeMap = mapOf(
            "User.kt" to """
                package com.example.domain
                import com.example.web.UserController
                class User {
                    fun notifyController(controller: UserController) {
                        // This is a layer violation - domain depending on presentation
                    }
                }
            """.trimIndent(),
            "UserController.kt" to """
                package com.example.web
                class UserController
            """.trimIndent()
        )
        
        val ktFiles = createKtFiles(codeMap)
        val dependencyGraph = buildDependencyGraph(ktFiles)
        val architecture = analyzeLayeredArchitecture(ktFiles, dependencyGraph)
        
        assertTrue(architecture.violations.isNotEmpty(), "Should detect layer violations")
        
        val violation = architecture.violations.find { 
            it.violationType == ViolationType.LAYER_VIOLATION 
        }
        assertNotNull(violation, "Should detect layer violation")
        assertEquals("User", violation?.fromClass)
        assertEquals("UserController", violation?.toClass)
    }
    
    @Test
    fun `should create proper layer hierarchy`() {
        val codeMap = mapOf(
            "UserController.kt" to """
                package com.example.presentation
                class UserController
            """.trimIndent(),
            "UserService.kt" to """
                package com.example.application
                class UserService
            """.trimIndent(),
            "User.kt" to """
                package com.example.domain
                class User
            """.trimIndent(),
            "UserRepository.kt" to """
                package com.example.data
                class UserRepository
            """.trimIndent()
        )
        
        val ktFiles = createKtFiles(codeMap)
        val dependencyGraph = buildDependencyGraph(ktFiles)
        val architecture = analyzeLayeredArchitecture(ktFiles, dependencyGraph)
        
        val presentationLayer = architecture.layers.find { it.type == LayerType.PRESENTATION }
        val applicationLayer = architecture.layers.find { it.type == LayerType.APPLICATION }
        val domainLayer = architecture.layers.find { it.type == LayerType.DOMAIN }
        val dataLayer = architecture.layers.find { it.type == LayerType.DATA }
        
        assertNotNull(presentationLayer, "Should have presentation layer")
        assertNotNull(applicationLayer, "Should have application layer")
        assertNotNull(domainLayer, "Should have domain layer")
        assertNotNull(dataLayer, "Should have data layer")
        
        assertEquals(1, presentationLayer?.level)
        assertEquals(2, applicationLayer?.level)
        assertEquals(3, domainLayer?.level)
        assertEquals(4, dataLayer?.level)
    }
    
    @Test
    fun `should count dependencies between layers correctly`() {
        val codeMap = mapOf(
            "UserController.kt" to """
                package com.example.presentation
                import com.example.application.UserService
                import com.example.domain.User
                class UserController {
                    fun handle(service: UserService): User? = null
                }
            """.trimIndent(),
            "UserService.kt" to """
                package com.example.application
                import com.example.domain.User
                class UserService {
                    fun create(): User? = null
                }
            """.trimIndent(),
            "User.kt" to """
                package com.example.domain
                class User
            """.trimIndent()
        )
        
        val ktFiles = createKtFiles(codeMap)
        val dependencyGraph = buildDependencyGraph(ktFiles)
        val architecture = analyzeLayeredArchitecture(ktFiles, dependencyGraph)
        
        val presentationToApplication = architecture.dependencies.find { 
            it.fromLayer == "presentation" && it.toLayer == "application" 
        }
        val presentationToDomain = architecture.dependencies.find { 
            it.fromLayer == "presentation" && it.toLayer == "domain" 
        }
        val applicationToDomain = architecture.dependencies.find { 
            it.fromLayer == "application" && it.toLayer == "domain" 
        }
        
        assertNotNull(presentationToApplication, "Should detect presentation->application dependency")
        assertNotNull(presentationToDomain, "Should detect presentation->domain dependency")
        assertNotNull(applicationToDomain, "Should detect application->domain dependency")
        
        assertTrue(presentationToApplication?.isValid == true, "Presentation->Application should be valid")
        assertTrue(presentationToDomain?.isValid == true, "Presentation->Domain should be valid")
        assertTrue(applicationToDomain?.isValid == true, "Application->Domain should be valid")
    }
    
    @Test
    fun `should handle unknown layers gracefully`() {
        val codeMap = mapOf(
            "Utility.kt" to """
                package com.example.utils
                class Utility
            """.trimIndent(),
            "Helper.kt" to """
                package com.example.misc
                class Helper
            """.trimIndent()
        )
        
        val ktFiles = createKtFiles(codeMap)
        val dependencyGraph = buildDependencyGraph(ktFiles)
        val architecture = analyzeLayeredArchitecture(ktFiles, dependencyGraph)
        
        assertEquals(ArchitecturePattern.UNKNOWN, architecture.pattern)
        assertTrue(architecture.layers.any { it.name == "unknown" })
        assertTrue(architecture.violations.isEmpty(), "Unknown layers should not cause violations")
    }
    
    @Test
    fun `should determine architecture pattern correctly`() {
        // Test CLEAN architecture
        val cleanLayers = listOf(
            ArchitectureLayer("domain", LayerType.DOMAIN, emptyList(), emptyList(), 3),
            ArchitectureLayer("application", LayerType.APPLICATION, emptyList(), emptyList(), 2),
            ArchitectureLayer("infrastructure", LayerType.INFRASTRUCTURE, emptyList(), emptyList(), 4)
        )
        val cleanDependencies = listOf(
            LayerDependency("application", "domain", 1, true),
            LayerDependency("infrastructure", "domain", 1, true)
        )
        
        val cleanPattern = determineArchitecturePattern(cleanLayers, cleanDependencies)
        assertEquals(ArchitecturePattern.CLEAN, cleanPattern)
        
        // Test LAYERED architecture
        val layeredLayers = listOf(
            ArchitectureLayer("web", LayerType.PRESENTATION, emptyList(), emptyList(), 1),
            ArchitectureLayer("service", LayerType.APPLICATION, emptyList(), emptyList(), 2),
            ArchitectureLayer("data", LayerType.DATA, emptyList(), emptyList(), 3)
        )
        
        val layeredPattern = determineArchitecturePattern(layeredLayers, emptyList())
        assertEquals(ArchitecturePattern.LAYERED, layeredPattern)
        
        // Test UNKNOWN architecture
        val unknownLayers = listOf(
            ArchitectureLayer("misc", LayerType.DOMAIN, emptyList(), emptyList(), 1)
        )
        
        val unknownPattern = determineArchitecturePattern(unknownLayers, emptyList())
        assertEquals(ArchitecturePattern.UNKNOWN, unknownPattern)
    }
}