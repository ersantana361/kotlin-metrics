package com.metrics.analyzer

import com.metrics.util.MethodSignature
import com.metrics.util.ParameterInfo
import com.metrics.util.SourceContext
import com.metrics.util.SourceContextLoader
import com.metrics.util.SourceLanguage
import java.io.File

/**
 * Analyzes semantic changes in code, detecting modifications that affect behavior,
 * API surface, and contract compliance.
 */
class SemanticChangeAnalyzer(
    private val sourceContextLoader: SourceContextLoader
) {
    
    /**
     * Analyzes semantic changes between two versions of source files.
     * 
     * @param beforeFiles Source files from the before version
     * @param afterFiles Source files from the after version
     * @return SemanticChangeAnalysis with detected changes
     */
    fun analyzeSemanticChanges(
        beforeFiles: List<File>,
        afterFiles: List<File>
    ): SemanticChangeAnalysis {
        
        val beforeContexts = sourceContextLoader.loadMultipleContexts(beforeFiles)
        val afterContexts = sourceContextLoader.loadMultipleContexts(afterFiles)
        
        val methodChanges = analyzeMethodChanges(beforeContexts, afterContexts)
        val signatureChanges = analyzeSignatureChanges(beforeContexts, afterContexts)
        val behavioralChanges = analyzeBehavioralChanges(beforeContexts, afterContexts)
        val apiChanges = analyzeApiChanges(beforeContexts, afterContexts)
        
        val changesSummary = generateChangesSummary(methodChanges, signatureChanges, behavioralChanges, apiChanges)
        
        return SemanticChangeAnalysis(
            methodChanges = methodChanges,
            signatureChanges = signatureChanges,
            behavioralChanges = behavioralChanges,
            apiChanges = apiChanges,
            changesSummary = changesSummary
        )
    }
    
    /**
     * Analyzes changes in method definitions.
     */
    private fun analyzeMethodChanges(
        beforeContexts: Map<String, SourceContext>,
        afterContexts: Map<String, SourceContext>
    ): List<MethodChange> {
        
        val changes = mutableListOf<MethodChange>()
        
        // Compare methods in modified files
        beforeContexts.forEach { (filePath, beforeContext) ->
            afterContexts[filePath]?.let { afterContext ->
                val beforeMethods = sourceContextLoader.extractMethodSignatures(beforeContext)
                val afterMethods = sourceContextLoader.extractMethodSignatures(afterContext)
                
                // Find added methods
                val addedMethods = afterMethods.filter { afterMethod ->
                    beforeMethods.none { beforeMethod ->
                        beforeMethod.name == afterMethod.name &&
                        beforeMethod.className == afterMethod.className
                    }
                }
                
                addedMethods.forEach { method ->
                    changes.add(MethodChange(
                        filePath = filePath,
                        methodName = method.name,
                        className = method.className,
                        changeType = MethodChangeType.ADDED,
                        beforeSignature = null,
                        afterSignature = method,
                        impactLevel = calculateMethodImpactLevel(MethodChangeType.ADDED, method)
                    ))
                }
                
                // Find removed methods
                val removedMethods = beforeMethods.filter { beforeMethod ->
                    afterMethods.none { afterMethod ->
                        afterMethod.name == beforeMethod.name &&
                        afterMethod.className == beforeMethod.className
                    }
                }
                
                removedMethods.forEach { method ->
                    changes.add(MethodChange(
                        filePath = filePath,
                        methodName = method.name,
                        className = method.className,
                        changeType = MethodChangeType.REMOVED,
                        beforeSignature = method,
                        afterSignature = null,
                        impactLevel = calculateMethodImpactLevel(MethodChangeType.REMOVED, method)
                    ))
                }
                
                // Find modified methods
                beforeMethods.forEach { beforeMethod ->
                    afterMethods.find { afterMethod ->
                        afterMethod.name == beforeMethod.name &&
                        afterMethod.className == beforeMethod.className
                    }?.let { afterMethod ->
                        if (!areMethodSignaturesEqual(beforeMethod, afterMethod)) {
                            changes.add(MethodChange(
                                filePath = filePath,
                                methodName = beforeMethod.name,
                                className = beforeMethod.className,
                                changeType = MethodChangeType.MODIFIED,
                                beforeSignature = beforeMethod,
                                afterSignature = afterMethod,
                                impactLevel = calculateMethodImpactLevel(MethodChangeType.MODIFIED, beforeMethod, afterMethod)
                            ))
                        }
                    }
                }
            }
        }
        
        return changes
    }
    
    /**
     * Analyzes changes in method signatures.
     */
    private fun analyzeSignatureChanges(
        beforeContexts: Map<String, SourceContext>,
        afterContexts: Map<String, SourceContext>
    ): List<SignatureChange> {
        
        val changes = mutableListOf<SignatureChange>()
        
        beforeContexts.forEach { (filePath, beforeContext) ->
            afterContexts[filePath]?.let { afterContext ->
                val beforeMethods = sourceContextLoader.extractMethodSignatures(beforeContext)
                val afterMethods = sourceContextLoader.extractMethodSignatures(afterContext)
                
                beforeMethods.forEach { beforeMethod ->
                    afterMethods.find { afterMethod ->
                        afterMethod.name == beforeMethod.name &&
                        afterMethod.className == beforeMethod.className
                    }?.let { afterMethod ->
                        
                        // Check parameter changes
                        val parameterChanges = analyzeParameterChanges(beforeMethod.parameters, afterMethod.parameters)
                        if (parameterChanges.isNotEmpty()) {
                            changes.add(SignatureChange(
                                filePath = filePath,
                                methodName = beforeMethod.name,
                                className = beforeMethod.className,
                                changeType = SignatureChangeType.PARAMETERS,
                                beforeSignature = beforeMethod,
                                afterSignature = afterMethod,
                                parameterChanges = parameterChanges,
                                breakingChange = isBreakingParameterChange(parameterChanges)
                            ))
                        }
                        
                        // Check return type changes
                        if (beforeMethod.returnType != afterMethod.returnType) {
                            changes.add(SignatureChange(
                                filePath = filePath,
                                methodName = beforeMethod.name,
                                className = beforeMethod.className,
                                changeType = SignatureChangeType.RETURN_TYPE,
                                beforeSignature = beforeMethod,
                                afterSignature = afterMethod,
                                parameterChanges = emptyList(),
                                breakingChange = isBreakingReturnTypeChange(beforeMethod.returnType, afterMethod.returnType)
                            ))
                        }
                        
                        // Check visibility changes
                        if (beforeMethod.visibility != afterMethod.visibility) {
                            changes.add(SignatureChange(
                                filePath = filePath,
                                methodName = beforeMethod.name,
                                className = beforeMethod.className,
                                changeType = SignatureChangeType.VISIBILITY,
                                beforeSignature = beforeMethod,
                                afterSignature = afterMethod,
                                parameterChanges = emptyList(),
                                breakingChange = isBreakingVisibilityChange(beforeMethod.visibility, afterMethod.visibility)
                            ))
                        }
                    }
                }
            }
        }
        
        return changes
    }
    
    /**
     * Analyzes behavioral changes in code logic.
     */
    private fun analyzeBehavioralChanges(
        beforeContexts: Map<String, SourceContext>,
        afterContexts: Map<String, SourceContext>
    ): List<BehavioralChange> {
        
        val changes = mutableListOf<BehavioralChange>()
        
        beforeContexts.forEach { (filePath, beforeContext) ->
            afterContexts[filePath]?.let { afterContext ->
                // Analyze changes in control flow complexity
                val complexityChanges = analyzeComplexityChanges(beforeContext, afterContext)
                changes.addAll(complexityChanges)
                
                // Analyze changes in conditional logic
                val conditionalChanges = analyzeConditionalChanges(beforeContext, afterContext)
                changes.addAll(conditionalChanges)
                
                // Analyze changes in exception handling
                val exceptionChanges = analyzeExceptionHandlingChanges(beforeContext, afterContext)
                changes.addAll(exceptionChanges)
            }
        }
        
        return changes
    }
    
    /**
     * Analyzes changes in API surface area.
     */
    private fun analyzeApiChanges(
        beforeContexts: Map<String, SourceContext>,
        afterContexts: Map<String, SourceContext>
    ): List<ApiChange> {
        
        val changes = mutableListOf<ApiChange>()
        
        beforeContexts.forEach { (filePath, beforeContext) ->
            afterContexts[filePath]?.let { afterContext ->
                val beforeMethods = sourceContextLoader.extractMethodSignatures(beforeContext)
                val afterMethods = sourceContextLoader.extractMethodSignatures(afterContext)
                
                // Find changes in public API
                val publicMethodChanges = analyzePublicMethodChanges(beforeMethods, afterMethods)
                changes.addAll(publicMethodChanges.map { methodChange ->
                    ApiChange(
                        filePath = filePath,
                        changeType = mapMethodChangeToApiChange(methodChange.changeType),
                        affectedElement = "${methodChange.className}.${methodChange.methodName}",
                        breakingChange = isBreakingApiChange(methodChange),
                        description = generateApiChangeDescription(methodChange)
                    )
                })
            }
        }
        
        return changes
    }
    
    // Helper methods
    
    private fun areMethodSignaturesEqual(method1: MethodSignature, method2: MethodSignature): Boolean {
        return method1.name == method2.name &&
               method1.returnType == method2.returnType &&
               method1.visibility == method2.visibility &&
               method1.parameters.size == method2.parameters.size &&
               method1.parameters.zip(method2.parameters).all { (param1, param2) ->
                   param1.name == param2.name &&
                   param1.type == param2.type &&
                   param1.hasDefault == param2.hasDefault
               }
    }
    
    private fun analyzeParameterChanges(
        beforeParams: List<ParameterInfo>,
        afterParams: List<ParameterInfo>
    ): List<ParameterChange> {
        
        val changes = mutableListOf<ParameterChange>()
        
        // Check for added parameters
        afterParams.forEachIndexed { index, param ->
            if (index >= beforeParams.size) {
                changes.add(ParameterChange(
                    type = ParameterChangeType.ADDED,
                    parameterName = param.name,
                    beforeType = null,
                    afterType = param.type,
                    position = index,
                    hasDefault = param.hasDefault
                ))
            }
        }
        
        // Check for removed parameters
        beforeParams.forEachIndexed { index, param ->
            if (index >= afterParams.size) {
                changes.add(ParameterChange(
                    type = ParameterChangeType.REMOVED,
                    parameterName = param.name,
                    beforeType = param.type,
                    afterType = null,
                    position = index,
                    hasDefault = param.hasDefault
                ))
            }
        }
        
        // Check for modified parameters
        val minSize = minOf(beforeParams.size, afterParams.size)
        for (i in 0 until minSize) {
            val beforeParam = beforeParams[i]
            val afterParam = afterParams[i]
            
            if (beforeParam.type != afterParam.type) {
                changes.add(ParameterChange(
                    type = ParameterChangeType.TYPE_CHANGED,
                    parameterName = beforeParam.name,
                    beforeType = beforeParam.type,
                    afterType = afterParam.type,
                    position = i,
                    hasDefault = afterParam.hasDefault
                ))
            }
        }
        
        return changes
    }
    
    private fun calculateMethodImpactLevel(
        changeType: MethodChangeType,
        beforeMethod: MethodSignature?,
        afterMethod: MethodSignature? = null
    ): ImpactLevel {
        
        return when (changeType) {
            MethodChangeType.ADDED -> ImpactLevel.LOW
            MethodChangeType.REMOVED -> {
                val method = beforeMethod ?: return ImpactLevel.HIGH
                if (method.visibility == "public") ImpactLevel.HIGH else ImpactLevel.MEDIUM
            }
            MethodChangeType.MODIFIED -> {
                val before = beforeMethod ?: return ImpactLevel.MEDIUM
                val after = afterMethod ?: return ImpactLevel.MEDIUM
                
                when {
                    before.visibility == "public" && after.visibility != "public" -> ImpactLevel.HIGH
                    before.returnType != after.returnType -> ImpactLevel.HIGH
                    before.parameters.size != after.parameters.size -> ImpactLevel.HIGH
                    else -> ImpactLevel.MEDIUM
                }
            }
        }
    }
    
    private fun isBreakingParameterChange(parameterChanges: List<ParameterChange>): Boolean {
        return parameterChanges.any { change ->
            when (change.type) {
                ParameterChangeType.ADDED -> !change.hasDefault
                ParameterChangeType.REMOVED -> true
                ParameterChangeType.TYPE_CHANGED -> true
                ParameterChangeType.POSITION_CHANGED -> true
            }
        }
    }
    
    private fun isBreakingReturnTypeChange(beforeType: String, afterType: String): Boolean {
        // Simplified logic - in practice, would need to check type compatibility
        return beforeType != afterType
    }
    
    private fun isBreakingVisibilityChange(beforeVisibility: String, afterVisibility: String): Boolean {
        val visibilityOrder = listOf("private", "internal", "protected", "public")
        val beforeIndex = visibilityOrder.indexOf(beforeVisibility)
        val afterIndex = visibilityOrder.indexOf(afterVisibility)
        
        return afterIndex < beforeIndex // Reducing visibility is breaking
    }
    
    private fun analyzeComplexityChanges(
        beforeContext: SourceContext,
        afterContext: SourceContext
    ): List<BehavioralChange> {
        // Simplified implementation - would need detailed complexity analysis
        return emptyList()
    }
    
    private fun analyzeConditionalChanges(
        beforeContext: SourceContext,
        afterContext: SourceContext
    ): List<BehavioralChange> {
        // Simplified implementation - would need detailed conditional analysis
        return emptyList()
    }
    
    private fun analyzeExceptionHandlingChanges(
        beforeContext: SourceContext,
        afterContext: SourceContext
    ): List<BehavioralChange> {
        // Simplified implementation - would need detailed exception analysis
        return emptyList()
    }
    
    private fun analyzePublicMethodChanges(
        beforeMethods: List<MethodSignature>,
        afterMethods: List<MethodSignature>
    ): List<MethodChange> {
        // Filter for public methods only and return changes
        return emptyList() // Simplified implementation
    }
    
    private fun mapMethodChangeToApiChange(changeType: MethodChangeType): ApiChangeType {
        return when (changeType) {
            MethodChangeType.ADDED -> ApiChangeType.ADDITION
            MethodChangeType.REMOVED -> ApiChangeType.REMOVAL
            MethodChangeType.MODIFIED -> ApiChangeType.MODIFICATION
        }
    }
    
    private fun isBreakingApiChange(methodChange: MethodChange): Boolean {
        return methodChange.changeType == MethodChangeType.REMOVED ||
               (methodChange.changeType == MethodChangeType.MODIFIED && 
                methodChange.beforeSignature?.visibility == "public")
    }
    
    private fun generateApiChangeDescription(methodChange: MethodChange): String {
        return when (methodChange.changeType) {
            MethodChangeType.ADDED -> "Added method ${methodChange.methodName}"
            MethodChangeType.REMOVED -> "Removed method ${methodChange.methodName}"
            MethodChangeType.MODIFIED -> "Modified method ${methodChange.methodName}"
        }
    }
    
    private fun generateChangesSummary(
        methodChanges: List<MethodChange>,
        signatureChanges: List<SignatureChange>,
        behavioralChanges: List<BehavioralChange>,
        apiChanges: List<ApiChange>
    ): ChangesSummary {
        
        val totalChanges = methodChanges.size + signatureChanges.size + behavioralChanges.size + apiChanges.size
        val breakingChanges = signatureChanges.count { it.breakingChange } + 
                             apiChanges.count { it.breakingChange }
        
        val riskLevel = when {
            breakingChanges > 0 -> ChangeRiskLevel.HIGH
            methodChanges.any { it.changeType == MethodChangeType.REMOVED } -> ChangeRiskLevel.HIGH
            totalChanges > 10 -> ChangeRiskLevel.MEDIUM
            totalChanges > 5 -> ChangeRiskLevel.LOW
            else -> ChangeRiskLevel.MINIMAL
        }
        
        return ChangesSummary(
            totalChanges = totalChanges,
            breakingChanges = breakingChanges,
            riskLevel = riskLevel,
            impactAssessment = "Analysis of ${totalChanges} changes with ${breakingChanges} breaking changes"
        )
    }
}

