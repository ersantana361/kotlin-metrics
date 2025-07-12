package com.metrics.util

/**
 * Utility class for calculating LCOM (Lack of Cohesion of Methods) values.
 */
object LcomCalculator {
    
    /**
     * Calculates LCOM using the traditional P-Q formula.
     * 
     * LCOM = P - Q, where:
     * - P = Number of method pairs that don't share properties
     * - Q = Number of method pairs that do share properties
     * 
     * Result is clamped to minimum 0.
     * 
     * @param methodPropertiesMap Map of method names to their used properties
     * @return LCOM value (minimum 0)
     */
    fun calculateLcom(methodPropertiesMap: Map<String, Set<String>>): Int {
        val methodsList = methodPropertiesMap.toList()
        
        if (methodsList.size <= 1) {
            return 0
        }
        
        var pairsWithoutCommon = 0
        var pairsWithCommon = 0
        
        // Compare each pair of methods
        for (i in methodsList.indices) {
            for (j in i + 1 until methodsList.size) {
                val props1 = methodsList[i].second
                val props2 = methodsList[j].second
                
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
    
    /**
     * Gets a cohesion level description based on LCOM value.
     */
    fun getCohesionLevel(lcom: Int): String {
        return when (lcom) {
            0 -> "Excellent"
            in 1..2 -> "Good"
            in 3..5 -> "Fair"
            in 6..10 -> "Poor"
            else -> "Very Poor"
        }
    }
    
    /**
     * Gets a cohesion badge emoji based on LCOM value.
     */
    fun getCohesionBadge(lcom: Int): String {
        return when (lcom) {
            0 -> "✅"
            in 1..2 -> "👍"
            in 3..5 -> "⚠️"
            else -> "❌"
        }
    }
}