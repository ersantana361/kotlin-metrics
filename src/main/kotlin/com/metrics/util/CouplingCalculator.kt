package com.metrics.util

import org.jetbrains.kotlin.psi.*
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.metrics.model.analysis.CouplingRelation
import com.metrics.model.analysis.CouplingType

/**
 * Calculator for coupling metrics: CBO, RFC, CA, CE.
 */
object CouplingCalculator {
    
    /**
     * Calculates CBO (Coupling Between Objects) for a Kotlin class.
     * CBO is the number of classes this class is coupled to.
     */
    fun calculateCbo(classOrObject: KtClassOrObject, allClasses: List<KtClassOrObject>): Int {
        val className = classOrObject.name ?: return 0
        val coupledClasses = mutableSetOf<String>()
        
        // Check imports and type references in the class
        val classText = classOrObject.text
        
        allClasses.forEach { otherClass ->
            val otherClassName = otherClass.name
            if (otherClassName != null && otherClassName != className) {
                if (classText.contains(otherClassName)) {
                    coupledClasses.add(otherClassName)
                }
            }
        }
        
        return coupledClasses.size
    }
    
    /**
     * Calculates RFC (Response For a Class) for a Kotlin class.
     * RFC is the number of methods that can be invoked in response to a message.
     */
    fun calculateRfc(classOrObject: KtClassOrObject): Int {
        val methods = classOrObject.declarations.filterIsInstance<KtNamedFunction>()
        val externalMethods = mutableSetOf<String>()
        
        // Count local methods
        var localMethods = methods.size
        
        // Count external method calls
        methods.forEach { method ->
            val methodText = method.text
            
            // Simple pattern matching for method calls (this is a simplified approach)
            val methodCallPattern = Regex("""(\w+)\s*\(""")
            val matches = methodCallPattern.findAll(methodText)
            
            matches.forEach { match ->
                val calledMethod = match.groupValues[1]
                if (!methods.any { it.name == calledMethod }) {
                    externalMethods.add(calledMethod)
                }
            }
        }
        
        return localMethods + externalMethods.size
    }
    
    /**
     * Calculates CA (Afferent Coupling) for a Kotlin class.
     * CA is the number of classes that depend on this class.
     */
    fun calculateCa(classOrObject: KtClassOrObject, allClasses: List<KtClassOrObject>): Int {
        val className = classOrObject.name ?: return 0
        
        return allClasses.count { otherClass ->
            val otherClassName = otherClass.name
            otherClassName != null && otherClassName != className && 
            otherClass.text.contains(className)
        }
    }
    
    /**
     * Calculates CE (Efferent Coupling) for a Kotlin class.
     * CE is the number of classes this class depends on.
     */
    fun calculateCe(classOrObject: KtClassOrObject, allClasses: List<KtClassOrObject>): Int {
        return calculateCbo(classOrObject, allClasses) // CE is essentially the same as CBO
    }
    
    /**
     * Calculates CBO for a Java class.
     */
    fun calculateJavaCbo(classDecl: ClassOrInterfaceDeclaration, allClasses: List<ClassOrInterfaceDeclaration>): Int {
        val className = classDecl.nameAsString
        val coupledClasses = mutableSetOf<String>()
        
        val classText = classDecl.toString()
        
        allClasses.forEach { otherClass ->
            val otherClassName = otherClass.nameAsString
            if (otherClassName != className && classText.contains(otherClassName)) {
                coupledClasses.add(otherClassName)
            }
        }
        
        return coupledClasses.size
    }
    
    /**
     * Calculates RFC for a Java class.
     */
    fun calculateJavaRfc(classDecl: ClassOrInterfaceDeclaration): Int {
        val methods = classDecl.getMethods()
        val externalMethods = mutableSetOf<String>()
        
        var localMethods = methods.size
        
        methods.forEach { method ->
            val methodText = method.toString()
            
            // Simple pattern matching for method calls
            val methodCallPattern = Regex("""(\w+)\s*\(""")
            val matches = methodCallPattern.findAll(methodText)
            
            matches.forEach { match ->
                val calledMethod = match.groupValues[1]
                if (!methods.any { it.nameAsString == calledMethod }) {
                    externalMethods.add(calledMethod)
                }
            }
        }
        
        return localMethods + externalMethods.size
    }
    
