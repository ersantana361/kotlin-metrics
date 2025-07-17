package com.metrics.util

import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * Performance optimization utilities for enhanced PR diff analysis.
 * Provides caching, parallel processing, and performance monitoring.
 */
class PerformanceOptimizer {
    
    private val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private val cache = mutableMapOf<String, Any>()
    private val performanceMetrics = mutableMapOf<String, Long>()
    
    /**
     * Executes a task with performance monitoring.
     * 
     * @param taskName Name of the task for performance tracking
     * @param task The task to execute
     * @return Result of the task execution
     */
    fun <T> withPerformanceMonitoring(taskName: String, task: () -> T): T {
        val result: T
        val executionTime = measureTimeMillis {
            result = task()
        }
        
        performanceMetrics[taskName] = executionTime
        return result
    }
    
    /**
     * Caches the result of an expensive operation.
     * 
     * @param key Cache key
     * @param expiration Expiration time in milliseconds
     * @param operation Operation to cache
     * @return Cached or computed result
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> cached(key: String, expiration: Long = 300000, operation: () -> T): T {
        val cacheKey = "$key:${System.currentTimeMillis() / expiration}"
        
        return if (cache.containsKey(cacheKey)) {
            cache[cacheKey] as T
        } else {
            val result = operation()
            cache[cacheKey] = result as Any
            
            // Clean up expired entries
            val currentWindow = System.currentTimeMillis() / expiration
            cache.keys.removeIf { cachedKey ->
                val keyWindow = cachedKey.substringAfter(":").toLongOrNull() ?: 0
                keyWindow < currentWindow - 1
            }
            
            result
        }
    }
    
    /**
     * Processes a list of items in parallel.
     * 
     * @param items Items to process
     * @param processor Function to process each item
     * @return List of processed results
     */
    fun <T, R> processInParallel(items: List<T>, processor: (T) -> R): List<R> {
        val futures = items.map { item ->
            executor.submit<R> { processor(item) }
        }
        
        return futures.map { it.get() }
    }
    
    /**
     * Processes a list of items in parallel with a timeout.
     * 
     * @param items Items to process
     * @param processor Function to process each item
     * @param timeout Timeout in milliseconds
     * @return List of processed results (may be incomplete if timeout occurs)
     */
    fun <T, R> processInParallelWithTimeout(
        items: List<T>, 
        processor: (T) -> R, 
        timeout: Long = 30000
    ): List<R> {
        val futures = items.map { item ->
            executor.submit<R> { processor(item) }
        }
        
        val results = mutableListOf<R>()
        futures.forEach { future ->
            try {
                results.add(future.get(timeout, TimeUnit.MILLISECONDS))
            } catch (e: Exception) {
                // Log timeout or other errors, but continue processing
                System.err.println("Task failed or timed out: ${e.message}")
            }
        }
        
        return results
    }
    
    /**
     * Batches items for processing to avoid memory issues.
     * 
     * @param items Items to process
     * @param batchSize Size of each batch
     * @param processor Function to process each batch
     * @return Flattened list of processed results
     */
    fun <T, R> processBatched(
        items: List<T>, 
        batchSize: Int = 100, 
        processor: (List<T>) -> List<R>
    ): List<R> {
        val results = mutableListOf<R>()
        
        items.chunked(batchSize).forEach { batch ->
            results.addAll(processor(batch))
        }
        
        return results
    }
    
    /**
     * Creates a lazy sequence for memory-efficient processing.
     * 
     * @param items Items to process
     * @param processor Function to process each item
     * @return Lazy sequence of processed results
     */
    fun <T, R> processLazily(items: List<T>, processor: (T) -> R): Sequence<R> {
        return items.asSequence().map { processor(it) }
    }
    
    /**
     * Gets performance metrics for all monitored tasks.
     * 
     * @return Map of task names to execution times in milliseconds
     */
    fun getPerformanceMetrics(): Map<String, Long> {
        return performanceMetrics.toMap()
    }
    
