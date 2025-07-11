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
import java.io.File

class DddPatternDetectionTest {
    
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
    
    private fun createKtFile(content: String, fileName: String = "Test.kt"): KtFile {
        return psiFileFactory.createFileFromText(
            fileName,
            KotlinLanguage.INSTANCE,
            content
        ) as KtFile
    }
    
    @Test
    fun `should detect entity with ID field and mutability`() {
        val entityCode = """
            package test
            import java.util.UUID
            
            data class User(
                val id: UUID,
                val email: String,
                var isActive: Boolean = true
            ) {
                fun activate() {
                    isActive = true
                }
                
                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (other !is User) return false
                    return id == other.id
                }
                
                override fun hashCode(): Int {
                    return id.hashCode()
                }
            }
        """.trimIndent()
        
        val ktFile = createKtFile(entityCode)
        val classOrObject = ktFile.declarations.filterIsInstance<KtClassOrObject>().first()
        
        val entityAnalysis = analyzeEntity(classOrObject, "User", "User.kt")
        
        assertTrue(entityAnalysis.hasUniqueId, "Should detect ID field")
        assertTrue(entityAnalysis.isMutable, "Should detect mutable properties")
        assertTrue(entityAnalysis.confidence > 0.7, "Should have high confidence for entity pattern")
        assertEquals(listOf("id"), entityAnalysis.idFields)
    }
    
    @Test
    fun `should detect value object with immutability and value equality`() {
        val valueObjectCode = """
            package test
            
            data class Email(val value: String) {
                init {
                    require(value.contains("@")) { "Invalid email format" }
                }
                
                fun getDomain(): String {
                    return value.substringAfter("@")
                }
            }
        """.trimIndent()
        
        val ktFile = createKtFile(valueObjectCode)
        val classOrObject = ktFile.declarations.filterIsInstance<KtClassOrObject>().first()
        
        val valueObjectAnalysis = analyzeValueObject(classOrObject, "Email", "Email.kt")
        
        assertTrue(valueObjectAnalysis.isImmutable, "Should detect immutability")
        assertTrue(valueObjectAnalysis.hasValueEquality, "Should detect value equality from data class")
        assertTrue(valueObjectAnalysis.confidence > 0.5, "Should have reasonable confidence for value object pattern")
        assertEquals(listOf("value"), valueObjectAnalysis.properties)
    }
    
    @Test
    fun `should detect service with stateless design and domain logic`() {
        val serviceCode = """
            package test.application
            
            class UserService(
                private val userRepository: UserRepository
            ) {
                fun createUser(email: String, name: String): User {
                    val validatedEmail = Email(email)
                    return userRepository.save(User(email = validatedEmail.value, name = name))
                }
                
                fun validateEmail(email: String): Boolean {
                    return try {
                        Email(email)
                        true
                    } catch (e: IllegalArgumentException) {
                        false
                    }
                }
                
                fun calculateUserScore(user: User): Int {
                    return when {
                        user.isActive -> 100
                        else -> 0
                    }
                }
            }
        """.trimIndent()
        
        val ktFile = createKtFile(serviceCode)
        val classOrObject = ktFile.declarations.filterIsInstance<KtClassOrObject>().first()
        
        val serviceAnalysis = analyzeService(classOrObject, "UserService", "UserService.kt")
        
        assertTrue(serviceAnalysis.isStateless, "Should detect stateless design")
        assertTrue(serviceAnalysis.hasDomainLogic, "Should detect domain logic methods")
        assertTrue(serviceAnalysis.confidence > 0.5, "Should have reasonable confidence for service pattern")
        assertEquals(3, serviceAnalysis.methods.size, "Should detect all methods")
    }
    
