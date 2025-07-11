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
import com.github.javaparser.StaticJavaParser
import java.io.File
import java.nio.file.Path

class MixedLanguageAnalysisTest {
    
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
    fun `should analyze mixed Kotlin and Java project`() {
        // Create Kotlin file
        val kotlinCode = """
            package com.example.service
            
            class UserService {
                private val repository: UserRepository = UserRepositoryImpl()
                
                fun createUser(name: String, email: String): User {
                    if (name.isBlank() || email.isBlank()) {
                        throw IllegalArgumentException("Name and email required")
                    }
                    
                    val user = User()
                    user.name = name
                    user.email = email
                    
                    return repository.save(user)
                }
            }
        """.trimIndent()
        
        // Create Java file
        val javaCode = """
            package com.example.domain;
            
            import javax.persistence.*;
            
            @Entity
            public class User {
                @Id
                private Long id;
                private String name;
                private String email;
                
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
                
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                
                public String getEmail() { return email; }
                public void setEmail(String email) { this.email = email; }
                
                @Override
                public boolean equals(Object obj) {
                    if (this == obj) return true;
                    if (obj == null || getClass() != obj.getClass()) return false;
                    User user = (User) obj;
                    return Objects.equals(id, user.id);
                }
                
                @Override
                public int hashCode() {
                    return Objects.hash(id);
                }
            }
        """.trimIndent()
        
        // Write files to temp directory
        val kotlinFile = tempDir.resolve("UserService.kt").toFile()
        val javaFile = tempDir.resolve("User.java").toFile()
        
        kotlinFile.writeText(kotlinCode)
        javaFile.writeText(javaCode)
        
        // Analyze both files
        val ktFile = psiFileFactory.createFileFromText(
            "UserService.kt",
            KotlinLanguage.INSTANCE,
            kotlinCode
        ) as KtFile
        
        val javaCompilationUnit = StaticJavaParser.parse(javaFile)
        val javaClass = javaCompilationUnit.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration::class.java).first()
        
        // Analyze classes
        val kotlinAnalysis = analyzeClass(ktFile.declarations.filterIsInstance<org.jetbrains.kotlin.psi.KtClassOrObject>().first(), "UserService.kt")
        val javaAnalysis = analyzeJavaClass(javaClass, "User.java")
        
        // Verify analysis results
        assertEquals("UserService", kotlinAnalysis.className)
        assertEquals("User", javaAnalysis.className)
        
        // Both should have reasonable LCOM values
        assertTrue(kotlinAnalysis.lcom >= 0)
        assertTrue(javaAnalysis.lcom >= 0)
        
        // Both should have complexity analysis
        assertNotNull(kotlinAnalysis.complexity)
        assertNotNull(javaAnalysis.complexity)
        
