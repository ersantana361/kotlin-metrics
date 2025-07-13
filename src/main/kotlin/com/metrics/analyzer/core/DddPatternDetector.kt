package com.metrics.analyzer.core

import com.metrics.model.architecture.*
import java.io.File

/**
 * Interface for detecting Domain-Driven Design (DDD) patterns in code.
 */
interface DddPatternDetector {
    /**
     * Analyzes files to detect DDD patterns.
     * 
     * @param files Source code files to analyze
     * @return DddPatternAnalysis containing detected entities, services, repositories, etc.
     */
    fun detectPatterns(files: List<File>): DddPatternAnalysis
    
    /**
     * Detects if a class represents a DDD Entity.
     */
    fun isEntity(classNode: Any, className: String, fileName: String): DddEntity?
    
    /**
     * Detects if a class represents a DDD Value Object.
     */
    fun isValueObject(classNode: Any, className: String, fileName: String): DddValueObject?
    
    /**
     * Detects if a class represents a DDD Service.
     */
    fun isService(classNode: Any, className: String, fileName: String): DddService?
    
    /**
     * Detects if a class represents a DDD Repository.
     */
    fun isRepository(classNode: Any, className: String, fileName: String): DddRepository?
}