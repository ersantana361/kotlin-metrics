package com.metrics.parser.kotlin

import com.metrics.parser.SourceCodeParser
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

/**
 * Parser for Kotlin source code files.
 * Uses the Kotlin compiler's PSI (Program Structure Interface) for parsing.
 */
class KotlinFileParser : SourceCodeParser<KtFile> {
    
    private val disposable: Disposable = Disposer.newDisposable()
    private val psiFileFactory: PsiFileFactory
    
    init {
        val configuration = CompilerConfiguration()
        configuration.put(CommonConfigurationKeys.MODULE_NAME, "kotlin-metrics")
        val env = KotlinCoreEnvironment.createForProduction(
            disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        psiFileFactory = PsiFileFactory.getInstance(env.project)
    }
    
    override fun parse(file: File): KtFile? {
        return try {
            val content = file.readText()
            psiFileFactory.createFileFromText(
                file.name,
                KotlinLanguage.INSTANCE,
                content
            ) as? KtFile
        } catch (e: Exception) {
            println("Error parsing Kotlin file ${file.name}: ${e.message}")
            null
        }
    }
    
    override fun canParse(file: File): Boolean {
        return file.extension.equals("kt", ignoreCase = true)
    }
    
    /**
     * Cleanup resources when parser is no longer needed.
     */
    fun dispose() {
        Disposer.dispose(disposable)
    }
}