    /**
     * Calculates CA for a Java class.
     */
    fun calculateJavaCa(classDecl: ClassOrInterfaceDeclaration, allClasses: List<ClassOrInterfaceDeclaration>): Int {
        val className = classDecl.nameAsString
        
        return allClasses.count { otherClass ->
            val otherClassName = otherClass.nameAsString
            otherClassName != className && otherClass.toString().contains(className)
        }
    }
    
    /**
     * Calculates CE for a Java class.
     */
    fun calculateJavaCe(classDecl: ClassOrInterfaceDeclaration, allClasses: List<ClassOrInterfaceDeclaration>): Int {
        return calculateJavaCbo(classDecl, allClasses)
    }
    
    /**
     * Creates coupling matrix between classes.
     */
    fun createCouplingMatrix(
        kotlinClasses: List<KtClassOrObject>,
        javaClasses: List<ClassOrInterfaceDeclaration>
    ): List<CouplingRelation> {
        val relations = mutableListOf<CouplingRelation>()
        
        // Kotlin to Kotlin coupling
        kotlinClasses.forEach { fromClass ->
            kotlinClasses.forEach { toClass ->
                if (fromClass != toClass) {
                    val strength = calculateCouplingStrength(fromClass.text, toClass.name ?: "")
                    if (strength > 0) {
                        relations.add(CouplingRelation(
                            fromClass = fromClass.name ?: "Unknown",
                            toClass = toClass.name ?: "Unknown",
                            strength = strength,
                            type = determineCouplingType(fromClass, toClass)
                        ))
                    }
                }
            }
        }
        
        // Java to Java coupling
        javaClasses.forEach { fromClass ->
            javaClasses.forEach { toClass ->
                if (fromClass != toClass) {
                    val strength = calculateCouplingStrength(fromClass.toString(), toClass.nameAsString)
                    if (strength > 0) {
                        relations.add(CouplingRelation(
                            fromClass = fromClass.nameAsString,
                            toClass = toClass.nameAsString,
                            strength = strength,
                            type = CouplingType.USAGE // Simplified for Java
                        ))
                    }
                }
            }
        }
        
        return relations
    }
    
    /**
     * Gets CBO quality assessment.
     */
    fun getCboAssessment(cbo: Int): String {
        return when {
            cbo <= 5 -> "Low coupling - excellent design"
            cbo <= 10 -> "Moderate coupling - acceptable"
            cbo <= 20 -> "High coupling - consider reducing dependencies"
            else -> "Very high coupling - critical refactoring needed"
        }
    }
    
    /**
     * Gets coupling quality level (1-10 scale).
     */
    fun getCouplingQualityLevel(cbo: Int, rfc: Int, ca: Int, ce: Int): Double {
        val avgCoupling = (cbo + (rfc / 5) + ca + ce) / 4.0
        return when {
            avgCoupling <= 3 -> 10.0
            avgCoupling <= 6 -> 8.0
            avgCoupling <= 10 -> 6.0
            avgCoupling <= 15 -> 4.0
            avgCoupling <= 25 -> 2.0
            else -> 1.0
        }
    }
    
    private fun calculateCouplingStrength(fromClassText: String, toClassName: String): Int {
        return fromClassText.split(toClassName).size - 1
    }
    
    private fun determineCouplingType(fromClass: KtClassOrObject, toClass: KtClassOrObject): CouplingType {
        val fromText = fromClass.text
        val toClassName = toClass.name ?: return CouplingType.USAGE
        
        return when {
            fromText.contains(": $toClassName") -> CouplingType.INHERITANCE
            fromText.contains("private val.*$toClassName".toRegex()) -> CouplingType.COMPOSITION
            fromText.contains("interface.*$toClassName".toRegex()) -> CouplingType.INTERFACE_IMPLEMENTATION
            else -> CouplingType.USAGE
        }
    }
}