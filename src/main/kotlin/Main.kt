import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.com.intellij.openapi.Disposable
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: run <file.kt>")
        return
    }

    val path = args[0]
    val file = File(path)
    if (!file.exists()) {
        println("File not found: $path")
        return
    }

    val disposable: Disposable = Disposer.newDisposable()
    val configuration = CompilerConfiguration()
    configuration.put(CommonConfigurationKeys.MODULE_NAME, "lcom-analyzer")
    configuration.addJvmClasspathRoot(File("."))
    val env = KotlinCoreEnvironment.createForProduction(
        disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
    )

    val psiFileFactory = PsiFileFactory.getInstance(env.project)
    val ktFile = psiFileFactory.createFileFromText(
        file.name,
        KotlinLanguage.INSTANCE,
        file.readText()
    ) as KtFile

    for (classOrObject in ktFile.declarations.filterIsInstance<KtClassOrObject>()) {
        val className = classOrObject.name ?: continue
        val props = classOrObject.declarations.filterIsInstance<KtProperty>().map { it.name!! }
        val methods = classOrObject.body?.functions ?: emptyList()

        val methodProps = mutableMapOf<String, Set<String>>()

        for (funDecl in methods) {
            val usedProps = mutableSetOf<String>()
            funDecl.bodyExpression?.forEachDescendantOfType<KtNameReferenceExpression> {
                val name = it.getReferencedName()
                if (props.contains(name)) {
                    usedProps.add(name)
                }
            }
            methodProps[funDecl.name ?: "anonymous"] = usedProps
        }

        // Calculate LCOM
        val methodsList = methodProps.entries.toList()
        var pairsWithoutCommon = 0
        var pairsWithCommon = 0

        for (i in methodsList.indices) {
            for (j in i + 1 until methodsList.size) {
                val props1 = methodsList[i].value
                val props2 = methodsList[j].value
                if (props1.intersect(props2).isEmpty()) {
                    pairsWithoutCommon++
                } else {
                    pairsWithCommon++
                }
            }
        }

        var lcom = pairsWithoutCommon - pairsWithCommon
        if (lcom < 0) lcom = 0

        println("Class: $className")
        println("LCOM: $lcom")
    }

    Disposer.dispose(disposable)
}