    /**
     * Clears performance metrics.
     */
    fun clearPerformanceMetrics() {
        performanceMetrics.clear()
    }
    
    /**
     * Clears the cache.
     */
    fun clearCache() {
        cache.clear()
    }
    
    /**
     * Shuts down the executor service.
     */
    fun shutdown() {
        executor.shutdown()
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }
    
    /**
     * Optimizes source file loading for large projects.
     * 
     * @param files Files to load
     * @param loader Function to load each file
     * @return Map of file paths to loaded contexts
     */
    fun <T> optimizeSourceLoading(files: List<java.io.File>, loader: (java.io.File) -> T?): Map<String, T> {
        val results = mutableMapOf<String, T>()
        
        // Process files in parallel batches
        val batchSize = maxOf(1, files.size / Runtime.getRuntime().availableProcessors())
        
        files.chunked(batchSize).forEach { batch ->
            val batchResults = processInParallel(batch) { file ->
                cached("file:${file.path}:${file.lastModified()}") {
                    loader(file)
                }
            }
            
            batch.zip(batchResults).forEach { (file, result) ->
                result?.let { results[file.path] = it }
            }
        }
        
        return results
    }
    
    /**
     * Optimizes diff parsing for large diffs.
     * 
     * @param diffContent The diff content to parse
     * @param parser The diff parser
     * @return Parsed diff result
     */
    fun optimizeDiffParsing(diffContent: String, parser: DiffParser): com.metrics.util.ParsedDiff {
        return cached("diff:${diffContent.hashCode()}") {
            withPerformanceMonitoring("diff_parsing") {
                parser.parse(diffContent)
            }
        }
    }
    
    /**
     * Optimizes metrics calculation by caching expensive operations.
     * 
     * @param className Class name for caching
     * @param sourceContent Source content for cache validation
     * @param calculator Function to calculate metrics
     * @return Calculated metrics
     */
    fun <T> optimizeMetricsCalculation(
        className: String, 
        sourceContent: String, 
        calculator: () -> T
    ): T {
        val contentHash = sourceContent.hashCode()
        return cached("metrics:$className:$contentHash") {
            withPerformanceMonitoring("metrics_calculation:$className") {
                calculator()
            }
        }
    }
}

/**
 * Extension functions for performance optimization.
 */
fun <T> List<T>.processInParallel(optimizer: PerformanceOptimizer, processor: (T) -> Any) {
    optimizer.processInParallel(this, processor)
}

fun <T> List<T>.processBatched(optimizer: PerformanceOptimizer, batchSize: Int = 100, processor: (List<T>) -> List<Any>) {
    optimizer.processBatched(this, batchSize, processor)
}

/**
 * Memory usage monitor for tracking resource consumption.
 */
class MemoryMonitor {
    
    private val runtime = Runtime.getRuntime()
    
    /**
     * Gets current memory usage information.
     * 
     * @return Memory usage statistics
     */
    fun getMemoryUsage(): MemoryUsage {
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        
        return MemoryUsage(
            totalMemory = totalMemory,
            freeMemory = freeMemory,
            usedMemory = usedMemory,
            maxMemory = maxMemory,
            usagePercentage = (usedMemory.toDouble() / maxMemory.toDouble()) * 100
        )
    }
    
    /**
     * Checks if memory usage is above threshold.
     * 
     * @param threshold Threshold percentage (0-100)
     * @return True if memory usage is above threshold
     */
    fun isMemoryUsageHigh(threshold: Double = 80.0): Boolean {
        return getMemoryUsage().usagePercentage > threshold
    }
    
    /**
     * Suggests garbage collection if memory usage is high.
     */
    fun suggestGC(threshold: Double = 85.0) {
        if (isMemoryUsageHigh(threshold)) {
            System.gc()
        }
    }
}

/**
 * Memory usage statistics.
 */
data class MemoryUsage(
    val totalMemory: Long,
    val freeMemory: Long,
    val usedMemory: Long,
    val maxMemory: Long,
    val usagePercentage: Double
)