        assertTrue(kotlinAnalysis.complexity.methods.isNotEmpty())
        assertTrue(javaAnalysis.complexity.methods.isNotEmpty())
    }
    
    @Test
    fun `should build mixed dependency graph`() {
        val kotlinCode = """
            package com.example.service
            
            import com.example.domain.User
            
            class UserService {
                fun processUser(user: User): String {
                    return user.getName().uppercase()
                }
            }
        """.trimIndent()
        
        val javaCode = """
            package com.example.domain;
            
            public class User {
                private String name;
                
                public String getName() { 
                    return name; 
                }
                
                public void setName(String name) { 
                    this.name = name; 
                }
            }
        """.trimIndent()
        
        // Create KtFile and Java File
        val ktFile = psiFileFactory.createFileFromText(
            "UserService.kt",
            KotlinLanguage.INSTANCE,
            kotlinCode
        ) as KtFile
        
        val javaFile = tempDir.resolve("User.java").toFile()
        javaFile.writeText(javaCode)
        
        // Build mixed dependency graph
        val dependencyGraph = buildMixedDependencyGraph(listOf(ktFile), listOf(javaFile))
        
        // Verify nodes for both languages
        assertTrue(dependencyGraph.nodes.any { it.language == "Kotlin" && it.className == "UserService" })
        assertTrue(dependencyGraph.nodes.any { it.language == "Java" && it.className == "User" })
        
        // Verify package analysis
        assertTrue(dependencyGraph.packages.any { it.packageName == "com.example.service" })
        assertTrue(dependencyGraph.packages.any { it.packageName == "com.example.domain" })
        
        // Verify edges exist (dependencies between classes)
        assertFalse(dependencyGraph.edges.isEmpty())
    }
    
    @Test
    fun `should analyze DDD patterns across languages`() {
        val kotlinServiceCode = """
            package com.example.service
            
            class PaymentService {
                fun processPayment(amount: Money): PaymentResult {
                    if (amount.isNegative()) {
                        return PaymentResult.failure("Invalid amount")
                    }
                    return PaymentResult.success()
                }
            }
        """.trimIndent()
        
        val javaEntityCode = """
            package com.example.domain;
            
            import javax.persistence.*;
            
            @Entity
            public class Payment {
                @Id
                private UUID id;
                private BigDecimal amount;
                private String status;
                
                public UUID getId() { return id; }
                public void setId(UUID id) { this.id = id; }
                
                public BigDecimal getAmount() { return amount; }
                public void setAmount(BigDecimal amount) { this.amount = amount; }
                
                public String getStatus() { return status; }
                public void setStatus(String status) { this.status = status; }
            }
        """.trimIndent()
        
        val kotlinValueObjectCode = """
            package com.example.domain
            
            data class Money(val amount: BigDecimal, val currency: String) {
                fun isNegative(): Boolean = amount < BigDecimal.ZERO
            }
        """.trimIndent()
        
        // Create files
        val serviceFile = psiFileFactory.createFileFromText(
            "PaymentService.kt",
            KotlinLanguage.INSTANCE,
            kotlinServiceCode
        ) as KtFile
        
        val valueObjectFile = psiFileFactory.createFileFromText(
            "Money.kt",
            KotlinLanguage.INSTANCE,
            kotlinValueObjectCode
        ) as KtFile
        
        val entityFile = tempDir.resolve("Payment.java").toFile()
        entityFile.writeText(javaEntityCode)
        
        // Analyze mixed DDD patterns
        val dddPatterns = analyzeMixedDddPatterns(
            listOf(serviceFile, valueObjectFile), 
            listOf(entityFile), 
            emptyList()
        )
        
        // Verify patterns detected across languages
        assertTrue(dddPatterns.services.any { it.className == "PaymentService" })
        assertTrue(dddPatterns.entities.any { it.className == "Payment" })
        assertTrue(dddPatterns.valueObjects.any { it.className == "Money" })
        
        // Verify confidence levels
        val paymentEntity = dddPatterns.entities.find { it.className == "Payment" }
        assertNotNull(paymentEntity)
        assertTrue(paymentEntity!!.confidence > 0.5)
        
        val moneyValueObject = dddPatterns.valueObjects.find { it.className == "Money" }
        assertNotNull(moneyValueObject)
        assertTrue(moneyValueObject!!.confidence > 0.5)
    }
    
    @Test
    fun `should handle mixed layered architecture analysis`() {
        val controllerCode = """
            package com.example.presentation
            
            class UserController {
                private val userService = UserService()
                
                fun getUser(id: String): UserDto {
                    return userService.findUser(id).toDto()
                }
            }
        """.trimIndent()
        
        val serviceCode = """
            package com.example.application;
            
            import org.springframework.stereotype.Service;
            
            @Service
            public class UserService {
                private final UserRepository repository;
                
                public UserService(UserRepository repository) {
                    this.repository = repository;
                }
                
                public User findUser(String id) {
                    return repository.findById(id);
                }
            }
        """.trimIndent()
        
        // Create files
        val controllerFile = psiFileFactory.createFileFromText(
            "UserController.kt",
            KotlinLanguage.INSTANCE,
            controllerCode
        ) as KtFile
        
        val serviceFile = tempDir.resolve("UserService.java").toFile()
        serviceFile.writeText(serviceCode)
        
        // Build dependency graph and analyze architecture
        val dependencyGraph = buildMixedDependencyGraph(listOf(controllerFile), listOf(serviceFile))
        val architecture = analyzeMixedLayeredArchitecture(listOf(controllerFile), listOf(serviceFile), dependencyGraph)
        
        // Verify layers detected
        val layerNames = architecture.layers.map { it.name }
        assertTrue(layerNames.contains("presentation"))
        assertTrue(layerNames.contains("application"))
        
        // Verify classes assigned to correct layers
        val presentationLayer = architecture.layers.find { it.name == "presentation" }
        val applicationLayer = architecture.layers.find { it.name == "application" }
        
        assertTrue(presentationLayer?.classes?.contains("UserController") == true)
        assertTrue(applicationLayer?.classes?.contains("UserService") == true)
        
        // Verify no architecture violations in this simple case
        assertTrue(architecture.violations.isEmpty())
    }
    
    @org.junit.jupiter.api.AfterEach
    fun tearDown() {
        Disposer.dispose(disposable)
    }
}