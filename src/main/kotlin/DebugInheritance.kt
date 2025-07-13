import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import com.metrics.util.InheritanceCalculator
import java.io.File

object DebugInheritance {
    @JvmStatic
    fun main(args: Array<String>) {
    println("=== Debug Inheritance Calculations ===")
    
    // Setup Kotlin environment (same as test files)
    val disposable = Disposer.newDisposable()
    val configuration = CompilerConfiguration()
    configuration.put(CommonConfigurationKeys.MODULE_NAME, "debug-inheritance")
    val env = KotlinCoreEnvironment.createForProduction(
        disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
    )
    val psiFileFactory = PsiFileFactory.getInstance(env.project)
    
    try {
        // Read the actual test files
        val userRepositoryFile = File("test-project/src/main/kotlin/com/example/domain/UserRepository.kt")
        val databaseUserRepositoryFile = File("test-project/src/main/kotlin/com/example/infrastructure/DatabaseUserRepository.kt")
        
        if (!userRepositoryFile.exists()) {
            println("ERROR: UserRepository.kt not found at ${userRepositoryFile.absolutePath}")
            return
        }
        
        if (!databaseUserRepositoryFile.exists()) {
            println("ERROR: DatabaseUserRepository.kt not found at ${databaseUserRepositoryFile.absolutePath}")
            return
        }
        
        // Read file contents
        val userRepositoryContent = userRepositoryFile.readText()
        val databaseUserRepositoryContent = databaseUserRepositoryFile.readText()
        
        println("Files found:")
        println("- UserRepository.kt (${userRepositoryContent.length} chars)")
        println("- DatabaseUserRepository.kt (${databaseUserRepositoryContent.length} chars)")
        println()
        
        // Parse files with PSI
        val userRepositoryKtFile = psiFileFactory.createFileFromText(
            "UserRepository.kt", 
            KotlinLanguage.INSTANCE, 
            userRepositoryContent
        ) as KtFile
        
        val databaseUserRepositoryKtFile = psiFileFactory.createFileFromText(
            "DatabaseUserRepository.kt", 
            KotlinLanguage.INSTANCE, 
            databaseUserRepositoryContent
        ) as KtFile
        
        // Extract classes
        val userRepositoryClass = userRepositoryKtFile.declarations.filterIsInstance<KtClassOrObject>().firstOrNull()
        val databaseUserRepositoryClass = databaseUserRepositoryKtFile.declarations.filterIsInstance<KtClassOrObject>().firstOrNull()
        
        if (userRepositoryClass == null) {
            println("ERROR: Could not find UserRepository class in parsed file")
            return
        }
        
        if (databaseUserRepositoryClass == null) {
            println("ERROR: Could not find DatabaseUserRepository class in parsed file")
            return
        }
        
        println("Classes extracted:")
        println("- UserRepository: ${userRepositoryClass.name} (${userRepositoryClass::class.simpleName})")
        println("- DatabaseUserRepository: ${databaseUserRepositoryClass.name} (${databaseUserRepositoryClass::class.simpleName})")
        println()
        
        // Analyze inheritance relationships
        println("=== Inheritance Analysis ===")
        
        // Check supertype information for DatabaseUserRepository
        println("DatabaseUserRepository supertype analysis:")
        val superTypes = databaseUserRepositoryClass.superTypeListEntries
        println("- Number of supertypes: ${superTypes.size}")
        superTypes.forEachIndexed { index, superType ->
            println("  [$index] ${superType.text}")
            println("      typeAsUserType: ${superType.typeAsUserType}")
            println("      referencedName: ${superType.typeAsUserType?.referencedName}")
        }
        println()
        
        // Check if UserRepository is an interface
        println("UserRepository analysis:")
        if (userRepositoryClass is KtClass) {
            println("- Is interface: ${userRepositoryClass.isInterface()}")
            println("- Has abstract modifier: ${userRepositoryClass.hasModifier(org.jetbrains.kotlin.lexer.KtTokens.ABSTRACT_KEYWORD)}")
        }
        println()
        
        // Test calculations with both classes
        val allClasses = listOf(userRepositoryClass, databaseUserRepositoryClass)
        
        println("=== DIT Calculations ===")
        val userRepositoryDit = InheritanceCalculator.calculateDit(userRepositoryClass, allClasses)
        val databaseUserRepositoryDit = InheritanceCalculator.calculateDit(databaseUserRepositoryClass, allClasses)
        
        println("UserRepository DIT: $userRepositoryDit")
        println("DatabaseUserRepository DIT: $databaseUserRepositoryDit")
        println()
        
        println("=== NOC Calculations ===")
        val userRepositoryNoc = InheritanceCalculator.calculateNoc(userRepositoryClass, allClasses)
        val databaseUserRepositoryNoc = InheritanceCalculator.calculateNoc(databaseUserRepositoryClass, allClasses)
        
        println("UserRepository NOC: $userRepositoryNoc")
        println("DatabaseUserRepository NOC: $databaseUserRepositoryNoc")
        println()
        
        println("=== Expected vs Actual ===")
        println("Expected:")
        println("- DatabaseUserRepository DIT: 1 (implements UserRepository)")
        println("- UserRepository NOC: 1 (implemented by DatabaseUserRepository)")
        println()
        println("Actual:")
        println("- DatabaseUserRepository DIT: $databaseUserRepositoryDit")
        println("- UserRepository NOC: $userRepositoryNoc")
        println()
        
        // Test specific inheritance checks
        println("=== Direct Inheritance Check ===")
        val directInheritance = databaseUserRepositoryClass.superTypeListEntries.any { superType ->
            superType.typeAsUserType?.referencedName == "UserRepository"
        }
        println("DatabaseUserRepository directly inherits from UserRepository: $directInheritance")
        
        // Check NOC calculation manually
        println("\n=== Manual NOC Check ===")
        val manualNoc = allClasses.count { otherClass ->
            otherClass != userRepositoryClass && 
            otherClass.superTypeListEntries.any { 
                it.typeAsUserType?.referencedName == "UserRepository" 
            }
        }
        println("Manual NOC calculation for UserRepository: $manualNoc")
        
        println("\n=== Assessment ===")
        println("DIT Assessment - DatabaseUserRepository: ${InheritanceCalculator.getDitAssessment(databaseUserRepositoryDit)}")
        println("NOC Assessment - UserRepository: ${InheritanceCalculator.getNocAssessment(userRepositoryNoc)}")
        
    } catch (e: Exception) {
        println("ERROR: ${e.message}")
        e.printStackTrace()
    } finally {
        Disposer.dispose(disposable)
    }
    }
}