import com.metrics.model.analysis.*
import com.metrics.model.architecture.*
import com.metrics.model.common.*
import com.metrics.analyzer.core.KotlinCodeAnalyzer
import com.metrics.analyzer.core.JavaCodeAnalyzer
import com.metrics.parser.MultiLanguageParser
import com.metrics.report.console.ConsoleReportGenerator
import com.metrics.report.html.HtmlReportGenerator
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
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.*
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun main() {
    val currentDir = File(".")
    println("Analyzing Kotlin and Java files in: ${currentDir.absolutePath}")
    
    val target = currentDir

    val kotlinFiles = if (target.isDirectory()) {
        target.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
    } else if (target.extension == "kt") {
        listOf(target)
    } else {
        emptyList()
    }
    
    val javaFiles = if (target.isDirectory()) {
        target.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()
    } else if (target.extension == "java") {
        listOf(target)
    } else {
        emptyList()
    }

    println("Found ${kotlinFiles.size} Kotlin files and ${javaFiles.size} Java files")

    val disposable: Disposable = Disposer.newDisposable()
    val configuration = CompilerConfiguration()
    configuration.put(CommonConfigurationKeys.MODULE_NAME, "kotlin-metrics")
    configuration.addJvmClasspathRoot(File("."))
    val env = KotlinCoreEnvironment.createForProduction(
        disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
    )

    val psiFileFactory = PsiFileFactory.getInstance(env.project)
    val analyses = mutableListOf<ClassAnalysis>()

    val allKtFiles = mutableListOf<KtFile>()
    
    // Process Kotlin files
    for (file in kotlinFiles) {
        val ktFile = psiFileFactory.createFileFromText(
            file.name,
            KotlinLanguage.INSTANCE,
            file.readText()
        ) as KtFile
        allKtFiles.add(ktFile)

        for (classOrObject in ktFile.declarations.filterIsInstance<KtClassOrObject>()) {
            val analysis = analyzeClass(classOrObject, file.name)
            analyses.add(analysis)
        }
    }
    
    // Process Java files
    for (file in javaFiles) {
        try {
            val cu = StaticJavaParser.parse(file)
            cu.findAll(ClassOrInterfaceDeclaration::class.java).forEach { classDecl ->
                val analysis = analyzeJavaClass(classDecl, file.name)
                analyses.add(analysis)
            }
        } catch (e: Exception) {
            println("Error parsing Java file ${file.name}: ${e.message}")
        }
    }

    val architectureAnalysis = if (javaFiles.isNotEmpty()) {
        analyzeArchitectureWithJava(allKtFiles, javaFiles, analyses)
    } else {
        analyzeArchitecture(allKtFiles, analyses)
    }
    generateSummary(analyses, architectureAnalysis)
    generateHtmlReport(analyses, architectureAnalysis)

    Disposer.dispose(disposable)
}

fun analyzeArchitecture(ktFiles: List<KtFile>, classAnalyses: List<ClassAnalysis>): ArchitectureAnalysis {
    val dependencyGraph = buildDependencyGraph(ktFiles)
    val dddPatterns = analyzeDddPatterns(ktFiles, classAnalyses)
    val layeredArchitecture = analyzeLayeredArchitecture(ktFiles, dependencyGraph)
    
    return ArchitectureAnalysis(
        dddPatterns = dddPatterns,
        layeredArchitecture = layeredArchitecture,
        dependencyGraph = dependencyGraph
    )
}

// Extended architecture analysis supporting both Kotlin and Java
fun analyzeArchitectureWithJava(ktFiles: List<KtFile>, javaFiles: List<File>, classAnalyses: List<ClassAnalysis>): ArchitectureAnalysis {
    val dependencyGraph = buildMixedDependencyGraph(ktFiles, javaFiles)
    val dddPatterns = analyzeMixedDddPatterns(ktFiles, javaFiles, classAnalyses)
    val layeredArchitecture = analyzeMixedLayeredArchitecture(ktFiles, javaFiles, dependencyGraph)
    
    return ArchitectureAnalysis(
        dddPatterns = dddPatterns,
        layeredArchitecture = layeredArchitecture,
        dependencyGraph = dependencyGraph
    )
}