// Data classes for semantic change analysis

data class SemanticChangeAnalysis(
    val methodChanges: List<MethodChange>,
    val signatureChanges: List<SignatureChange>,
    val behavioralChanges: List<BehavioralChange>,
    val apiChanges: List<ApiChange>,
    val changesSummary: ChangesSummary
)

data class MethodChange(
    val filePath: String,
    val methodName: String,
    val className: String,
    val changeType: MethodChangeType,
    val beforeSignature: MethodSignature?,
    val afterSignature: MethodSignature?,
    val impactLevel: ImpactLevel
)

data class SignatureChange(
    val filePath: String,
    val methodName: String,
    val className: String,
    val changeType: SignatureChangeType,
    val beforeSignature: MethodSignature,
    val afterSignature: MethodSignature,
    val parameterChanges: List<ParameterChange>,
    val breakingChange: Boolean
)

data class BehavioralChange(
    val filePath: String,
    val changeType: BehavioralChangeType,
    val description: String,
    val impactLevel: ImpactLevel
)

data class ApiChange(
    val filePath: String,
    val changeType: ApiChangeType,
    val affectedElement: String,
    val breakingChange: Boolean,
    val description: String
)

data class ParameterChange(
    val type: ParameterChangeType,
    val parameterName: String,
    val beforeType: String?,
    val afterType: String?,
    val position: Int,
    val hasDefault: Boolean
)

data class ChangesSummary(
    val totalChanges: Int,
    val breakingChanges: Int,
    val riskLevel: ChangeRiskLevel,
    val impactAssessment: String
)

enum class MethodChangeType {
    ADDED,
    REMOVED,
    MODIFIED
}

enum class SignatureChangeType {
    PARAMETERS,
    RETURN_TYPE,
    VISIBILITY
}

enum class BehavioralChangeType {
    COMPLEXITY_CHANGE,
    CONDITIONAL_CHANGE,
    EXCEPTION_HANDLING_CHANGE
}

enum class ApiChangeType {
    ADDITION,
    REMOVAL,
    MODIFICATION
}

enum class ParameterChangeType {
    ADDED,
    REMOVED,
    TYPE_CHANGED,
    POSITION_CHANGED
}

enum class ImpactLevel {
    HIGH,
    MEDIUM,
    LOW,
    MINIMAL
}

enum class ChangeRiskLevel {
    HIGH,
    MEDIUM,
    LOW,
    MINIMAL
}