package com.metrics.analyzer.core

import com.metrics.model.analysis.ProjectReport
import java.io.File

/**
 * Base interface for code analyzers.
 */
interface CodeAnalyzer {
    fun analyze(files: List<File>): ProjectReport
}