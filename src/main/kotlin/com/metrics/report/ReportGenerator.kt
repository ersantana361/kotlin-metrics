package com.metrics.report

import com.metrics.model.analysis.ProjectReport

/**
 * Interface for generating reports from analysis results.
 */
interface ReportGenerator {
    /**
     * Generates a report from the given project analysis.
     * The format and destination depend on the implementation.
     * 
     * @param report The project report containing all analysis data
     */
    fun generate(report: ProjectReport)
}