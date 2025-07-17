package com.metrics.util

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

/**
 * Loads and parses source files for complete context analysis.
 * Handles both Kotlin and Java files with proper AST parsing.
 */
class SourceContextLoader {
    
    private val kotlinEnvironment: KotlinCoreEnvironment
    private val javaParser: JavaParser
    
    init {
        val disposable = Disposer.newDisposable()
        kotlinEnvironment = KotlinCoreEnvironment.createForProduction(
            disposable,
            CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        javaParser = JavaParser()
    }
    
    /**
     * Loads complete source context for a file.
     * 
     * @param file The source file to load
     * @return SourceContext containing parsed AST and metadata
     */
    fun loadSourceContext(file: File): SourceContext? {
        return try {
            when (file.extension.lowercase()) {
                "kt" -> loadKotlinContext(file)
                "java" -> loadJavaContext(file)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Loads multiple source files and returns their contexts.
     * 
     * @param files List of source files to load
     * @return Map of file paths to their source contexts
     */
    fun loadMultipleContexts(files: List<File>): Map<String, SourceContext> {
        val contexts = mutableMapOf<String, SourceContext>()
        
        files.forEach { file ->
            loadSourceContext(file)?.let { context ->
                contexts[file.path] = context
            }
        }
        
        return contexts
    }
    
    /**
     * Extracts method signatures from a source context.
     * 
     * @param context The source context to analyze
     * @return List of method signatures in the file
     */
    fun extractMethodSignatures(context: SourceContext): List<MethodSignature> {
        return when (context.language) {
            SourceLanguage.KOTLIN -> extractKotlinMethodSignatures(context)
            SourceLanguage.JAVA -> extractJavaMethodSignatures(context)
        }
    }
    
    /**
     * Extracts class dependencies from a source context.
     * 
     * @param context The source context to analyze
     * @return List of class dependencies (imports and references)
     */
    fun extractDependencies(context: SourceContext): List<ClassDependency> {
        return when (context.language) {
            SourceLanguage.KOTLIN -> extractKotlinDependencies(context)
            SourceLanguage.JAVA -> extractJavaDependencies(context)
        }
    }
    
    private fun loadKotlinContext(file: File): SourceContext? {
        val content = file.readText()
        val virtualFile = LightVirtualFile(file.name, KotlinFileType.INSTANCE, content)
        val psiManager = PsiManager.getInstance(kotlinEnvironment.project)
        val ktFile = psiManager.findFile(virtualFile) as? KtFile
        
        return ktFile?.let { 
            SourceContext(
                file = file,
                language = SourceLanguage.KOTLIN,
                content = content,
                kotlinFile = it,
                javaFile = null
            )
        }
    }
    
    private fun loadJavaContext(file: File): SourceContext? {
        val content = file.readText()
        val parseResult = javaParser.parse(content)
        
        return if (parseResult.isSuccessful) {
            SourceContext(
                file = file,
                language = SourceLanguage.JAVA,
                content = content,
                kotlinFile = null,
                javaFile = parseResult.result.orElse(null)
            )
        } else {
            null
        }
    }
    
    private fun extractKotlinMethodSignatures(context: SourceContext): List<MethodSignature> {
        val signatures = mutableListOf<MethodSignature>()
        val ktFile = context.kotlinFile ?: return signatures
        
        ktFile.accept(object : org.jetbrains.kotlin.psi.KtVisitorVoid() {
            override fun visitNamedFunction(function: org.jetbrains.kotlin.psi.KtNamedFunction) {
                val name = function.name ?: ""
                val parameters = function.valueParameters.map { param ->
                    ParameterInfo(
                        name = param.name ?: "",
                        type = param.typeReference?.text ?: "",
                        hasDefault = param.hasDefaultValue()
                    )
                }
                val returnType = function.typeReference?.text ?: "Unit"
                val visibility = "public" // Simplified for now
                
                signatures.add(MethodSignature(
                    name = name,
                    parameters = parameters,
                    returnType = returnType,
                    visibility = visibility,
                    isStatic = false,
                    className = findContainingClassName(function)
                ))
                
                super.visitNamedFunction(function)
            }
        })
        
        return signatures
    }
    
    private fun extractJavaMethodSignatures(context: SourceContext): List<MethodSignature> {
        val signatures = mutableListOf<MethodSignature>()
        val javaFile = context.javaFile ?: return signatures
        
        javaFile.accept(object : com.github.javaparser.ast.visitor.VoidVisitorAdapter<Unit>() {
            override fun visit(method: com.github.javaparser.ast.body.MethodDeclaration, arg: Unit?) {
                val name = method.nameAsString
                val parameters = method.parameters.map { param ->
                    ParameterInfo(
                        name = param.nameAsString,
                        type = param.typeAsString,
                        hasDefault = false
                    )
                }
                val returnType = method.typeAsString
                val visibility = method.accessSpecifier.toString().lowercase()
                val isStatic = method.isStatic
                
                signatures.add(MethodSignature(
                    name = name,
                    parameters = parameters,
                    returnType = returnType,
                    visibility = visibility,
                    isStatic = isStatic,
                    className = findContainingClassName(method)
                ))
                
                super.visit(method, arg)
            }
        }, Unit)
        
        return signatures
    }
    
    private fun extractKotlinDependencies(context: SourceContext): List<ClassDependency> {
        val dependencies = mutableListOf<ClassDependency>()
        val ktFile = context.kotlinFile ?: return dependencies
        
        // Extract imports
        ktFile.importDirectives.forEach { import ->
            val importedName = import.importedFqName?.asString() ?: ""
            if (importedName.isNotEmpty()) {
                dependencies.add(ClassDependency(
                    type = DependencyType.IMPORT,
                    className = importedName,
                    location = "import"
                ))
            }
        }
        
        return dependencies
    }
    
    private fun extractJavaDependencies(context: SourceContext): List<ClassDependency> {
        val dependencies = mutableListOf<ClassDependency>()
        val javaFile = context.javaFile ?: return dependencies
        
        // Extract imports
        javaFile.imports.forEach { import ->
            dependencies.add(ClassDependency(
                type = DependencyType.IMPORT,
                className = import.nameAsString,
                location = "import"
            ))
        }
        
        return dependencies
    }
    
    private fun findContainingClassName(function: org.jetbrains.kotlin.psi.KtNamedFunction): String {
        var parent = function.parent
        while (parent != null) {
            if (parent is org.jetbrains.kotlin.psi.KtClass) {
                return parent.name ?: ""
            }
            if (parent is org.jetbrains.kotlin.psi.KtObjectDeclaration) {
                return parent.name ?: ""
            }
            parent = parent.parent
        }
        return ""
    }
    
    private fun findContainingClassName(method: com.github.javaparser.ast.body.MethodDeclaration): String {
        var parent = method.parentNode.orElse(null)
        while (parent != null) {
            if (parent is com.github.javaparser.ast.body.ClassOrInterfaceDeclaration) {
                return parent.nameAsString
            }
            parent = parent.parentNode.orElse(null)
        }
        return ""
    }
}

/**
 * Represents the complete source context for a file.
 */
data class SourceContext(
    val file: File,
    val language: SourceLanguage,
    val content: String,
    val kotlinFile: KtFile?,
    val javaFile: CompilationUnit?
)

/**
 * Represents a method signature extracted from source code.
 */
data class MethodSignature(
    val name: String,
    val parameters: List<ParameterInfo>,
    val returnType: String,
    val visibility: String,
    val isStatic: Boolean,
    val className: String
)

/**
 * Represents a method parameter.
 */
data class ParameterInfo(
    val name: String,
    val type: String,
    val hasDefault: Boolean
)

/**
 * Represents a class dependency.
 */
data class ClassDependency(
    val type: DependencyType,
    val className: String,
    val location: String
)

/**
 * Language of the source file.
 */
enum class SourceLanguage {
    KOTLIN,
    JAVA
}

/**
 * Type of dependency between classes.
 */
enum class DependencyType {
    IMPORT,
    INHERITANCE,
    COMPOSITION,
    USAGE
}