    @Test
    fun `should detect repository interface with CRUD operations`() {
        val repositoryCode = """
            package test.domain
            import java.util.UUID
            
            interface UserRepository {
                fun findById(id: UUID): User?
                fun findByEmail(email: String): User?
                fun save(user: User): User
                fun delete(id: UUID)
                fun findAll(): List<User>
                fun count(): Long
                fun exists(id: UUID): Boolean
            }
        """.trimIndent()
        
        val ktFile = createKtFile(repositoryCode)
        val classOrObject = ktFile.declarations.filterIsInstance<KtClassOrObject>().first()
        
        val repositoryAnalysis = analyzeRepository(classOrObject, "UserRepository", "UserRepository.kt")
        
        assertTrue(repositoryAnalysis.isInterface, "Should detect interface")
        assertTrue(repositoryAnalysis.hasDataAccess, "Should detect data access methods")
        assertTrue(repositoryAnalysis.confidence > 0.7, "Should have high confidence for repository pattern")
        assertTrue(repositoryAnalysis.crudMethods.contains("save"), "Should detect save method")
        assertTrue(repositoryAnalysis.crudMethods.contains("findById"), "Should detect find method")
        assertTrue(repositoryAnalysis.crudMethods.contains("delete"), "Should detect delete method")
    }
    
    @Test
    fun `should detect domain event with timestamp and immutability`() {
        val domainEventCode = """
            package test.domain
            import java.time.LocalDateTime
            import java.util.UUID
            
            data class UserCreatedEvent(
                val userId: UUID,
                val email: String,
                val name: String,
                val occurredAt: LocalDateTime = LocalDateTime.now()
            )
        """.trimIndent()
        
        val ktFile = createKtFile(domainEventCode)
        val classOrObject = ktFile.declarations.filterIsInstance<KtClassOrObject>().first()
        
        val eventAnalysis = analyzeDomainEvent(classOrObject, "UserCreatedEvent", "UserCreatedEvent.kt")
        
        assertTrue(eventAnalysis.isEvent, "Should detect event naming pattern")
        assertTrue(eventAnalysis.confidence > 0.6, "Should have reasonable confidence for domain event pattern")
    }
    
    @Test
    fun `should not detect DDD patterns in regular classes`() {
        val regularClassCode = """
            package test
            
            class Calculator {
                fun add(a: Int, b: Int): Int {
                    return a + b
                }
                
                fun multiply(a: Int, b: Int): Int {
                    return a * b
                }
            }
        """.trimIndent()
        
        val ktFile = createKtFile(regularClassCode)
        val classOrObject = ktFile.declarations.filterIsInstance<KtClassOrObject>().first()
        
        val entityAnalysis = analyzeEntity(classOrObject, "Calculator", "Calculator.kt")
        val serviceAnalysis = analyzeService(classOrObject, "Calculator", "Calculator.kt")
        val repositoryAnalysis = analyzeRepository(classOrObject, "Calculator", "Calculator.kt")
        
        assertTrue(entityAnalysis.confidence < 0.3, "Should not detect entity pattern")
        assertTrue(serviceAnalysis.confidence < 0.5, "Should not detect strong service pattern")
        assertTrue(repositoryAnalysis.confidence < 0.3, "Should not detect repository pattern")
    }
    
    @Test
    fun `should detect layer from package naming conventions`() {
        assertEquals("presentation", inferLayer("com.example.presentation", "UserController"))
        assertEquals("presentation", inferLayer("com.example.web", "UserApi"))
        assertEquals("application", inferLayer("com.example.application", "UserService"))
        assertEquals("application", inferLayer("com.example.service", "UserManager"))
        assertEquals("domain", inferLayer("com.example.domain", "User"))
        assertEquals("domain", inferLayer("com.example.model", "User"))
        assertEquals("data", inferLayer("com.example.repository", "UserRepository"))
        assertEquals("data", inferLayer("com.example.data", "UserDao"))
        assertEquals("infrastructure", inferLayer("com.example.infrastructure", "DatabaseConfig"))
        assertEquals("infrastructure", inferLayer("com.example.config", "AppConfig"))
    }
    
    @Test
    fun `should detect layer from class naming conventions`() {
        assertEquals("presentation", inferLayer("com.example", "UserController"))
        assertEquals("presentation", inferLayer("com.example", "UserApi"))
        assertEquals("application", inferLayer("com.example", "UserService"))
        assertEquals("data", inferLayer("com.example", "UserRepository"))
        assertEquals("data", inferLayer("com.example", "UserDao"))
        assertNull(inferLayer("com.example", "RegularClass"))
    }
}