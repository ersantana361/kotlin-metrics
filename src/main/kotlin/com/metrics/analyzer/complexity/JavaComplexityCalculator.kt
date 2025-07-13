package com.metrics.analyzer.complexity

import com.metrics.analyzer.core.ComplexityCalculator
import com.metrics.model.analysis.ComplexityAnalysis
import com.metrics.model.analysis.MethodComplexity
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.visitor.VoidVisitorAdapter

/**
 * Cyclomatic complexity calculator for Java classes and methods.
 */
class JavaComplexityCalculator : ComplexityCalculator<ClassOrInterfaceDeclaration, MethodDeclaration> {
    
    override fun analyzeClassComplexity(classNode: ClassOrInterfaceDeclaration): ComplexityAnalysis {
        val methods = classNode.methods
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
    
    override fun calculateMethodComplexity(methodNode: MethodDeclaration): MethodComplexity {
        val complexity = calculateCyclomaticComplexity(methodNode)
        val lineCount = methodNode.toString().lines().size
        return MethodComplexity(methodNode.nameAsString, complexity, lineCount)
    }
    
    private fun calculateCyclomaticComplexity(method: MethodDeclaration): Int {
        var complexity = 1 // Base complexity
        
        method.accept(object : VoidVisitorAdapter<Void>() {
            override fun visit(n: IfStmt, arg: Void?) {
                complexity++
                super.visit(n, arg)
            }
            
            override fun visit(n: SwitchStmt, arg: Void?) {
                // Add 1 for each case (including default)
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
                // Add 1 for try block + 1 for each catch clause
                complexity += 1 + n.catchClauses.size
                super.visit(n, arg)
            }
            
            override fun visit(n: BinaryExpr, arg: Void?) {
                // Add complexity for logical operators (&& and ||)
                when (n.operator) {
                    BinaryExpr.Operator.AND, BinaryExpr.Operator.OR -> complexity++
                    else -> {}
                }
                super.visit(n, arg)
            }
        }, null)
        
        return complexity
    }
}