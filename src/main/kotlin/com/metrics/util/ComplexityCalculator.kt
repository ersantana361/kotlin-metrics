package com.metrics.util

import org.jetbrains.kotlin.psi.*
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.expr.*

/**
 * Utility class for calculating cyclomatic complexity of methods.
 */
object ComplexityCalculator {
    
    /**
     * Calculates cyclomatic complexity for a Kotlin method.
     * Uses AST visitor pattern to count decision points.
     */
    fun calculateCyclomaticComplexity(method: KtNamedFunction): Int {
        var complexity = 1 // Base complexity
        
        method.bodyExpression?.accept(object : KtTreeVisitorVoid() {
            override fun visitIfExpression(expression: KtIfExpression) {
                complexity++ // Each if adds 1
                super.visitIfExpression(expression)
            }
            
            override fun visitWhenExpression(expression: KtWhenExpression) {
                complexity += expression.entries.size // Each when branch adds 1
                super.visitWhenExpression(expression)
            }
            
            override fun visitForExpression(expression: KtForExpression) {
                complexity++ // Each for loop adds 1
                super.visitForExpression(expression)
            }
            
            override fun visitWhileExpression(expression: KtWhileExpression) {
                complexity++ // Each while loop adds 1
                super.visitWhileExpression(expression)
            }
            
            override fun visitDoWhileExpression(expression: KtDoWhileExpression) {
                complexity++ // Each do-while loop adds 1
                super.visitDoWhileExpression(expression)
            }
            
            override fun visitTryExpression(expression: KtTryExpression) {
                complexity++ // Try block adds 1
                expression.catchClauses.forEach { _ ->
                    complexity++ // Each catch clause adds 1
                }
                super.visitTryExpression(expression)
            }
            
            override fun visitBinaryExpression(expression: KtBinaryExpression) {
                val operationToken = expression.operationToken
                if (operationToken == org.jetbrains.kotlin.lexer.KtTokens.ANDAND ||
                    operationToken == org.jetbrains.kotlin.lexer.KtTokens.OROR) {
                    complexity++ // Each && or || adds 1
                }
                super.visitBinaryExpression(expression)
            }
            
            override fun visitBreakExpression(expression: KtBreakExpression) {
                complexity++ // Each break adds 1
                super.visitBreakExpression(expression)
            }
            
            override fun visitContinueExpression(expression: KtContinueExpression) {
                complexity++ // Each continue adds 1
                super.visitContinueExpression(expression)
            }
            
            override fun visitReturnExpression(expression: KtReturnExpression) {
                // Multiple return statements add complexity
                if (expression.parent !is KtBlockExpression || 
                    (expression.parent as? KtBlockExpression)?.statements?.last() != expression) {
                    complexity++ // Early returns add 1
                }
                super.visitReturnExpression(expression)
            }
        })
        
        return complexity
    }
    
    /**
     * Calculates cyclomatic complexity for a Java method.
     */
    fun calculateJavaCyclomaticComplexity(method: MethodDeclaration): Int {
        var complexity = 1 // Base complexity
        
        method.body.ifPresent { body ->
            body.accept(object : com.github.javaparser.ast.visitor.VoidVisitorAdapter<Void>() {
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
                    complexity++ // Try block
                    complexity += n.catchClauses.size // Each catch
                    super.visit(n, arg)
                }
                
                override fun visit(n: BinaryExpr, arg: Void?) {
                    if (n.operator == BinaryExpr.Operator.AND || 
                        n.operator == BinaryExpr.Operator.OR) {
                        complexity++
                    }
                    super.visit(n, arg)
                }
                
                override fun visit(n: ConditionalExpr, arg: Void?) {
                    complexity++ // Ternary operator
                    super.visit(n, arg)
                }
                
                override fun visit(n: BreakStmt, arg: Void?) {
                    complexity++
                    super.visit(n, arg)
                }
                
                override fun visit(n: ContinueStmt, arg: Void?) {
                    complexity++
                    super.visit(n, arg)
                }
                
                override fun visit(n: ReturnStmt, arg: Void?) {
                    // Early returns add complexity (simplified check)
                    complexity++
                    super.visit(n, arg)
                }
            }, null)
        }
        
        return complexity
    }
    
    /**
     * Gets complexity level description.
     */
    fun getComplexityLevel(complexity: Int): String {
        return when (complexity) {
            1 -> "Simple"
            in 2..5 -> "Low"
            in 6..10 -> "Moderate"
            in 11..20 -> "High"
            else -> "Very High"
        }
    }
    
    /**
     * Gets complexity level color for UI.
     */
    fun getComplexityColor(complexity: Int): String {
        return when (complexity) {
            1 -> "#28a745" // Green
            in 2..5 -> "#17a2b8" // Blue
            in 6..10 -> "#ffc107" // Yellow
            in 11..20 -> "#fd7e14" // Orange
            else -> "#dc3545" // Red
        }
    }
    
    /**
     * Determines if a method is considered complex.
     */
    fun isComplex(complexity: Int): Boolean {
        return complexity > 10
    }
    
    /**
     * Determines if a method is considered very complex.
     */
    fun isVeryComplex(complexity: Int): Boolean {
        return complexity > 20
    }
    
    /**
     * Gets recommendation message for complexity level.
     */
    fun getComplexityRecommendation(complexity: Int): String {
        return when (complexity) {
            1 -> "✅ Excellent simplicity"
            in 2..5 -> "👍 Good complexity level"
            in 6..10 -> "⚠️ Consider simplifying"
            in 11..20 -> "🔧 Refactoring recommended"
            else -> "🚨 Critical - needs immediate attention"
        }
    }
}