// Mixed language analysis functions
fun buildMixedDependencyGraph(ktFiles: List<KtFile>, javaFiles: List<File>): DependencyGraph {
    val nodes = mutableListOf<DependencyNode>()
    val edges = mutableListOf<DependencyEdge>()
    val packages = mutableMapOf<String, MutableList<String>>()
    
    // Process Kotlin files
    for (ktFile in ktFiles) {
        val packageName = ktFile.packageFqName.asString()
        
        for (classOrObject in ktFile.declarations.filterIsInstance<KtClassOrObject>()) {
            val className = classOrObject.name ?: "Anonymous"
            val nodeType = when {
                classOrObject is KtClass && classOrObject.isInterface() -> NodeType.INTERFACE
                classOrObject is KtClass && classOrObject.isAbstract() -> NodeType.ABSTRACT_CLASS
                classOrObject is KtObjectDeclaration -> NodeType.OBJECT
                classOrObject is KtEnumEntry -> NodeType.ENUM
                else -> NodeType.CLASS
            }
            
            val node = DependencyNode(
                id = "$packageName.$className",
                className = className,
                fileName = ktFile.name,
                packageName = packageName,
                nodeType = nodeType,
                layer = inferLayer(packageName, className),
                language = "Kotlin"
            )
            nodes.add(node)
            
            packages.getOrPut(packageName) { mutableListOf() }.add(className)
        }
    }
    
    // Process Java files
    for (javaFile in javaFiles) {
        try {
            val cu = StaticJavaParser.parse(javaFile)
            val packageName = cu.packageDeclaration.map { it.nameAsString }.orElse("")
            
            cu.findAll(ClassOrInterfaceDeclaration::class.java).forEach { classDecl ->
                val className = classDecl.nameAsString
                val nodeType = when {
                    classDecl.isInterface -> NodeType.INTERFACE
                    classDecl.isAbstract -> NodeType.ABSTRACT_CLASS
                    else -> NodeType.CLASS
                }
                
                val node = DependencyNode(
                    id = "$packageName.$className",
                    className = className,
                    fileName = javaFile.name,
                    packageName = packageName,
                    nodeType = nodeType,
                    layer = inferLayer(packageName, className),
                    language = "Java"
                )
                nodes.add(node)
                
                packages.getOrPut(packageName) { mutableListOf() }.add(className)
            }
            
            cu.findAll(EnumDeclaration::class.java).forEach { enumDecl ->
                val className = enumDecl.nameAsString
                val node = DependencyNode(
                    id = "$packageName.$className",
                    className = className,
                    fileName = javaFile.name,
                    packageName = packageName,
                    nodeType = NodeType.ENUM,
                    layer = inferLayer(packageName, className),
                    language = "Java"
                )
                nodes.add(node)
                packages.getOrPut(packageName) { mutableListOf() }.add(className)
            }
        } catch (e: Exception) {
            println("Error processing Java file ${javaFile.name}: ${e.message}")
        }
    }
    
    // Build edges for Kotlin files (existing logic)
    for (ktFile in ktFiles) {
        val packageName = ktFile.packageFqName.asString()
        for (classOrObject in ktFile.declarations.filterIsInstance<KtClassOrObject>()) {
            val fromClassName = classOrObject.name ?: "Anonymous"
            val fromNodeId = "$packageName.$fromClassName"
            
            // Find references to other classes
            classOrObject.accept(object : KtTreeVisitorVoid() {
                override fun visitUserType(type: KtUserType) {
                    val referencedTypeName = type.referencedName
                    if (referencedTypeName != null) {
                        // Find matching node
                        val toNode = nodes.find { it.className == referencedTypeName }
                        if (toNode != null && toNode.id != fromNodeId) {
                            val edge = DependencyEdge(
                                fromId = fromNodeId,
                                toId = toNode.id,
                                dependencyType = DependencyType.USAGE,
                                strength = 1
                            )
                            if (!edges.contains(edge)) {
                                edges.add(edge)
                            }
                        }
                    }
                    super.visitUserType(type)
                }
            })
        }
    }
    
    // Build edges for Java files
    for (javaFile in javaFiles) {
        try {
            val cu = StaticJavaParser.parse(javaFile)
            val packageName = cu.packageDeclaration.map { it.nameAsString }.orElse("")
            
            cu.findAll(ClassOrInterfaceDeclaration::class.java).forEach { classDecl ->
                val fromClassName = classDecl.nameAsString
                val fromNodeId = "$packageName.$fromClassName"
                
                // Find type references
                classDecl.findAll(ClassOrInterfaceType::class.java).forEach { typeRef ->
                    val referencedTypeName = typeRef.nameAsString
                    val toNode = nodes.find { it.className == referencedTypeName }
                    if (toNode != null && toNode.id != fromNodeId) {
                        val edge = DependencyEdge(
                            fromId = fromNodeId,
                            toId = toNode.id,
                            dependencyType = DependencyType.USAGE,
                            strength = 1
                        )
                        if (!edges.contains(edge)) {
                            edges.add(edge)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error processing Java dependencies for ${javaFile.name}: ${e.message}")
        }
    }
    
    val cycles = detectCycles(nodes, edges)
    val packageAnalyses = packages.map { (packageName, classes) ->
        PackageAnalysis(
            packageName = packageName,
            classes = classes,
            dependencies = edges.filter { it.fromId.startsWith("$packageName.") }.map { it.toId },
            layer = inferLayer(packageName, ""),
            cohesion = 0.0
        )
    }
    
    return DependencyGraph(
        nodes = nodes,
        edges = edges,
        cycles = cycles,
        packages = packageAnalyses
    )
}

fun analyzeMixedDddPatterns(ktFiles: List<KtFile>, javaFiles: List<File>, classAnalyses: List<ClassAnalysis>): DddPatternAnalysis {
    val entities = mutableListOf<DddEntity>()
    val valueObjects = mutableListOf<DddValueObject>()
    val services = mutableListOf<DddService>()
    val repositories = mutableListOf<DddRepository>()
    val aggregates = mutableListOf<DddAggregate>()
    val domainEvents = mutableListOf<DddDomainEvent>()
    
    // Analyze Kotlin files (existing logic)
    for (ktFile in ktFiles) {
        for (classOrObject in ktFile.declarations.filterIsInstance<KtClassOrObject>()) {
            val className = classOrObject.name ?: "Anonymous"
            val fileName = ktFile.name
            
            // Entity analysis
            val entityAnalysis = analyzeKotlinEntity(classOrObject, fileName)
            if (entityAnalysis.confidence > 0.5) {
                entities.add(entityAnalysis)
            }
            
            // Value Object analysis
            val valueObjectAnalysis = analyzeKotlinValueObject(classOrObject, fileName)
            if (valueObjectAnalysis.confidence > 0.5) {
                valueObjects.add(valueObjectAnalysis)
            }
            
            // Service analysis
            val serviceAnalysis = analyzeKotlinService(classOrObject, fileName)
            if (serviceAnalysis.confidence > 0.5) {
                services.add(serviceAnalysis)
            }
            
            // Repository analysis
            val repositoryAnalysis = analyzeKotlinRepository(classOrObject, fileName)
            if (repositoryAnalysis.confidence > 0.5) {
                repositories.add(repositoryAnalysis)
            }
        }
    }
    
    // Analyze Java files
    for (javaFile in javaFiles) {
        try {
            val cu = StaticJavaParser.parse(javaFile)
            
            cu.findAll(ClassOrInterfaceDeclaration::class.java).forEach { classDecl ->
                val className = classDecl.nameAsString
                val fileName = javaFile.name
                
                // Entity analysis
                val entityAnalysis = analyzeJavaEntity(classDecl, fileName)
                if (entityAnalysis.confidence > 0.5) {
                    entities.add(entityAnalysis)
                }
                
                // Value Object analysis
                val valueObjectAnalysis = analyzeJavaValueObject(classDecl, fileName)
                if (valueObjectAnalysis.confidence > 0.5) {
                    valueObjects.add(valueObjectAnalysis)
                }
                
                // Service analysis
                val serviceAnalysis = analyzeJavaService(classDecl, fileName)
                if (serviceAnalysis.confidence > 0.5) {
                    services.add(serviceAnalysis)
                }
                
                // Repository analysis
                val repositoryAnalysis = analyzeJavaRepository(classDecl, fileName)
                if (repositoryAnalysis.confidence > 0.5) {
                    repositories.add(repositoryAnalysis)
                }
            }
        } catch (e: Exception) {
            println("Error analyzing DDD patterns in Java file ${javaFile.name}: ${e.message}")
        }
    }
    
    return DddPatternAnalysis(
        entities = entities,
        valueObjects = valueObjects,
        services = services,
        repositories = repositories,
        aggregates = aggregates,
        domainEvents = domainEvents
    )
}

fun analyzeMixedLayeredArchitecture(ktFiles: List<KtFile>, javaFiles: List<File>, dependencyGraph: DependencyGraph): LayeredArchitectureAnalysis {
    val layers = mutableMapOf<String, MutableList<String>>()
    val dependencies = mutableListOf<LayerDependency>()
    val violations = mutableListOf<ArchitectureViolation>()
    
    // Collect layers from all files
    for (node in dependencyGraph.nodes) {
        val layer = node.layer ?: "unknown"
        layers.getOrPut(layer) { mutableListOf() }.add(node.className)
    }
    
    // Convert to ArchitectureLayer objects
    val architectureLayers = layers.map { (layerName, classes) ->
        ArchitectureLayer(
            name = layerName,
            type = when (layerName.lowercase()) {
                "presentation" -> LayerType.PRESENTATION
                "application" -> LayerType.APPLICATION
                "domain" -> LayerType.DOMAIN
                "infrastructure" -> LayerType.INFRASTRUCTURE
                "data" -> LayerType.DATA
                else -> LayerType.APPLICATION
            },
            packages = emptyList(),
            classes = classes,
            level = 0
        )
    }
    
    // Analyze dependencies between layers
    for (edge in dependencyGraph.edges) {
        val fromNode = dependencyGraph.nodes.find { it.id == edge.fromId }
        val toNode = dependencyGraph.nodes.find { it.id == edge.toId }
        
        if (fromNode != null && toNode != null) {
            val fromLayer = fromNode.layer ?: "unknown"
            val toLayer = toNode.layer ?: "unknown"
            
            if (fromLayer != toLayer) {
                val dependency = LayerDependency(
                    fromLayer = fromLayer,
                    toLayer = toLayer,
                    dependencyCount = 1,
                    isValid = true
                )
                
                val existingDep = dependencies.find { it.fromLayer == fromLayer && it.toLayer == toLayer }
                if (existingDep != null) {
                    dependencies[dependencies.indexOf(existingDep)] = LayerDependency(
                        fromLayer = existingDep.fromLayer,
                        toLayer = existingDep.toLayer,
                        dependencyCount = existingDep.dependencyCount + 1,
                        isValid = existingDep.isValid
                    )
                } else {
                    dependencies.add(dependency)
                }
            }
        }
    }
    
    // Detect violations
    for (dependency in dependencies) {
        if (!dependency.isValid) {
            violations.add(ArchitectureViolation(
                fromClass = dependency.fromLayer,
                toClass = dependency.toLayer,
                violationType = ViolationType.LAYER_VIOLATION,
                suggestion = "Layer '${dependency.fromLayer}' should not depend on '${dependency.toLayer}'"
            ))
        }
    }
    
    val pattern = ArchitecturePattern.LAYERED // Simple default for mixed analysis
    
    return LayeredArchitectureAnalysis(
        layers = architectureLayers,
        dependencies = dependencies,
        violations = violations,
        pattern = pattern
    )
}

// Java DDD Pattern Analysis Functions
fun analyzeJavaEntity(classDecl: ClassOrInterfaceDeclaration, fileName: String): DddEntity {
    var confidence = 0.0
    val className = classDecl.nameAsString
    
    // Check for ID fields
    val idFields = mutableListOf<String>()
    val hasIdField = classDecl.fields.any { field ->
        field.variables.any { variable ->
            val fieldName = variable.nameAsString.lowercase()
            val hasIdAnnotation = field.annotations.any { it.nameAsString.contains("Id") }
            val isIdField = fieldName == "id" || fieldName.endsWith("id") || hasIdAnnotation
            if (isIdField) {
                idFields.add(variable.nameAsString)
            }
            isIdField
        }
    }
    if (hasIdField) confidence += 0.3
    
    // Check for mutability (non-final fields)
    val hasMutableFields = classDecl.fields.any { !it.isFinal }
    if (hasMutableFields) confidence += 0.2
    
    // Check for equals/hashCode methods
    val hasEqualsHashCode = classDecl.methods.any { it.nameAsString == "equals" } &&
                           classDecl.methods.any { it.nameAsString == "hashCode" }
    if (hasEqualsHashCode) confidence += 0.3
    
    // Check naming patterns
    if (className.endsWith("Entity") || className.endsWith("Aggregate")) confidence += 0.2
    
    // Check for JPA annotations
    val hasJpaAnnotations = classDecl.annotations.any { 
        it.nameAsString.contains("Entity") || it.nameAsString.contains("Table")
    }
    if (hasJpaAnnotations) confidence += 0.3
    
    return DddEntity(
        className = className,
        fileName = fileName,
        hasUniqueId = hasIdField,
        isMutable = hasMutableFields,
        idFields = idFields,
        confidence = confidence
    )
}

fun analyzeJavaValueObject(classDecl: ClassOrInterfaceDeclaration, fileName: String): DddValueObject {
    var confidence = 0.0
    val className = classDecl.nameAsString
    
    // Check for immutability (final fields)
    val isImmutable = classDecl.fields.all { it.isFinal }
    if (isImmutable) confidence += 0.4
    
    // Check for value equality (equals/hashCode methods)
    val hasValueEquality = classDecl.methods.any { it.nameAsString == "equals" } &&
                          classDecl.methods.any { it.nameAsString == "hashCode" }
    if (hasValueEquality) confidence += 0.3
    
    // Check naming patterns
    if (className.endsWith("Value") || className.endsWith("ValueObject")) confidence += 0.2
    
    // Check for lack of setters (immutability indicator)
    val hasSetters = classDecl.methods.any { it.nameAsString.startsWith("set") }
    if (!hasSetters) confidence += 0.1
    
    val properties = classDecl.fields.map { field ->
        field.variables.first().nameAsString
    }
    
    return DddValueObject(
        className = className,
        fileName = fileName,
        isImmutable = isImmutable,
        hasValueEquality = hasValueEquality,
        properties = properties,
        confidence = confidence
    )
}

fun analyzeJavaService(classDecl: ClassOrInterfaceDeclaration, fileName: String): DddService {
    var confidence = 0.0
    val className = classDecl.nameAsString
    
    // Check for statelessness (no instance fields except dependencies)
    val isStateless = classDecl.fields.all { field ->
        field.annotations.any { it.nameAsString.contains("Inject") || it.nameAsString.contains("Autowired") } ||
        field.isFinal // Final fields are often dependencies
    }
    if (isStateless) confidence += 0.3
    
    // Check naming patterns
    if (className.endsWith("Service") || className.endsWith("Handler") || className.contains("Service")) {
        confidence += 0.3
    }
    
    // Check for business logic methods
    val businessMethods = classDecl.methods.filter { method ->
        !method.nameAsString.startsWith("get") && 
        !method.nameAsString.startsWith("set") &&
        !method.nameAsString.equals("equals") &&
        !method.nameAsString.equals("hashCode") &&
        !method.nameAsString.equals("toString")
    }
    val hasDomainLogic = businessMethods.isNotEmpty()
    if (hasDomainLogic) confidence += 0.2
    
    // Check for Spring Service annotation
    val hasServiceAnnotation = classDecl.annotations.any { 
        it.nameAsString.contains("Service") || it.nameAsString.contains("Component")
    }
    if (hasServiceAnnotation) confidence += 0.2
    
    return DddService(
        className = className,
        fileName = fileName,
        isStateless = isStateless,
        hasDomainLogic = hasDomainLogic,
        methods = businessMethods.map { it.nameAsString },
        confidence = confidence
    )
}

fun analyzeJavaRepository(classDecl: ClassOrInterfaceDeclaration, fileName: String): DddRepository {
    var confidence = 0.0
    val className = classDecl.nameAsString
    
    // Check if it's an interface
    val isInterface = classDecl.isInterface
    if (isInterface) confidence += 0.3
    
    // Check naming patterns
    if (className.endsWith("Repository") || className.contains("Repository")) {
        confidence += 0.4
    }
    
    // Check for CRUD methods
    val crudMethods = mutableListOf<String>()
    classDecl.methods.forEach { method ->
        val methodName = method.nameAsString.lowercase()
        if (methodName.startsWith("find") || methodName.startsWith("get") ||
            methodName.startsWith("save") || methodName.startsWith("delete") ||
            methodName.startsWith("create") || methodName.startsWith("update")) {
            crudMethods.add(method.nameAsString)
        }
    }
    val hasDataAccess = crudMethods.isNotEmpty()
    if (hasDataAccess) confidence += 0.2
    
    // Check for Spring Repository annotation
    val hasRepositoryAnnotation = classDecl.annotations.any { 
        it.nameAsString.contains("Repository")
    }
    if (hasRepositoryAnnotation) confidence += 0.3
    
    return DddRepository(
        className = className,
        fileName = fileName,
        isInterface = isInterface,
        hasDataAccess = hasDataAccess,
        crudMethods = crudMethods,
        confidence = confidence
    )
}

// Kotlin DDD Pattern Analysis Functions (referenced in mixed analysis)
fun analyzeKotlinEntity(classOrObject: KtClassOrObject, fileName: String): DddEntity {
    var confidence = 0.0
    val className = classOrObject.name ?: "Anonymous"
    
    // Check for ID fields
    val idFields = mutableListOf<String>()
    val hasIdField = classOrObject.declarations.filterIsInstance<KtProperty>().any { property ->
        val propertyName = property.name?.lowercase() ?: ""
        val hasIdAnnotation = property.annotationEntries.any { it.shortName?.asString()?.contains("Id") == true }
        val isIdField = propertyName == "id" || propertyName.endsWith("id") || hasIdAnnotation
        if (isIdField) {
            idFields.add(property.name ?: "")
        }
        isIdField
    }
    if (hasIdField) confidence += 0.3
    
    // Check for mutability
    val hasMutableProperties = classOrObject.declarations.filterIsInstance<KtProperty>().any { property ->
        property.isVar
    }
    if (hasMutableProperties) confidence += 0.2
    
    // Check for equals/hashCode
    val methods = classOrObject.declarations.filterIsInstance<KtNamedFunction>()
    val hasEqualsHashCode = methods.any { it.name == "equals" } && methods.any { it.name == "hashCode" }
    if (hasEqualsHashCode) confidence += 0.3
    
    // Check naming patterns
    if (className.endsWith("Entity") || className.endsWith("Aggregate")) confidence += 0.2
    
    return DddEntity(
        className = className,
        fileName = fileName,
        hasUniqueId = hasIdField,
        isMutable = hasMutableProperties,
        idFields = idFields,
        confidence = confidence
    )
}

fun analyzeKotlinValueObject(classOrObject: KtClassOrObject, fileName: String): DddValueObject {
    var confidence = 0.0
    val className = classOrObject.name ?: "Anonymous"
    
    // Check for immutability (val properties)
    val properties = classOrObject.declarations.filterIsInstance<KtProperty>()
    val isImmutable = properties.all { !it.isVar }
    if (isImmutable) confidence += 0.4
    
    // Check for data class
    if (classOrObject is KtClass && classOrObject.isData()) confidence += 0.3
    
    // Check naming patterns
    if (className.endsWith("Value") || className.endsWith("ValueObject")) confidence += 0.2
    
    return DddValueObject(
        className = className,
        fileName = fileName,
        isImmutable = isImmutable,
        hasValueEquality = classOrObject is KtClass && classOrObject.isData(),
        properties = properties.map { it.name ?: "" },
        confidence = confidence
    )
}

fun analyzeKotlinService(classOrObject: KtClassOrObject, fileName: String): DddService {
    var confidence = 0.0
    val className = classOrObject.name ?: "Anonymous"
    
    // Check naming patterns
    if (className.endsWith("Service") || className.endsWith("Handler") || className.contains("Service")) {
        confidence += 0.3
    }
    
    // Check for business logic methods
    val methods = classOrObject.declarations.filterIsInstance<KtNamedFunction>()
    val businessMethods = methods.filter { method ->
        val methodName = method.name ?: ""
        !methodName.startsWith("get") && 
        !methodName.startsWith("set") &&
        !methodName.equals("equals") &&
        !methodName.equals("hashCode") &&
        !methodName.equals("toString")
    }
    val hasDomainLogic = businessMethods.isNotEmpty()
    if (hasDomainLogic) confidence += 0.2
    
    // Check for statelessness (no var properties except dependencies)
    val properties = classOrObject.declarations.filterIsInstance<KtProperty>()
    val isStateless = properties.all { property ->
        !property.isVar || property.annotationEntries.any { 
            it.shortName?.asString()?.contains("Inject") == true
        }
    }
    if (isStateless) confidence += 0.3
    
    return DddService(
        className = className,
        fileName = fileName,
        isStateless = isStateless,
        hasDomainLogic = hasDomainLogic,
        methods = businessMethods.map { it.name ?: "" },
        confidence = confidence
    )
}

fun analyzeKotlinRepository(classOrObject: KtClassOrObject, fileName: String): DddRepository {
    var confidence = 0.0
    val className = classOrObject.name ?: "Anonymous"
    
    // Check if it's an interface
    val isInterface = classOrObject is KtClass && classOrObject.isInterface()
    if (isInterface) confidence += 0.3
    
    // Check naming patterns
    if (className.endsWith("Repository") || className.contains("Repository")) {
        confidence += 0.4
    }
    
    // Check for CRUD methods
    val methods = classOrObject.declarations.filterIsInstance<KtNamedFunction>()
    val crudMethods = mutableListOf<String>()
    methods.forEach { method ->
        val methodName = method.name?.lowercase() ?: ""
        if (methodName.startsWith("find") || methodName.startsWith("get") ||
            methodName.startsWith("save") || methodName.startsWith("delete") ||
            methodName.startsWith("create") || methodName.startsWith("update")) {
            crudMethods.add(method.name ?: "")
        }
    }
    val hasDataAccess = crudMethods.isNotEmpty()
    if (hasDataAccess) confidence += 0.2
    
    return DddRepository(
        className = className,
        fileName = fileName,
        isInterface = isInterface,
        hasDataAccess = hasDataAccess,
        crudMethods = crudMethods,
        confidence = confidence
    )
}

fun buildDependencyGraph(ktFiles: List<KtFile>): DependencyGraph {
    val nodes = mutableListOf<DependencyNode>()
    val edges = mutableListOf<DependencyEdge>()
    val packages = mutableMapOf<String, MutableList<String>>()
    
    // Build nodes
    for (ktFile in ktFiles) {
        val packageName = ktFile.packageFqName.asString()
        
        for (classOrObject in ktFile.declarations.filterIsInstance<KtClassOrObject>()) {
            val className = classOrObject.name ?: "Anonymous"
            val nodeType = when {
                classOrObject is KtClass && classOrObject.isInterface() -> NodeType.INTERFACE
                classOrObject is KtClass && classOrObject.isAbstract() -> NodeType.ABSTRACT_CLASS
                classOrObject is KtObjectDeclaration -> NodeType.OBJECT
                classOrObject is KtEnumEntry -> NodeType.ENUM
                else -> NodeType.CLASS
            }
            
            val node = DependencyNode(
                id = "$packageName.$className",
                className = className,
                fileName = ktFile.name,
                packageName = packageName,
                nodeType = nodeType,
                layer = inferLayer(packageName, className)
            )
            nodes.add(node)
            
            packages.getOrPut(packageName) { mutableListOf() }.add(className)
        }
    }
    
    // Build edges by analyzing imports and references
    for (ktFile in ktFiles) {
        val currentPackage = ktFile.packageFqName.asString()
        
        for (classOrObject in ktFile.declarations.filterIsInstance<KtClassOrObject>()) {
            val currentClass = classOrObject.name ?: "Anonymous"
            val currentId = "$currentPackage.$currentClass"
            
            // Analyze supertype dependencies
            classOrObject.superTypeListEntries.forEach { superTypeEntry ->
                val referencedType = superTypeEntry.typeReference?.text
                if (referencedType != null) {
                    val targetId = resolveTypeReference(referencedType, ktFiles, currentPackage)
                    if (targetId != null) {
                        edges.add(DependencyEdge(
                            fromId = currentId,
                            toId = targetId,
                            dependencyType = DependencyType.INHERITANCE,
                            strength = 3
                        ))
                    }
                }
            }
            
            // Analyze property dependencies
            classOrObject.declarations.filterIsInstance<KtProperty>().forEach { property ->
                property.typeReference?.text?.let { typeRef ->
                    val targetId = resolveTypeReference(typeRef, ktFiles, currentPackage)
                    if (targetId != null) {
                        edges.add(DependencyEdge(
                            fromId = currentId,
                            toId = targetId,
                            dependencyType = DependencyType.COMPOSITION,
                            strength = 2
                        ))
                    }
                }
            }
            
            // Analyze method parameter and return type dependencies
            classOrObject.body?.functions?.forEach { function ->
                function.valueParameters.forEach { param ->
                    param.typeReference?.text?.let { typeRef ->
                        val targetId = resolveTypeReference(typeRef, ktFiles, currentPackage)
                        if (targetId != null) {
                            edges.add(DependencyEdge(
                                fromId = currentId,
                                toId = targetId,
                                dependencyType = DependencyType.USAGE,
                                strength = 1
                            ))
                        }
                    }
                }
                
                function.typeReference?.text?.let { typeRef ->
                    val targetId = resolveTypeReference(typeRef, ktFiles, currentPackage)
                    if (targetId != null) {
                        edges.add(DependencyEdge(
                            fromId = currentId,
                            toId = targetId,
                            dependencyType = DependencyType.USAGE,
                            strength = 1
                        ))
                    }
                }
            }
        }
    }
    
    val cycles = detectCycles(nodes, edges)
    val packageAnalyses = packages.map { (packageName, classes) ->
        PackageAnalysis(
            packageName = packageName,
            classes = classes,
            dependencies = edges.filter { edge -> 
                nodes.find { it.id == edge.fromId }?.packageName == packageName 
            }.mapNotNull { edge ->
                nodes.find { it.id == edge.toId }?.packageName
            }.distinct(),
            layer = inferLayer(packageName, ""),
            cohesion = calculatePackageCohesion(packageName, edges, nodes)
        )
    }
    
    return DependencyGraph(
        nodes = nodes,
        edges = edges,
        cycles = cycles,
        packages = packageAnalyses
    )
}

fun resolveTypeReference(typeRef: String, ktFiles: List<KtFile>, currentPackage: String): String? {
    val cleanTypeRef = typeRef.replace("<.*>".toRegex(), "").replace("?", "").trim()
    
    for (ktFile in ktFiles) {
        val packageName = ktFile.packageFqName.asString()
        for (classOrObject in ktFile.declarations.filterIsInstance<KtClassOrObject>()) {
            val className = classOrObject.name ?: continue
            
            if (className == cleanTypeRef) {
                return "$packageName.$className"
            }
        }
    }
    
    return null
}

fun inferLayer(packageName: String, className: String): String? {
    val packageLower = packageName.lowercase()
    val classLower = className.lowercase()
    
    return when {
        packageLower.contains("controller") || packageLower.contains("web") || 
        packageLower.contains("api") || packageLower.contains("ui") -> "presentation"
        packageLower.contains("service") || packageLower.contains("application") -> "application"
        packageLower.contains("domain") || packageLower.contains("model") -> "domain"
        packageLower.contains("repository") || packageLower.contains("dao") || 
        packageLower.contains("data") || packageLower.contains("persistence") -> "data"
        packageLower.contains("infrastructure") || packageLower.contains("config") -> "infrastructure"
        classLower.endsWith("controller") || classLower.endsWith("api") -> "presentation"
        classLower.endsWith("service") -> "application"
        classLower.endsWith("repository") || classLower.endsWith("dao") -> "data"
        else -> null
    }
}

fun detectCycles(nodes: List<DependencyNode>, edges: List<DependencyEdge>): List<DependencyCycle> {
    val cycles = mutableListOf<DependencyCycle>()
    val visited = mutableSetOf<String>()
    val recursionStack = mutableSetOf<String>()
    val adjacencyList = edges.groupBy { it.fromId }
    
    fun dfs(nodeId: String, path: MutableList<String>): Boolean {
        if (recursionStack.contains(nodeId)) {
            val cycleStart = path.indexOf(nodeId)
            if (cycleStart != -1) {
                val cyclePath = path.subList(cycleStart, path.size).toList() // Create a copy
                val severity = when {
                    cyclePath.size <= 3 -> CycleSeverity.LOW
                    cyclePath.size <= 6 -> CycleSeverity.MEDIUM
                    else -> CycleSeverity.HIGH
                }
                cycles.add(DependencyCycle(cyclePath, severity))
                return true
            }
        }
        
        if (visited.contains(nodeId)) return false
        
        visited.add(nodeId)
        recursionStack.add(nodeId)
        path.add(nodeId)
        
        adjacencyList[nodeId]?.forEach { edge ->
            dfs(edge.toId, path)
        }
        
        recursionStack.remove(nodeId)
        path.removeAt(path.size - 1)
        
        return false
    }
    
    nodes.forEach { node ->
        if (!visited.contains(node.id)) {
            dfs(node.id, mutableListOf())
        }
    }
    
    return cycles
}

fun calculatePackageCohesion(packageName: String, edges: List<DependencyEdge>, nodes: List<DependencyNode>): Double {
    val packageNodes = nodes.filter { it.packageName == packageName }
    if (packageNodes.size < 2) return 1.0
    
    val internalEdges = edges.filter { edge ->
        val fromNode = nodes.find { it.id == edge.fromId }
        val toNode = nodes.find { it.id == edge.toId }
        fromNode?.packageName == packageName && toNode?.packageName == packageName
    }
    
    val maxPossibleEdges = packageNodes.size * (packageNodes.size - 1)
    return if (maxPossibleEdges > 0) internalEdges.size.toDouble() / maxPossibleEdges else 0.0
}

fun analyzeDddPatterns(ktFiles: List<KtFile>, classAnalyses: List<ClassAnalysis>): DddPatternAnalysis {
    val entities = mutableListOf<DddEntity>()
    val valueObjects = mutableListOf<DddValueObject>()
    val services = mutableListOf<DddService>()
    val repositories = mutableListOf<DddRepository>()
    val aggregates = mutableListOf<DddAggregate>()
    val domainEvents = mutableListOf<DddDomainEvent>()
    
    for (ktFile in ktFiles) {
        for (classOrObject in ktFile.declarations.filterIsInstance<KtClassOrObject>()) {
            val className = classOrObject.name ?: continue
            val fileName = ktFile.name
            
            // Detect Entity
            val entityAnalysis = analyzeEntity(classOrObject, className, fileName)
            if (entityAnalysis.confidence > 0.3) {
                entities.add(entityAnalysis)
            }
            
            // Detect Value Object
            val valueObjectAnalysis = analyzeValueObject(classOrObject, className, fileName)
            if (valueObjectAnalysis.confidence > 0.3) {
                valueObjects.add(valueObjectAnalysis)
            }
            
            // Detect Service
            val serviceAnalysis = analyzeService(classOrObject, className, fileName)
            if (serviceAnalysis.confidence > 0.3) {
                services.add(serviceAnalysis)
            }
            
            // Detect Repository
            val repositoryAnalysis = analyzeRepository(classOrObject, className, fileName)
            if (repositoryAnalysis.confidence > 0.3) {
                repositories.add(repositoryAnalysis)
            }
            
            // Detect Domain Event
            val domainEventAnalysis = analyzeDomainEvent(classOrObject, className, fileName)
            if (domainEventAnalysis.confidence > 0.3) {
                domainEvents.add(domainEventAnalysis)
            }
        }
    }
    
    // Detect Aggregates based on entity relationships
    val aggregateAnalysis = analyzeAggregates(entities, ktFiles)
    aggregates.addAll(aggregateAnalysis)
    
    return DddPatternAnalysis(
        entities = entities,
        valueObjects = valueObjects,
        services = services,
        repositories = repositories,
        aggregates = aggregates,
        domainEvents = domainEvents
    )
}

// Helper functions for better DDD pattern detection
fun isTestClass(className: String): Boolean {
    return className.endsWith("Test") || className.endsWith("Tests") || 
           className.contains("Test") || className.contains("Mock") || 
           className.contains("Spec") || className.contains("IT") ||
           className.endsWith("UnitTest") || className.endsWith("IntegrationTest") ||
           className.endsWith("MvcTest")
}

fun isUtilityClass(className: String): Boolean {
    return className.endsWith("Util") || className.endsWith("Utils") || 
           className.endsWith("Helper") || className.endsWith("Constants") ||
           className.endsWith("Config") || className.endsWith("Configuration")
}

fun isDtoClass(className: String, classOrObject: KtClassOrObject): Boolean {
    if (className.endsWith("Dto") || className.endsWith("DTO") || 
        className.endsWith("Request") || className.endsWith("Response") ||
        className.endsWith("Payload") || className.endsWith("Data")) {
        return true
    }
    
    // Check if it's a data class with no business logic
    if (classOrObject is KtClass && classOrObject.isData()) {
        val methods = classOrObject.body?.functions ?: emptyList()
        val businessMethods = methods.filter { method ->
            val name = method.name?.lowercase() ?: ""
            !name.contains("equals") && !name.contains("hashcode") && !name.contains("tostring") &&
            !name.contains("copy") && !name.contains("component")
        }
        return businessMethods.isEmpty()
    }
    
    return false
}

fun hasSpringAnnotation(classOrObject: KtClassOrObject, annotationName: String): Boolean {
    val annotations = classOrObject.annotationEntries
    return annotations.any { annotation ->
        val shortName = annotation.shortName?.asString()
        val fullName = annotation.typeReference?.text
        
        // Check for short name match (e.g., @Controller)
        shortName == annotationName ||
        // Check for Spring stereotype annotations
        fullName?.endsWith("stereotype.$annotationName") == true ||
        fullName?.endsWith("web.bind.annotation.$annotationName") == true ||
        fullName?.endsWith("data.jpa.repository.$annotationName") == true ||
        // Check for JPA/Hibernate annotations
        fullName?.endsWith("persistence.$annotationName") == true ||
        fullName?.endsWith("jpa.$annotationName") == true ||
        fullName?.endsWith("hibernate.annotations.$annotationName") == true ||
        // Check for Spring Data annotations
        fullName?.endsWith("data.repository.$annotationName") == true ||
        fullName?.endsWith("repository.$annotationName") == true ||
        // Check for Spring Boot annotations
        fullName?.endsWith("boot.autoconfigure.$annotationName") == true ||
        fullName?.endsWith("transaction.annotation.$annotationName") == true
    }
}

fun isControllerClass(className: String, classOrObject: KtClassOrObject? = null): Boolean {
    // Check Spring annotations first
    if (classOrObject != null) {
        if (hasSpringAnnotation(classOrObject, "Controller") || 
            hasSpringAnnotation(classOrObject, "RestController") ||
            hasSpringAnnotation(classOrObject, "RequestMapping")) {
            return true
        }
    }
    
    // Fallback to naming patterns
    return className.endsWith("Controller") || className.endsWith("Endpoint") ||
           className.endsWith("Resource") || className.endsWith("Handler") ||
           className.contains("Controller")
}

fun isServiceClass(className: String, classOrObject: KtClassOrObject? = null): Boolean {
    // Check Spring annotations first
    if (classOrObject != null) {
        if (hasSpringAnnotation(classOrObject, "Service") || 
            hasSpringAnnotation(classOrObject, "Component") ||
            hasSpringAnnotation(classOrObject, "Transactional")) {
            return true
        }
    }
    
    // Fallback to naming patterns
    return className.endsWith("Service") || className.endsWith("Manager") ||
           className.endsWith("Provider") || className.endsWith("Factory") ||
           className.endsWith("Builder") || className.endsWith("Processor")
}

fun isRepositoryClass(className: String, classOrObject: KtClassOrObject? = null): Boolean {
    // Check Spring annotations first
    if (classOrObject != null) {
        if (hasSpringAnnotation(classOrObject, "Repository") || 
            hasSpringAnnotation(classOrObject, "Component") ||
            hasSpringAnnotation(classOrObject, "JpaRepository") ||
            hasSpringAnnotation(classOrObject, "CrudRepository")) {
            return true
        }
    }
    
    // Fallback to naming patterns
    return className.endsWith("Repository") || className.endsWith("DAO") ||
           className.endsWith("DataAccess") || className.contains("Repository")
}

fun isEntityClass(className: String, classOrObject: KtClassOrObject? = null): Boolean {
    // Check JPA/Hibernate annotations first
    if (classOrObject != null) {
        if (hasSpringAnnotation(classOrObject, "Entity") || 
            hasSpringAnnotation(classOrObject, "Table") ||
            hasSpringAnnotation(classOrObject, "Embeddable") ||
            hasSpringAnnotation(classOrObject, "MappedSuperclass")) {
            return true
        }
    }
    
    // Fallback to naming patterns
    return className.endsWith("Entity") || className.endsWith("Model") ||
           className.endsWith("Aggregate") || className.endsWith("Root")
}

fun hasBusinessLogic(classOrObject: KtClassOrObject): Boolean {
    val methods = classOrObject.body?.functions ?: emptyList()
    val businessMethods = methods.filter { method ->
        val name = method.name?.lowercase() ?: ""
        !name.contains("equals") && !name.contains("hashcode") && !name.contains("tostring") &&
        !name.contains("copy") && !name.contains("component") && !name.contains("get") &&
        !name.contains("set") && method.bodyExpression != null
    }
    return businessMethods.isNotEmpty()
}

fun isInDomainPackage(fileName: String): Boolean {
    return fileName.contains("/domain/") || fileName.contains("\\domain\\") ||
           fileName.contains("/entity/") || fileName.contains("\\entity\\") ||
           fileName.contains("/model/") || fileName.contains("\\model\\")
}

fun analyzeEntity(classOrObject: KtClassOrObject, className: String, fileName: String): DddEntity {
    var confidence = 0.0
    var hasUniqueId = false
    var isMutable = false
    val idFields = mutableListOf<String>()
    
    // Early exit for non-entity classes
    if (isTestClass(className) || isUtilityClass(className) || isDtoClass(className, classOrObject) || 
        isControllerClass(className, classOrObject) || isServiceClass(className, classOrObject) || 
        isRepositoryClass(className, classOrObject)) {
        return DddEntity(className, fileName, false, false, emptyList(), 0.0)
    }
    
    val properties = classOrObject.declarations.filterIsInstance<KtProperty>()
    
    // Check for ID fields
    properties.forEach { property ->
        val propName = property.name?.lowercase() ?: ""
        if (propName.contains("id") || propName == "uuid" || propName == "key") {
            hasUniqueId = true
            idFields.add(property.name ?: "")
            confidence += 0.3
        }
    }
    
    // Check for mutability
    val mutableProperties = properties.filter { it.isVar }
    if (mutableProperties.isNotEmpty()) {
        isMutable = true
        confidence += 0.2
    }
    
    // Check for equals/hashCode methods
    val methods = classOrObject.body?.functions ?: emptyList()
    val hasEquals = methods.any { it.name == "equals" }
    val hasHashCode = methods.any { it.name == "hashCode" }
    
    if (hasEquals && hasHashCode) {
        confidence += 0.3
    }
    
    // Check Spring/JPA annotations
    if (hasSpringAnnotation(classOrObject, "Entity")) {
        confidence += 0.5
    }
    if (hasSpringAnnotation(classOrObject, "Table")) {
        confidence += 0.3
    }
    if (hasSpringAnnotation(classOrObject, "Embeddable")) {
        confidence += 0.4
    }
    
    // Check class name patterns
    if (className.endsWith("Entity") || className.endsWith("Aggregate")) {
        confidence += 0.2
    }
    
    // Check for lifecycle methods
    val hasLifecycleMethods = methods.any { method ->
        val name = method.name?.lowercase() ?: ""
        name.contains("create") || name.contains("update") || name.contains("delete") || 
        name.contains("save") || name.contains("activate") || name.contains("deactivate")
    }
    
    if (hasLifecycleMethods) {
        confidence += 0.2
    }
    
    // Check for business logic
    if (hasBusinessLogic(classOrObject)) {
        confidence += 0.3
    }
    
    // Check package structure
    if (isInDomainPackage(fileName)) {
        confidence += 0.2
    }
    
    // Entities should have both ID and some business logic or lifecycle methods
    if (!hasUniqueId && !hasLifecycleMethods && !hasBusinessLogic(classOrObject)) {
        confidence = 0.0
    }
    
    return DddEntity(
        className = className,
        fileName = fileName,
        hasUniqueId = hasUniqueId,
        isMutable = isMutable,
        idFields = idFields,
        confidence = confidence.coerceAtMost(1.0)
    )
}

fun analyzeValueObject(classOrObject: KtClassOrObject, className: String, fileName: String): DddValueObject {
    var confidence = 0.0
    var isImmutable = false
    var hasValueEquality = false
    val properties = mutableListOf<String>()
    
    val classProperties = classOrObject.declarations.filterIsInstance<KtProperty>()
    
    // Check for immutability
    val immutableProperties = classProperties.filter { !it.isVar }
    if (immutableProperties.isNotEmpty() && immutableProperties.size == classProperties.size) {
        isImmutable = true
        confidence += 0.4
    }
    
    classProperties.forEach { property ->
        properties.add(property.name ?: "")
    }
    
    // Check for data class
    if (classOrObject is KtClass && classOrObject.isData()) {
        confidence += 0.3
        hasValueEquality = true
    }
    
    // Check for custom equals/hashCode based on all properties
    val methods = classOrObject.body?.functions ?: emptyList()
    val hasEquals = methods.any { it.name == "equals" }
    val hasHashCode = methods.any { it.name == "hashCode" }
    
    if (hasEquals && hasHashCode) {
        hasValueEquality = true
        confidence += 0.2
    }
    
    // Check class name patterns
    if (className.endsWith("Value") || className.endsWith("VO") || className.endsWith("ValueObject")) {
        confidence += 0.2
    }
    
    // Check for no business logic methods
    val businessMethods = methods.filter { method ->
        val name = method.name?.lowercase() ?: ""
        !name.contains("equals") && !name.contains("hashcode") && !name.contains("tostring") &&
        !name.contains("copy") && !name.contains("component")
    }
    
    if (businessMethods.isEmpty() && properties.isNotEmpty()) {
        confidence += 0.2
    }
    
    return DddValueObject(
        className = className,
        fileName = fileName,
        isImmutable = isImmutable,
        hasValueEquality = hasValueEquality,
        properties = properties,
        confidence = confidence.coerceAtMost(1.0)
    )
}

fun analyzeService(classOrObject: KtClassOrObject, className: String, fileName: String): DddService {
    var confidence = 0.0
    var isStateless = false
    var hasDomainLogic = false
    val methods = mutableListOf<String>()
    
    // Early exit for non-service classes
    if (isTestClass(className) || isDtoClass(className, classOrObject) || 
        isControllerClass(className, classOrObject) || isRepositoryClass(className, classOrObject)) {
        return DddService(className, fileName, false, false, emptyList(), 0.0)
    }
    
    val classProperties = classOrObject.declarations.filterIsInstance<KtProperty>()
    val classMethods = classOrObject.body?.functions ?: emptyList()
    
    // Check for statelessness (no or minimal state)
    if (classProperties.isEmpty() || classProperties.all { it.name?.lowercase()?.contains("repository") == true }) {
        isStateless = true
        confidence += 0.3
    }
    
    // Check Spring annotations first
    if (hasSpringAnnotation(classOrObject, "Service")) {
        confidence += 0.6
    }
    if (hasSpringAnnotation(classOrObject, "Component")) {
        confidence += 0.4
    }
    if (hasSpringAnnotation(classOrObject, "Transactional")) {
        confidence += 0.3
    }
    
    // Check class name patterns - be more specific for services
    if (className.endsWith("Service") && !className.endsWith("Test")) {
        confidence += 0.4
    } else if (className.endsWith("Manager") || className.endsWith("Handler")) {
        confidence += 0.2
    }
    
    classMethods.forEach { method ->
        methods.add(method.name ?: "")
        val methodName = method.name?.lowercase() ?: ""
        
        // Check for domain logic patterns
        if (methodName.contains("calculate") || methodName.contains("validate") || 
            methodName.contains("process") || methodName.contains("handle") ||
            methodName.contains("execute") || methodName.contains("apply") ||
            methodName.contains("transform") || methodName.contains("convert")) {
            hasDomainLogic = true
            confidence += 0.1
        }
    }
    
    // Check for interface implementation
    if (classOrObject is KtClass && classOrObject.isInterface()) {
        confidence += 0.2
    }
    
    // Check for dependency injection patterns
    val constructor = classOrObject.primaryConstructor
    if (constructor != null && constructor.valueParameters.isNotEmpty()) {
        confidence += 0.2
    }
    
    // Check package structure
    if (fileName.contains("/service/") || fileName.contains("\\service\\") ||
        fileName.contains("/domain/") || fileName.contains("\\domain\\")) {
        confidence += 0.2
    }
    
    // Services should have methods and be stateless
    if (classMethods.isEmpty() || !isStateless) {
        confidence *= 0.5
    }
    
    return DddService(
        className = className,
        fileName = fileName,
        isStateless = isStateless,
        hasDomainLogic = hasDomainLogic,
        methods = methods,
        confidence = confidence.coerceAtMost(1.0)
    )
}

fun analyzeRepository(classOrObject: KtClassOrObject, className: String, fileName: String): DddRepository {
    var confidence = 0.0
    var isInterface = false
    var hasDataAccess = false
    val crudMethods = mutableListOf<String>()
    
    // Early exit for non-repository classes
    if (isTestClass(className) || isDtoClass(className, classOrObject) || 
        isControllerClass(className, classOrObject) || isServiceClass(className, classOrObject)) {
        return DddRepository(className, fileName, false, false, emptyList(), 0.0)
    }
    
    val methods = classOrObject.body?.functions ?: emptyList()
    
    // Check if it's an interface
    if (classOrObject is KtClass && classOrObject.isInterface()) {
        isInterface = true
        confidence += 0.3
    }
    
    // Check Spring annotations first
    if (hasSpringAnnotation(classOrObject, "Repository")) {
        confidence += 0.6
    }
    if (hasSpringAnnotation(classOrObject, "Component")) {
        confidence += 0.4
    }
    
    // Check class name patterns
    if (className.endsWith("Repository") && !className.endsWith("Test")) {
        confidence += 0.4
    } else if (className.endsWith("DAO") || className.endsWith("DataAccess")) {
        confidence += 0.3
    }
    
    // Check for CRUD methods
    methods.forEach { method ->
        val methodName = method.name?.lowercase() ?: ""
        if (methodName.contains("save") || methodName.contains("create") || methodName.contains("insert") ||
            methodName.contains("find") || methodName.contains("get") || methodName.contains("select") ||
            methodName.contains("update") || methodName.contains("modify") ||
            methodName.contains("delete") || methodName.contains("remove")) {
            crudMethods.add(method.name ?: "")
            hasDataAccess = true
            confidence += 0.1
        }
    }
    
    // Check for collection-like methods
    val hasCollectionMethods = methods.any { method ->
        val name = method.name?.lowercase() ?: ""
        name.contains("findall") || name.contains("getall") || name.contains("list") ||
        name.contains("count") || name.contains("exists")
    }
    
    if (hasCollectionMethods) {
        confidence += 0.2
    }
    
    // Check package structure
    if (fileName.contains("/repository/") || fileName.contains("\\repository\\") ||
        fileName.contains("/dao/") || fileName.contains("\\dao\\") ||
        fileName.contains("/data/") || fileName.contains("\\data\\")) {
        confidence += 0.2
    }
    
    // Repositories should have CRUD methods
    if (crudMethods.isEmpty()) {
        confidence *= 0.3
    }
    
    return DddRepository(
        className = className,
        fileName = fileName,
        isInterface = isInterface,
        hasDataAccess = hasDataAccess,
        crudMethods = crudMethods,
        confidence = confidence.coerceAtMost(1.0)
    )
}

fun analyzeDomainEvent(classOrObject: KtClassOrObject, className: String, fileName: String): DddDomainEvent {
    var confidence = 0.0
    var isEvent = false
    
    // Check class name patterns
    if (className.endsWith("Event") || className.endsWith("Occurred") || className.endsWith("Happened")) {
        isEvent = true
        confidence += 0.4
    }
    
    // Check for event-related properties
    val properties = classOrObject.declarations.filterIsInstance<KtProperty>()
    val hasTimestamp = properties.any { 
        val name = it.name?.lowercase() ?: ""
        name.contains("timestamp") || name.contains("occurredat") || name.contains("time")
    }
    
    if (hasTimestamp) {
        confidence += 0.3
    }
    
    // Check for immutability (events should be immutable)
    val immutableProperties = properties.filter { !it.isVar }
    if (immutableProperties.isNotEmpty() && immutableProperties.size == properties.size) {
        confidence += 0.2
    }
    
    // Check for data class
    if (classOrObject is KtClass && classOrObject.isData()) {
        confidence += 0.1
    }
    
    return DddDomainEvent(
        className = className,
        fileName = fileName,
        isEvent = isEvent,
        confidence = confidence.coerceAtMost(1.0)
    )
}

fun analyzeAggregates(entities: List<DddEntity>, ktFiles: List<KtFile>): List<DddAggregate> {
    val aggregates = mutableListOf<DddAggregate>()
    
    // Simple heuristic: entities with high confidence that reference other entities
    entities.filter { it.confidence > 0.7 }.forEach { entity ->
        val relatedEntities = findRelatedEntities(entity, entities, ktFiles)
        if (relatedEntities.isNotEmpty()) {
            aggregates.add(DddAggregate(
                rootEntity = entity.className,
                relatedEntities = relatedEntities,
                confidence = entity.confidence * 0.8
            ))
        }
    }
    
    return aggregates
}

fun findRelatedEntities(entity: DddEntity, allEntities: List<DddEntity>, ktFiles: List<KtFile>): List<String> {
    val relatedEntities = mutableListOf<String>()
    
    // Find the class definition for the entity
    for (ktFile in ktFiles) {
        for (classOrObject in ktFile.declarations.filterIsInstance<KtClassOrObject>()) {
            if (classOrObject.name == entity.className) {
                // Check properties for references to other entities
                val properties = classOrObject.declarations.filterIsInstance<KtProperty>()
                properties.forEach { property ->
                    val propertyType = property.typeReference?.text?.replace("?", "")?.trim()
                    if (propertyType != null) {
                        val referencedEntity = allEntities.find { it.className == propertyType }
                        if (referencedEntity != null && referencedEntity.className != entity.className) {
                            relatedEntities.add(referencedEntity.className)
                        }
                    }
                }
                break
            }
        }
    }
    
    return relatedEntities
}

fun analyzeLayeredArchitecture(ktFiles: List<KtFile>, dependencyGraph: DependencyGraph): LayeredArchitectureAnalysis {
    val layers = mutableListOf<ArchitectureLayer>()
    val dependencies = mutableListOf<LayerDependency>()
    val violations = mutableListOf<ArchitectureViolation>()
    
    // Identify layers based on packages and classes
    val layerMap = mutableMapOf<String, MutableList<String>>()
    val layerPackages = mutableMapOf<String, MutableList<String>>()
    
    for (node in dependencyGraph.nodes) {
        val layer = node.layer ?: "unknown"
        layerMap.getOrPut(layer) { mutableListOf() }.add(node.className)
        layerPackages.getOrPut(layer) { mutableListOf() }.add(node.packageName)
    }
    
    // Create layer objects
    layerMap.forEach { (layerName, classes) ->
        val layerType = when (layerName) {
            "presentation" -> LayerType.PRESENTATION
            "application" -> LayerType.APPLICATION
            "domain" -> LayerType.DOMAIN
            "data" -> LayerType.DATA
            "infrastructure" -> LayerType.INFRASTRUCTURE
            else -> LayerType.DOMAIN
        }
        
        val level = when (layerName) {
            "presentation" -> 1
            "application" -> 2
            "domain" -> 3
            "data" -> 4
            "infrastructure" -> 4
            else -> 3
        }
        
        layers.add(ArchitectureLayer(
            name = layerName,
            type = layerType,
            packages = layerPackages[layerName]?.distinct() ?: emptyList(),
            classes = classes.distinct(),
            level = level
        ))
    }
    
    // Analyze dependencies between layers
    val layerDependencyMap = mutableMapOf<Pair<String, String>, MutableList<DependencyEdge>>()
    
    for (edge in dependencyGraph.edges) {
        val fromNode = dependencyGraph.nodes.find { it.id == edge.fromId }
        val toNode = dependencyGraph.nodes.find { it.id == edge.toId }
        
        if (fromNode != null && toNode != null) {
            val fromLayer = fromNode.layer ?: "unknown"
            val toLayer = toNode.layer ?: "unknown"
            
            if (fromLayer != toLayer) {
                val layerPair = Pair(fromLayer, toLayer)
                layerDependencyMap.getOrPut(layerPair) { mutableListOf() }.add(edge)
            }
        }
    }
    
    // Create layer dependencies and check for violations
    layerDependencyMap.forEach { (layerPair, edges) ->
        val fromLayer = layerPair.first
        val toLayer = layerPair.second
        
        val isValid = isValidLayerDependency(fromLayer, toLayer)
        
        dependencies.add(LayerDependency(
            fromLayer = fromLayer,
            toLayer = toLayer,
            dependencyCount = edges.size,
            isValid = isValid
        ))
        
        if (!isValid) {
            edges.forEach { edge ->
                val fromNode = dependencyGraph.nodes.find { it.id == edge.fromId }
                val toNode = dependencyGraph.nodes.find { it.id == edge.toId }
                
                if (fromNode != null && toNode != null) {
                    violations.add(ArchitectureViolation(
                        fromClass = fromNode.className,
                        toClass = toNode.className,
                        violationType = ViolationType.LAYER_VIOLATION,
                        suggestion = "Layer '${fromLayer}' should not depend on layer '${toLayer}'. Consider introducing an interface or moving the dependency to a proper layer."
                    ))
                }
            }
        }
    }
    
    // Check for circular dependencies
    for (cycle in dependencyGraph.cycles) {
        if (cycle.nodes.size > 1) {
            for (i in cycle.nodes.indices) {
                val currentNode = cycle.nodes[i]
                val nextNode = cycle.nodes[(i + 1) % cycle.nodes.size]
                
                violations.add(ArchitectureViolation(
                    fromClass = currentNode.substringAfterLast("."),
                    toClass = nextNode.substringAfterLast("."),
                    violationType = ViolationType.CIRCULAR_DEPENDENCY,
                    suggestion = "Circular dependency detected. Consider breaking the cycle by introducing interfaces or rearranging dependencies."
                ))
            }
        }
    }
    
    // Determine architecture pattern
    val pattern = determineArchitecturePattern(layers, dependencies)
    
    return LayeredArchitectureAnalysis(
        layers = layers,
        dependencies = dependencies,
        violations = violations,
        pattern = pattern
    )
}

fun isValidLayerDependency(fromLayer: String, toLayer: String): Boolean {
    // Define valid layer dependencies (higher layers can depend on lower layers)
    val layerHierarchy = mapOf(
        "presentation" to 1,
        "application" to 2,
        "domain" to 3,
        "data" to 4,
        "infrastructure" to 4
    )
    
    val fromLevel = layerHierarchy[fromLayer] ?: 3
    val toLevel = layerHierarchy[toLayer] ?: 3
    
    // Special cases
    if (fromLayer == "unknown" || toLayer == "unknown") return true
    if (fromLayer == toLayer) return true
    
    // Presentation can depend on application and domain
    if (fromLayer == "presentation" && (toLayer == "application" || toLayer == "domain")) return true
    
    // Application can depend on domain
    if (fromLayer == "application" && toLayer == "domain") return true
    
    // Domain should not depend on other layers (except infrastructure for technical concerns)
    if (fromLayer == "domain" && toLayer == "infrastructure") return true
    if (fromLayer == "domain" && toLayer != "domain") return false
    
    // Data/Infrastructure can depend on domain
    if ((fromLayer == "data" || fromLayer == "infrastructure") && toLayer == "domain") return true
    
    // General rule: higher level (lower number) can depend on lower level (higher number)
    return fromLevel <= toLevel
}

fun determineArchitecturePattern(layers: List<ArchitectureLayer>, dependencies: List<LayerDependency>): ArchitecturePattern {
    val layerNames = layers.map { it.name }.toSet()
    
    // Check for hexagonal/clean architecture patterns
    val hasDomainLayer = layerNames.contains("domain")
    val hasInfrastructureLayer = layerNames.contains("infrastructure")
    val hasApplicationLayer = layerNames.contains("application")
    
    // Check dependency directions
    val domainDependencies = dependencies.filter { it.fromLayer == "domain" }
    val domainIsIndependent = domainDependencies.all { it.toLayer == "domain" || it.toLayer == "infrastructure" }
    
    return when {
        hasDomainLayer && hasInfrastructureLayer && hasApplicationLayer && domainIsIndependent -> {
            // Check for hexagonal indicators
            val hasPortsAndAdapters = layerNames.any { it.contains("port") || it.contains("adapter") }
            if (hasPortsAndAdapters) ArchitecturePattern.HEXAGONAL else ArchitecturePattern.CLEAN
        }
        hasDomainLayer && domainIsIndependent -> ArchitecturePattern.ONION
        layerNames.size >= 3 -> ArchitecturePattern.LAYERED
        else -> ArchitecturePattern.UNKNOWN
    }
}

fun analyzeClass(classOrObject: KtClassOrObject, fileName: String): ClassAnalysis {
    val className = classOrObject.name ?: "Anonymous"
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

    val complexity = analyzeComplexity(methods)
    val suggestions = generateSuggestions(lcom, methodProps, props, complexity)

    return ClassAnalysis(
        className = className,
        fileName = fileName,
        lcom = lcom,
        methodCount = methods.size,
        propertyCount = props.size,
        methodDetails = methodProps,
        suggestions = suggestions,
        complexity = complexity
    )
}

data class Suggestion(
    val icon: String,
    val message: String,
    val tooltip: String
)

fun analyzeComplexity(methods: List<KtNamedFunction>): ComplexityAnalysis {
    val methodComplexities = methods.map { method ->
        val complexity = calculateCyclomaticComplexity(method)
        val lineCount = method.text.lines().size
        MethodComplexity(method.name ?: "anonymous", complexity, lineCount)
    }
    
    val totalComplexity = methodComplexities.sumOf { it.cyclomaticComplexity }
    val averageComplexity = if (methodComplexities.isNotEmpty()) {
        totalComplexity.toDouble() / methodComplexities.size
    } else 0.0
    val maxComplexity = methodComplexities.maxOfOrNull { it.cyclomaticComplexity } ?: 0
    val complexMethods = methodComplexities.filter { it.cyclomaticComplexity > 10 }
    
    return ComplexityAnalysis(
        methods = methodComplexities,
        totalComplexity = totalComplexity,
        averageComplexity = averageComplexity,
        maxComplexity = maxComplexity,
        complexMethods = complexMethods
    )
}

fun calculateCyclomaticComplexity(method: KtNamedFunction): Int {
    var complexity = 1 // Base complexity
    
    method.bodyExpression?.accept(object : KtTreeVisitorVoid() {
        override fun visitIfExpression(expression: KtIfExpression) {
            complexity++
            super.visitIfExpression(expression)
        }
        
        override fun visitWhenExpression(expression: KtWhenExpression) {
            // Add 1 for each when entry (branch)
            complexity += expression.entries.size
            super.visitWhenExpression(expression)
        }
        
        override fun visitForExpression(expression: KtForExpression) {
            complexity++
            super.visitForExpression(expression)
        }
        
        override fun visitWhileExpression(expression: KtWhileExpression) {
            complexity++
            super.visitWhileExpression(expression)
        }
        
        override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
            complexity++
            super.visitDoWhileExpression(expression)
        }
        
        override fun visitTryExpression(expression: KtTryExpression) {
            // Add 1 for the try block itself + 1 for each catch clause
            complexity += 1 + expression.catchClauses.size
            super.visitTryExpression(expression)
        }
        
        override fun visitBinaryExpression(expression: KtBinaryExpression) {
            // Add complexity for logical operators (&& and ||)
            when (expression.operationToken.toString()) {
                "ANDAND", "OROR" -> complexity++
            }
            super.visitBinaryExpression(expression)
        }
        
        override fun visitCallExpression(expression: KtCallExpression) {
            // Add complexity for elvis operator (?:) and safe calls that might branch
            val calleeText = expression.calleeExpression?.text
            if (calleeText?.contains("?:") == true) {
                complexity++
            }
            super.visitCallExpression(expression)
        }
    })
    
    return complexity
}

fun generateSuggestions(lcom: Int, methodProps: Map<String, Set<String>>, props: List<String>, complexity: ComplexityAnalysis): List<Suggestion> {
    val suggestions = mutableListOf<Suggestion>()
    
    // Specific actionable suggestions only
    val unusedProps = props.filter { prop -> 
        methodProps.values.none { it.contains(prop) } 
    }
    
    if (unusedProps.isNotEmpty()) {
        suggestions.add(Suggestion(
            "🔧",
            "${unusedProps.size} unused ${if (unusedProps.size == 1) "property" else "properties"}",
            "Unused: ${unusedProps.joinToString(", ")}. Remove dead code or integrate these properties into class functionality."
        ))
    }
    
    val methodsWithoutProps = methodProps.filter { it.value.isEmpty() }
    if (methodsWithoutProps.isNotEmpty()) {
        val methodCount = methodsWithoutProps.size
        val methodList = methodsWithoutProps.keys.take(3).joinToString(", ") + 
                        if (methodCount > 3) " +${methodCount - 3} more" else ""
        
        suggestions.add(Suggestion(
            "📤",
            "$methodCount ${if (methodCount == 1) "method doesn't" else "methods don't"} use properties",
            "Methods: $methodList. Consider moving to utility classes or making them static/extension functions."
        ))
    }
    
    // Pattern-based suggestions for poor cohesion
    if (lcom > 5 && methodProps.size > 8) {
        suggestions.add(Suggestion(
            "🔀",
            "Split large unfocused class",
            "Consider breaking into ${if (lcom > 8) "3-4" else "2-3"} smaller classes based on method-property relationships."
        ))
    } else if (lcom > 3) {
        suggestions.add(Suggestion(
            "🎯",
            "Group related functionality",
            "Look for methods that share properties and consider extracting them into focused classes."
        ))
    }
    
    // Method complexity suggestions
    val methodCount = methodProps.size
    if (methodCount > 15) {
        suggestions.add(Suggestion(
            "📏",
            "Class has $methodCount methods",
            "Large classes are harder to maintain. Consider if this class has too many responsibilities."
        ))
    }
    
    // Property usage patterns
    val propertyUsage = props.map { prop ->
        val usageCount = methodProps.values.count { it.contains(prop) }
        prop to usageCount
    }
    
    val lightlyUsedProps = propertyUsage.filter { it.second in 1..2 && methodCount > 3 }.map { it.first }
    
    if (lightlyUsedProps.isNotEmpty()) {
        suggestions.add(Suggestion(
            "⚡",
            "${lightlyUsedProps.size} rarely used ${if (lightlyUsedProps.size == 1) "property" else "properties"}",
            "Properties: ${lightlyUsedProps.joinToString(", ")}. Consider if these belong in a separate class."
        ))
    }
    
    // Complexity-based suggestions
    if (complexity.complexMethods.isNotEmpty()) {
        val complexMethodNames = complexity.complexMethods.take(3).joinToString(", ") { it.methodName } +
                if (complexity.complexMethods.size > 3) " +${complexity.complexMethods.size - 3} more" else ""
        
        suggestions.add(Suggestion(
            "🧠",
            "${complexity.complexMethods.size} complex ${if (complexity.complexMethods.size == 1) "method" else "methods"} (CC > 10)",
            "Methods: $complexMethodNames. Break down complex logic, extract helper methods, or simplify conditional logic."
        ))
    }
    
    if (complexity.averageComplexity > 7) {
        suggestions.add(Suggestion(
            "📊",
            "High average complexity (${String.format("%.1f", complexity.averageComplexity)})",
            "Consider refactoring methods to reduce branching logic and improve readability."
        ))
    }
    
    val veryComplexMethods = complexity.methods.filter { it.cyclomaticComplexity > 20 }
    if (veryComplexMethods.isNotEmpty()) {
        suggestions.add(Suggestion(
            "🚨",
            "${veryComplexMethods.size} very complex ${if (veryComplexMethods.size == 1) "method" else "methods"} (CC > 20)",
            "Methods: ${veryComplexMethods.joinToString(", ") { it.methodName }}. These methods are extremely difficult to test and maintain."
        ))
    }
    
    // No suggestions for well-designed classes
    if (suggestions.isEmpty() && lcom <= 2 && complexity.averageComplexity <= 5) {
        suggestions.add(Suggestion(
            "✨",
            "Well-designed class",
            "Good cohesion and low complexity. Consider this class as a model for others."
        ))
    }
    
    return suggestions
}

fun generateSummary(analyses: List<ClassAnalysis>, architectureAnalysis: ArchitectureAnalysis) {
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    
    println("\n" + "=".repeat(60))
    println("               📊 KOTLIN METRICS ANALYSIS SUMMARY")
    println("                    Generated: $timestamp")
    println("=".repeat(60))
    
    if (analyses.isEmpty()) {
        println("\n⚠️  No Kotlin classes found to analyze.")
        println("   Make sure you're running from a Kotlin project directory.")
        println("\n📄 Empty report generated: kotlin-metrics-report.html")
        println("=".repeat(60))
        return
    }
    
    // Project Overview
    val totalMethods = analyses.sumOf { it.complexity.methods.size }
    val totalProperties = analyses.sumOf { it.propertyCount }
    
    println("\n📈 PROJECT OVERVIEW")
    println("   Classes analyzed: ${analyses.size}")
    println("   Total methods: $totalMethods")
    println("   Total properties: $totalProperties")
    
    // Key Metrics
    val avgLcom = analyses.map { it.lcom }.average()
    val avgComplexity = analyses.map { it.complexity.averageComplexity }.average()
    
    println("\n🎯 KEY METRICS")
    println("   Average LCOM: ${String.format("%.2f", avgLcom)} ${getLcomQualityIcon(avgLcom)}")
    println("   Average Complexity: ${String.format("%.2f", avgComplexity)} ${getComplexityQualityIcon(avgComplexity)}")
    
    // Quality Distribution
    val qualityDistribution = analyses.groupBy { 
        when {
            it.lcom <= 2 && it.complexity.averageComplexity <= 5 -> "Excellent"
            it.lcom <= 5 && it.complexity.averageComplexity <= 7 -> "Good"
            it.lcom <= 8 && it.complexity.averageComplexity <= 10 -> "Moderate"
            else -> "Poor"
        }
    }
    
    println("\n📊 QUALITY DISTRIBUTION")
    qualityDistribution.forEach { (level, classes) ->
        val icon = when (level) {
            "Excellent" -> "✅"
            "Good" -> "👍"
            "Moderate" -> "⚠️"
            else -> "❌"
        }
        val percentage = (classes.size * 100.0 / analyses.size).let { "%.1f".format(it) }
        println("   $icon $level: ${classes.size} classes ($percentage%)")
    }
    
    // Issues Summary
    val complexMethods = analyses.sumOf { it.complexity.complexMethods.size }
    val veryComplexMethods = analyses.sumOf { analysis -> 
        analysis.complexity.methods.count { it.cyclomaticComplexity > 20 }
    }
    val poorCohesionClasses = analyses.count { it.lcom > 5 }
    
    if (complexMethods > 0 || poorCohesionClasses > 0) {
        println("\n⚠️  ISSUES DETECTED")
        if (poorCohesionClasses > 0) {
            println("   📊 $poorCohesionClasses ${if (poorCohesionClasses == 1) "class has" else "classes have"} poor cohesion (LCOM > 5)")
        }
        if (complexMethods > 0) {
            println("   🧠 $complexMethods ${if (complexMethods == 1) "method is" else "methods are"} complex (CC > 10)")
        }
        if (veryComplexMethods > 0) {
            println("   🚨 $veryComplexMethods ${if (veryComplexMethods == 1) "method is" else "methods are"} very complex (CC > 20)")
        }
    } else {
        println("\n✨ EXCELLENT CODE QUALITY")
        println("   No significant issues detected!")
        println("   All classes have good cohesion and low complexity.")
    }
    
    // Worst Offenders
    val worstClasses = analyses
        .filter { it.lcom > 5 || it.complexity.averageComplexity > 10 }
        .sortedWith(compareByDescending<ClassAnalysis> { it.lcom }.thenByDescending { it.complexity.averageComplexity })
    
    if (worstClasses.isNotEmpty()) {
        println("\n🎯 PRIORITY REFACTORING TARGETS")
        worstClasses.take(5).forEach { analysis ->
            val lcomBadge = if (analysis.lcom > 5) "LCOM:${analysis.lcom}" else ""
            val ccBadge = if (analysis.complexity.averageComplexity > 10) "CC:${String.format("%.1f", analysis.complexity.averageComplexity)}" else ""
            val badges = listOf(lcomBadge, ccBadge).filter { it.isNotEmpty() }.joinToString(" ")
            println("   📝 ${analysis.className} ($badges)")
        }
    }
    
    // Architecture Summary
    println("\n🏗️ ARCHITECTURE ANALYSIS")
    println("   Pattern: ${architectureAnalysis.layeredArchitecture.pattern}")
    println("   Layers: ${architectureAnalysis.layeredArchitecture.layers.size}")
    println("   Dependencies: ${architectureAnalysis.layeredArchitecture.dependencies.size}")
    println("   Violations: ${architectureAnalysis.layeredArchitecture.violations.size}")
    
    // DDD Patterns Summary
    val ddd = architectureAnalysis.dddPatterns
    println("\n📐 DDD PATTERNS DETECTED")
    println("   Entities: ${ddd.entities.size}")
    println("   Value Objects: ${ddd.valueObjects.size}")
    println("   Services: ${ddd.services.size}")
    println("   Repositories: ${ddd.repositories.size}")
    println("   Aggregates: ${ddd.aggregates.size}")
    
    // Dependency Graph Summary
    val graph = architectureAnalysis.dependencyGraph
    println("\n🌐 DEPENDENCY GRAPH")
    println("   Nodes: ${graph.nodes.size}")
    println("   Edges: ${graph.edges.size}")
    println("   Cycles: ${graph.cycles.size}")
    println("   Packages: ${graph.packages.size}")
    
    println("\n📄 Interactive report: kotlin-metrics-report.html")
    println("   Open in browser for detailed analysis, charts, and architecture visualization")
    println("=".repeat(60))
}

fun getLcomQualityIcon(avgLcom: Double): String = when {
    avgLcom <= 2 -> "✅"
    avgLcom <= 5 -> "👍"
    avgLcom <= 8 -> "⚠️"
    else -> "❌"
}

fun getComplexityQualityIcon(avgComplexity: Double): String = when {
    avgComplexity <= 5 -> "✅"
    avgComplexity <= 7 -> "👍"
    avgComplexity <= 10 -> "⚠️"
    else -> "❌"
}

// Java Analysis Functions
fun analyzeJavaClass(classDecl: ClassOrInterfaceDeclaration, fileName: String): ClassAnalysis {
    val className = classDecl.nameAsString
    val fields = classDecl.fields.map { it.variables.first().nameAsString }
    val methods = classDecl.methods
    val methodFieldUsage = mutableMapOf<String, Set<String>>()
    
    for (method in methods) {
        val usedFields = mutableSetOf<String>()
        method.accept(object : VoidVisitorAdapter<Void>() {
            override fun visit(n: NameExpr, arg: Void?) {
                if (fields.contains(n.nameAsString)) {
                    usedFields.add(n.nameAsString)
                }
                super.visit(n, arg)
            }
            
            override fun visit(n: FieldAccessExpr, arg: Void?) {
                val fieldName = n.nameAsString
                if (fields.contains(fieldName)) {
                    usedFields.add(fieldName)
                }
                super.visit(n, arg)
            }
        }, null)
        methodFieldUsage[method.nameAsString] = usedFields
    }
    
    // Calculate LCOM for Java
    val methodsList = methodFieldUsage.entries.toList()
    var pairsWithoutCommon = 0
    var pairsWithCommon = 0
    
    for (i in methodsList.indices) {
        for (j in i + 1 until methodsList.size) {
            val fields1 = methodsList[i].value
            val fields2 = methodsList[j].value
            if (fields1.intersect(fields2).isEmpty()) {
                pairsWithoutCommon++
            } else {
                pairsWithCommon++
            }
        }
    }
    
    var lcom = pairsWithoutCommon - pairsWithCommon
    if (lcom < 0) lcom = 0
    
    val complexity = analyzeJavaComplexity(methods)
    val suggestions = generateJavaSuggestions(lcom, methods.size, fields.size, complexity)
    
    return ClassAnalysis(
        className = className,
        fileName = fileName,
        lcom = lcom,
        methodCount = methods.size,
        propertyCount = fields.size,
        methodDetails = methodFieldUsage,
        suggestions = suggestions,
        complexity = complexity
    )
}

fun analyzeJavaComplexity(methods: List<MethodDeclaration>): ComplexityAnalysis {
    val methodComplexities = methods.map { method ->
        val complexity = calculateJavaCyclomaticComplexity(method)
        val lineCount = method.toString().lines().size
        MethodComplexity(method.nameAsString, complexity, lineCount)
    }
    
    val totalComplexity = methodComplexities.sumOf { it.cyclomaticComplexity }
    val averageComplexity = if (methodComplexities.isNotEmpty()) {
        totalComplexity.toDouble() / methodComplexities.size
    } else 0.0
    val maxComplexity = methodComplexities.maxOfOrNull { it.cyclomaticComplexity } ?: 0
    val complexMethods = methodComplexities.filter { it.cyclomaticComplexity > 10 }
    
    return ComplexityAnalysis(
        methods = methodComplexities,
        totalComplexity = totalComplexity,
        averageComplexity = averageComplexity,
        maxComplexity = maxComplexity,
        complexMethods = complexMethods
    )
}

fun calculateJavaCyclomaticComplexity(method: MethodDeclaration): Int {
    var complexity = 1 // Base complexity
    
    method.accept(object : VoidVisitorAdapter<Void>() {
        override fun visit(n: IfStmt, arg: Void?) {
            complexity++
            super.visit(n, arg)
        }
        
        override fun visit(n: SwitchStmt, arg: Void?) {
            complexity += n.entries.size
            super.visit(n, arg)
        }
        
        override fun visit(n: ForStmt, arg: Void?) {
            complexity++
            super.visit(n, arg)
        }
        
        override fun visit(n: ForEachStmt, arg: Void?) {
            complexity++
            super.visit(n, arg)
        }
        
        override fun visit(n: WhileStmt, arg: Void?) {
            complexity++
            super.visit(n, arg)
        }
        
        override fun visit(n: DoStmt, arg: Void?) {
            complexity++
            super.visit(n, arg)
        }
        
        override fun visit(n: TryStmt, arg: Void?) {
            complexity++ // try block
            complexity += n.catchClauses.size // each catch
            super.visit(n, arg)
        }
        
        override fun visit(n: ConditionalExpr, arg: Void?) {
            complexity++ // ternary operator
            super.visit(n, arg)
        }
        
        override fun visit(n: BinaryExpr, arg: Void?) {
            when (n.operator) {
                BinaryExpr.Operator.AND, BinaryExpr.Operator.OR -> complexity++
                else -> {}
            }
            super.visit(n, arg)
        }
    }, null)
    
    return complexity
}

fun generateJavaSuggestions(lcom: Int, methodCount: Int, fieldCount: Int, complexity: ComplexityAnalysis): List<Suggestion> {
    val suggestions = mutableListOf<Suggestion>()
    
    if (lcom > 0) {
        val icon = when {
            lcom > 10 -> "❌"
            lcom > 5 -> "⚠️"
            else -> "⚡"
        }
        suggestions.add(Suggestion(
            icon = icon,
            message = "Consider splitting this class (LCOM = $lcom)",
            tooltip = "LCOM measures lack of cohesion. Higher values suggest the class has multiple responsibilities."
        ))
    }
    
    if (complexity.maxComplexity > 10) {
        suggestions.add(Suggestion(
            icon = "❌",
            message = "High cyclomatic complexity detected (max: ${complexity.maxComplexity})",
            tooltip = "Methods with complexity > 10 are harder to test and maintain."
        ))
    }
    
    if (methodCount > 20) {
        suggestions.add(Suggestion(
            icon = "⚠️",
            message = "Large class with $methodCount methods",
            tooltip = "Classes with many methods might violate Single Responsibility Principle."
        ))
    }
    
    return suggestions
}

