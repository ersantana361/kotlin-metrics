package com.metrics.analyzer.lcom

import com.metrics.analyzer.core.LcomCalculator
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType

/**
 * LCOM (Lack of Cohesion of Methods) calculator for Kotlin classes.
 */
class KotlinLcomCalculator : LcomCalculator<KtClassOrObject> {
    
    override fun calculateLcom(classNode: KtClassOrObject): Int {
        val methodPropertyRelationships = analyzeMethodPropertyRelationships(classNode)
        return calculateLcomFromRelationships(methodPropertyRelationships)
    }
    
    override fun analyzeMethodPropertyRelationships(classNode: KtClassOrObject): Map<String, Set<String>> {
        val properties = classNode.declarations.filterIsInstance<KtProperty>().map { it.name!! }
        val methods = classNode.body?.functions ?: emptyList()
        val methodProps = mutableMapOf<String, Set<String>>()

        for (method in methods) {
            val usedProps = mutableSetOf<String>()
            method.bodyExpression?.forEachDescendantOfType<KtNameReferenceExpression> {
                val name = it.getReferencedName()
                if (properties.contains(name)) {
                    usedProps.add(name)
                }
            }
            methodProps[method.name ?: "anonymous"] = usedProps
        }
        
        return methodProps
    }
    
    /**
     * Calculates LCOM using the P-Q formula.
     * LCOM = P - Q, where:
     * P = number of method pairs with no common property usage
     * Q = number of method pairs with common property usage
     * Result is clamped to minimum 0.
     */
    private fun calculateLcomFromRelationships(methodPropertyRelationships: Map<String, Set<String>>): Int {
        val methodsList = methodPropertyRelationships.entries.toList()
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
        return lcom
    }
}