package com.metrics.util

import com.metrics.model.architecture.DependencyNode
import com.metrics.model.architecture.DependencyEdge
import com.metrics.model.architecture.DependencyCycle
import com.metrics.model.common.CycleSeverity

/**
 * Utility class for detecting circular dependencies and analyzing dependency cycles.
 */
object CycleDetectionUtils {
    
    /**
     * Detects circular dependencies using Depth-First Search (DFS) algorithm.
     * 
     * @param nodes List of dependency nodes
     * @param edges List of dependency edges
     * @return List of detected dependency cycles
     */
    fun detectCycles(nodes: List<DependencyNode>, edges: List<DependencyEdge>): List<DependencyCycle> {
        val cycles = mutableListOf<DependencyCycle>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        val path = mutableListOf<String>()
        
        // Build adjacency list for efficient traversal
        val adjacencyList = buildAdjacencyList(edges)
        
        // Perform DFS from each unvisited node
        for (node in nodes) {
            if (node.id !in visited) {
                findCyclesFromNode(
                    node.id, 
                    adjacencyList, 
                    visited, 
                    recursionStack, 
                    path, 
                    cycles,
                    nodes
                )
            }
        }
        
        return cycles.distinctBy { it.nodes.sorted() }
    }
    
    /**
     * Calculates package cohesion based on internal vs external dependencies.
     * 
     * @param packageName The package to analyze
     * @param edges All dependency edges
     * @param nodes All dependency nodes
     * @return Cohesion value between 0.0 and 1.0
     */
    fun calculatePackageCohesion(
        packageName: String, 
        edges: List<DependencyEdge>, 
        nodes: List<DependencyNode>
    ): Double {
        val packageNodes = nodes.filter { it.id.startsWith(packageName) }
        if (packageNodes.size <= 1) return 1.0
        
        val internalEdges = edges.count { edge ->
            edge.fromId.startsWith(packageName) && edge.toId.startsWith(packageName)
        }
        
        val externalEdges = edges.count { edge ->
            edge.fromId.startsWith(packageName) && !edge.toId.startsWith(packageName)
        }
        
        val totalEdges = internalEdges + externalEdges
        return if (totalEdges > 0) {
            internalEdges.toDouble() / totalEdges
        } else {
            1.0
        }
    }
    
    /**
     * Analyzes the impact of a cycle based on the classes involved.
     */
    fun analyzeCycleImpact(cycle: DependencyCycle, nodes: List<DependencyNode>): CycleSeverity {
        val involvedNodes = nodes.filter { it.id in cycle.nodes }
        
        // Count different packages involved
        val packagesInvolved = involvedNodes.map { 
            it.id.substringBeforeLast(".") 
        }.distinct().size
        
        // Count classes in cycle
        val classCount = cycle.nodes.size
        
        // Determine severity based on multiple factors
        return when {
            packagesInvolved >= 3 || classCount >= 5 -> CycleSeverity.HIGH
            packagesInvolved >= 2 || classCount >= 3 -> CycleSeverity.MEDIUM
            else -> CycleSeverity.LOW
        }
    }
    
    private fun buildAdjacencyList(edges: List<DependencyEdge>): Map<String, List<String>> {
        val adjacencyList = mutableMapOf<String, MutableList<String>>()
        
        for (edge in edges) {
            adjacencyList.computeIfAbsent(edge.fromId) { mutableListOf() }.add(edge.toId)
        }
        
        return adjacencyList
    }
    
    private fun findCyclesFromNode(
        nodeId: String,
        adjacencyList: Map<String, List<String>>,
        visited: MutableSet<String>,
        recursionStack: MutableSet<String>,
        path: MutableList<String>,
        cycles: MutableList<DependencyCycle>,
        allNodes: List<DependencyNode>
    ) {
        visited.add(nodeId)
        recursionStack.add(nodeId)
        path.add(nodeId)
        
        val neighbors = adjacencyList[nodeId] ?: emptyList()
        
        for (neighbor in neighbors) {
            if (neighbor in recursionStack) {
                // Found a cycle - extract the cycle path
                val cycleStartIndex = path.indexOf(neighbor)
                if (cycleStartIndex >= 0) {
                    val cyclePath = path.subList(cycleStartIndex, path.size) + neighbor
                    
                    val cycle = DependencyCycle(
                        nodes = cyclePath,
                        severity = CycleSeverity.MEDIUM // Will be updated by analyzeCycleImpact
                    )
                    
                    // Update severity
                    val updatedCycle = cycle.copy(severity = analyzeCycleImpact(cycle, allNodes))
                    cycles.add(updatedCycle)
                }
            } else if (neighbor !in visited) {
                findCyclesFromNode(
                    neighbor, 
                    adjacencyList, 
                    visited, 
                    recursionStack, 
                    path, 
                    cycles, 
                    allNodes
                )
            }
        }
        
        recursionStack.remove(nodeId)
        path.removeAt(path.size - 1)
    }
    
    /**
     * Groups cycles by severity for reporting.
     */
    fun groupCyclesBySeverity(cycles: List<DependencyCycle>): Map<CycleSeverity, List<DependencyCycle>> {
        return cycles.groupBy { it.severity }
    }
    
    /**
     * Finds the shortest cycle path between two classes.
     */
    fun findShortestCyclePath(
        fromClass: String, 
        toClass: String, 
        edges: List<DependencyEdge>
    ): List<String>? {
        val adjacencyList = buildAdjacencyList(edges)
        val queue = mutableListOf<List<String>>()
        val visited = mutableSetOf<String>()
        
        queue.add(listOf(fromClass))
        
        while (queue.isNotEmpty()) {
            val path = queue.removeAt(0)
            val current = path.last()
            
            if (current == toClass && path.size > 1) {
                return path
            }
            
            if (current in visited) continue
            visited.add(current)
            
            val neighbors = adjacencyList[current] ?: emptyList()
            for (neighbor in neighbors) {
                if (neighbor !in visited || (neighbor == toClass && path.size > 1)) {
                    queue.add(path + neighbor)
                }
            }
        }
        
        return null
    }
}