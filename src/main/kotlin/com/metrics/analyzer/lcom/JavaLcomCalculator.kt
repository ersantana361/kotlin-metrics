package com.metrics.analyzer.lcom

import com.metrics.analyzer.core.LcomCalculator
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.visitor.VoidVisitorAdapter

/**
 * LCOM (Lack of Cohesion of Methods) calculator for Java classes.
 */
class JavaLcomCalculator : LcomCalculator<ClassOrInterfaceDeclaration> {
    
    override fun calculateLcom(classNode: ClassOrInterfaceDeclaration): Int {
        val methodFieldRelationships = analyzeMethodPropertyRelationships(classNode)
        return calculateLcomFromRelationships(methodFieldRelationships)
    }
    
    override fun analyzeMethodPropertyRelationships(classNode: ClassOrInterfaceDeclaration): Map<String, Set<String>> {
        val fields = classNode.fields.map { it.variables.first().nameAsString }
        val methods = classNode.methods
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
        
        return methodFieldUsage
    }
    
    /**
     * Calculates LCOM using the P-Q formula.
     * LCOM = P - Q, where:
     * P = number of method pairs with no common field usage
     * Q = number of method pairs with common field usage
     * Result is clamped to minimum 0.
     */
    private fun calculateLcomFromRelationships(methodFieldRelationships: Map<String, Set<String>>): Int {
        val methodsList = methodFieldRelationships.entries.toList()
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
        return lcom
    }
}