package com.metrics.analyzer.core

import com.metrics.model.architecture.DependencyGraph
import java.io.File

/**
 * Interface for building dependency graphs from source code.
 */
interface DependencyGraphBuilder {
    /**
     * Builds a dependency graph from the given source files.
     * 
     * @param files Source code files to analyze
     * @return DependencyGraph containing nodes, edges, cycles, and package analysis
     */
    fun buildGraph(files: List<File>): DependencyGraph
}