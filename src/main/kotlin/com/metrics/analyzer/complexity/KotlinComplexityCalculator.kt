package com.metrics.analyzer.complexity

import com.metrics.analyzer.core.ComplexityCalculator
import com.metrics.model.analysis.ComplexityAnalysis
import com.metrics.model.analysis.MethodComplexity
import org.jetbrains.kotlin.psi.*

/**
 * Cyclomatic complexity calculator for Kotlin classes and methods.
 */
class KotlinComplexityCalculator : ComplexityCalculator<KtClassOrObject, KtNamedFunction> {
    
    override fun analyzeClassComplexity(classNode: KtClassOrObject): ComplexityAnalysis {
        val methods = classNode.body?.functions ?: emptyList()
        val methodComplexities = methods.map { method ->
            calculateMethodComplexity(method)
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
    
    override fun calculateMethodComplexity(methodNode: KtNamedFunction): MethodComplexity {
        val complexity = calculateCyclomaticComplexity(methodNode)
        val lineCount = methodNode.text.lines().size
        return MethodComplexity(methodNode.name ?: "anonymous", complexity, lineCount)
    }
    
    private fun calculateCyclomaticComplexity(method: KtNamedFunction): Int {
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
}