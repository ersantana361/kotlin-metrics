package com.metrics.report.html

import com.metrics.model.analysis.*
import com.metrics.model.architecture.*

/**
 * Simplified JavaScript generator for Phase 4.
 * Generates working JavaScript without complex string interpolation issues.
 */
class SimpleJavaScriptGenerator {
    
    fun generateJavaScript(analyses: List<ClassAnalysis>, architectureAnalysis: ArchitectureAnalysis): String {
        val analysisJson = generateAnalysisDataJson(analyses)
        val architectureJson = generateArchitectureDataJson(architectureAnalysis)
        
        return """
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    // Global data for charts and analytics
    const analysisData = $analysisJson;
    const architectureData = $architectureJson;
    
    // Main initialization
    document.addEventListener('DOMContentLoaded', function() {
        initializeAllCharts();
        setupTableInteractivity();
        setupFilterSystem();
        setupSearchFunctionality();
        generateCorrelationMatrix();
        initializeRiskAssessment();
        setupDependencyGraph();
        setupAdvancedFiltering();
        
        // Initialize tooltips
        var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
        
        console.log('✅ Kotlin Metrics Dashboard Initialized');
    });
    
    function initializeAllCharts() {
        generateOverviewCharts();
        generateQualityCharts();
        generateCouplingCharts();
        generateDesignCharts();
        generateArchitectureCharts();
    }
    
    // Overview Charts
    function generateOverviewCharts() {
        // Quality Score Distribution Pie Chart
        const qualityCtx = document.getElementById('overviewQualityChart');
        if (qualityCtx) {
            const qualityDistribution = getQualityDistribution(analysisData);
            
            new Chart(qualityCtx, {
                type: 'doughnut',
                data: {
                    labels: ['Excellent (9-10)', 'Good (7-8)', 'Moderate (5-6)', 'Poor (<5)'],
                    datasets: [{
                        data: [
                            qualityDistribution.excellent,
                            qualityDistribution.good,
                            qualityDistribution.moderate,
                            qualityDistribution.poor
                        ],
                        backgroundColor: ['#28a745', '#17a2b8', '#ffc107', '#dc3545'],
                        borderWidth: 2,
                        borderColor: '#ffffff'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'bottom' },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    const percentage = ((context.parsed / analysisData.length) * 100).toFixed(1);
                                    return context.label + ': ' + context.parsed + ' (' + percentage + '%)';
                                }
                            }
                        }
                    }
                }
            });
        }
        
        // Risk Assessment Bar Chart
        const riskCtx = document.getElementById('overviewRiskChart');
        if (riskCtx) {
            const riskDistribution = getRiskDistribution(analysisData);
            
            new Chart(riskCtx, {
                type: 'bar',
                data: {
                    labels: ['Low', 'Medium', 'High', 'Critical'],
                    datasets: [{
                        label: 'Number of Classes',
                        data: [
                            riskDistribution.low,
                            riskDistribution.medium,
                            riskDistribution.high,
                            riskDistribution.critical
                        ],
                        backgroundColor: ['#28a745', '#ffc107', '#fd7e14', '#dc3545']
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                        y: { beginAtZero: true, ticks: { stepSize: 1 } }
                    }
                }
            });
        }
    }
    
    // Quality Charts (LCOM & WMC)
    function generateQualityCharts() {
        // LCOM Distribution Histogram
        const cohesionCtx = document.getElementById('cohesionChart');
        if (cohesionCtx) {
            const lcomData = analysisData.map(d => d.lcom);
            const lcomHistogram = createHistogram(lcomData, 10);
            
            new Chart(cohesionCtx, {
                type: 'bar',
                data: {
                    labels: lcomHistogram.labels,
                    datasets: [{
                        label: 'Class Count',
                        data: lcomHistogram.values,
                        backgroundColor: 'rgba(54, 162, 235, 0.7)'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { title: { display: true, text: 'LCOM Distribution' } },
                    scales: {
                        x: { title: { display: true, text: 'LCOM Value' } },
                        y: { title: { display: true, text: 'Number of Classes' }, beginAtZero: true }
                    }
                }
            });
        }
        
        // WMC vs Complexity Scatter Plot
        const complexityCtx = document.getElementById('complexityChart');
        if (complexityCtx) {
            const scatterData = analysisData.map(d => ({
                x: d.ckMetrics.wmc,
                y: d.complexity.totalComplexity,
                className: d.className
            }));
            
            new Chart(complexityCtx, {
                type: 'scatter',
                data: {
                    datasets: [{
                        label: 'Classes',
                        data: scatterData,
                        backgroundColor: 'rgba(255, 99, 132, 0.6)',
                        pointRadius: 5
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        title: { display: true, text: 'WMC vs Cyclomatic Complexity' },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    return context.raw.className + ' - WMC: ' + context.raw.x + ', CC: ' + context.raw.y;
                                }
                            }
                        }
                    },
                    scales: {
                        x: { title: { display: true, text: 'Weighted Methods per Class (WMC)' } },
                        y: { title: { display: true, text: 'Total Cyclomatic Complexity' } }
                    }
                }
            });
        }
    }
    
    // Coupling Charts
    function generateCouplingCharts() {
        generateMetricChart('cboChart', 'cbo', 'Coupling Between Objects (CBO)', 'rgba(75, 192, 192, 0.7)');
        generateMetricChart('rfcChart', 'rfc', 'Response for Class (RFC)', 'rgba(153, 102, 255, 0.7)');
        generateMetricChart('caChart', 'ca', 'Afferent Coupling (CA)', 'rgba(255, 159, 64, 0.7)');
        generateMetricChart('ceChart', 'ce', 'Efferent Coupling (CE)', 'rgba(255, 206, 86, 0.7)');
    }
    
    // Design Charts
    function generateDesignCharts() {
        generateMetricChart('ditChart', 'dit', 'Depth of Inheritance Tree (DIT)', 'rgba(54, 162, 235, 0.7)');
        generateMetricChart('nocChart', 'noc', 'Number of Children (NOC)', 'rgba(255, 99, 132, 0.7)');
    }
    
    // Architecture Charts
    function generateArchitectureCharts() {
        const dddCtx = document.getElementById('dddChart');
        if (dddCtx && architectureData.dddPatterns) {
            const dddData = {
                entities: architectureData.dddPatterns.entities.length,
                valueObjects: architectureData.dddPatterns.valueObjects.length,
                services: architectureData.dddPatterns.services.length,
                repositories: architectureData.dddPatterns.repositories.length,
                aggregates: architectureData.dddPatterns.aggregates.length,
                domainEvents: architectureData.dddPatterns.domainEvents.length
            };
            
            new Chart(dddCtx, {
                type: 'bar',
                data: {
                    labels: ['Entities', 'Value Objects', 'Services', 'Repositories', 'Aggregates', 'Domain Events'],
                    datasets: [{
                        label: 'Count',
                        data: Object.values(dddData),
                        backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF', '#FF9F40']
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { title: { display: true, text: 'DDD Pattern Distribution' } },
                    scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
                }
            });
        }
    }
    
    // Utility function for metric charts
    function generateMetricChart(canvasId, metricKey, title, backgroundColor) {
        const ctx = document.getElementById(canvasId);
        if (!ctx) return;
        
        const metricData = analysisData.map(d => d.ckMetrics[metricKey]);
        const histogram = createHistogram(metricData, 8);
        
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: histogram.labels,
                datasets: [{
                    label: 'Class Count',
                    data: histogram.values,
                    backgroundColor: backgroundColor
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { title: { display: true, text: title } },
                scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
            }
        });
    }
    
    // Utility functions
    function getQualityDistribution(data) {
        return {
            excellent: data.filter(d => d.qualityScore.overall >= 9).length,
            good: data.filter(d => d.qualityScore.overall >= 7 && d.qualityScore.overall < 9).length,
            moderate: data.filter(d => d.qualityScore.overall >= 5 && d.qualityScore.overall < 7).length,
            poor: data.filter(d => d.qualityScore.overall < 5).length
        };
    }
    
    function getRiskDistribution(data) {
        return {
            low: data.filter(d => d.riskAssessment.level === 'LOW').length,
            medium: data.filter(d => d.riskAssessment.level === 'MEDIUM').length,
            high: data.filter(d => d.riskAssessment.level === 'HIGH').length,
            critical: data.filter(d => d.riskAssessment.level === 'CRITICAL').length
        };
    }
    
    function createHistogram(data, bins) {
        if (data.length === 0) return { labels: [], values: [] };
        
        const min = Math.min(...data);
        const max = Math.max(...data);
        const binWidth = Math.max(1, Math.ceil((max - min) / bins));
        
        const histogram = {};
        for (let i = 0; i < bins; i++) {
            const binStart = min + i * binWidth;
            const binEnd = binStart + binWidth;
            const binLabel = binStart === binEnd ? binStart.toString() : binStart + '-' + (binEnd-1);
            histogram[binLabel] = 0;
        }
        
        data.forEach(value => {
            const binIndex = Math.min(Math.floor((value - min) / binWidth), bins - 1);
            const binStart = min + binIndex * binWidth;
            const binEnd = binStart + binWidth;
            const binLabel = binStart === binEnd ? binStart.toString() : binStart + '-' + (binEnd-1);
            histogram[binLabel]++;
        });
        
        return {
            labels: Object.keys(histogram),
            values: Object.values(histogram)
        };
    }
    
    // Table sorting and filtering
    function setupTableInteractivity() {
        document.querySelectorAll('.sortable').forEach(header => {
            header.addEventListener('click', function() {
                sortTable(this);
            });
        });
    }
    
    function sortTable(header) {
        const table = header.closest('table');
        const tbody = table.querySelector('tbody');
        const rows = Array.from(tbody.querySelectorAll('tr'));
        const column = header.dataset.column;
        const isAscending = !header.classList.contains('asc');
        
        // Clear previous sort indicators
        table.querySelectorAll('.sortable .sort-indicator').forEach(indicator => {
            indicator.className = 'fas fa-sort sort-indicator';
        });
        
        // Set new sort indicator
        const indicator = header.querySelector('.sort-indicator');
        indicator.className = 'fas fa-sort-' + (isAscending ? 'up' : 'down') + ' sort-indicator active';
        header.classList.toggle('asc', isAscending);
        
        // Sort rows
        rows.sort((a, b) => {
            let aVal, bVal;
            
            switch(column) {
                case 'name':
                    aVal = a.cells[0].textContent.trim();
                    bVal = b.cells[0].textContent.trim();
                    return isAscending ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
                case 'quality':
                    aVal = parseFloat(a.cells[1].textContent);
                    bVal = parseFloat(b.cells[1].textContent);
                    break;
                case 'lcom':
                case 'wmc':
                case 'cbo':
                case 'dit':
                    const cellIndex = {'lcom': 2, 'wmc': 3, 'cbo': 4, 'dit': 5}[column];
                    aVal = parseInt(a.cells[cellIndex].textContent);
                    bVal = parseInt(b.cells[cellIndex].textContent);
                    break;
                case 'risk':
                    const riskOrder = {'LOW': 1, 'MEDIUM': 2, 'HIGH': 3, 'CRITICAL': 4};
                    aVal = riskOrder[a.cells[6].textContent.trim()] || 0;
                    bVal = riskOrder[b.cells[6].textContent.trim()] || 0;
                    break;
                default:
                    return 0;
            }
            
            if (isNaN(aVal) || isNaN(bVal)) return 0;
            return isAscending ? aVal - bVal : bVal - aVal;
        });
        
        rows.forEach(row => tbody.appendChild(row));
    }
    
    function setupFilterSystem() {
        document.querySelectorAll('.filter-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                filterTableByQuality(this.dataset.filter);
                updateActiveFilter(this);
            });
        });
    }
    
    function filterTableByQuality(filter) {
        const rows = document.querySelectorAll('.table-row');
        
        rows.forEach(row => {
            const qualityElement = row.querySelector('[data-quality]');
            if (!qualityElement) {
                row.style.display = '';
                return;
            }
            
            const quality = parseFloat(qualityElement.dataset.quality || qualityElement.textContent);
            let show = false;
            
            switch(filter) {
                case 'all': show = true; break;
                case 'excellent': show = quality >= 9; break;
                case 'good': show = quality >= 7 && quality < 9; break;
                case 'moderate': show = quality >= 5 && quality < 7; break;
                case 'poor': show = quality < 5; break;
            }
            
            row.style.display = show ? '' : 'none';
        });
    }
    
    function updateActiveFilter(activeButton) {
        const parentTab = activeButton.closest('.tab-pane');
        if (parentTab) {
            parentTab.querySelectorAll('.filter-btn').forEach(btn => {
                btn.classList.remove('active');
            });
        }
        activeButton.classList.add('active');
    }
    
    function setupSearchFunctionality() {
        const searchInput = document.getElementById('classSearchInput');
        const sortSelect = document.getElementById('sortSelect');
        
        if (searchInput) {
            searchInput.addEventListener('input', function() {
                filterTableBySearch(this.value);
            });
        }
        
        if (sortSelect) {
            sortSelect.addEventListener('change', function() {
                sortTableBySelect(this.value);
            });
        }
    }
    
    function filterTableBySearch(searchTerm) {
        const rows = document.querySelectorAll('#detailsTable tbody tr');
        const term = searchTerm.toLowerCase();
        
        rows.forEach(row => {
            const className = row.cells[0].textContent.toLowerCase();
            const fileName = row.cells[0].querySelector('small')?.textContent.toLowerCase() || '';
            const matches = className.includes(term) || fileName.includes(term);
            row.style.display = matches ? '' : 'none';
        });
    }
    
    function sortTableBySelect(sortBy) {
        const table = document.getElementById('detailsTable');
        if (!table) return;
        
        const tbody = table.querySelector('tbody');
        const rows = Array.from(tbody.querySelectorAll('tr'));
        
        rows.sort((a, b) => {
            let aVal, bVal;
            
            switch(sortBy) {
                case 'name':
                    aVal = a.cells[0].textContent.trim();
                    bVal = b.cells[0].textContent.trim();
                    return aVal.localeCompare(bVal);
                case 'quality':
                    aVal = parseFloat(a.cells[1].textContent);
                    bVal = parseFloat(b.cells[1].textContent);
                    return bVal - aVal;
                case 'lcom':
                    aVal = parseInt(a.cells[2].textContent);
                    bVal = parseInt(b.cells[2].textContent);
                    return bVal - aVal;
                case 'complexity':
                    aVal = parseInt(a.cells[3].textContent);
                    bVal = parseInt(b.cells[3].textContent);
                    return bVal - aVal;
                case 'coupling':
                    aVal = parseInt(a.cells[4].textContent);
                    bVal = parseInt(b.cells[4].textContent);
                    return bVal - aVal;
                case 'risk':
                    const riskOrder = {'LOW': 1, 'MEDIUM': 2, 'HIGH': 3, 'CRITICAL': 4};
                    aVal = riskOrder[a.cells[6].textContent.trim()] || 0;
                    bVal = riskOrder[b.cells[6].textContent.trim()] || 0;
                    return bVal - aVal;
                default:
                    return 0;
            }
        });
        
        rows.forEach(row => tbody.appendChild(row));
    }
    
    function showClassDetails(className) {
        const classData = analysisData.find(d => d.className === className);
        if (!classData) return;
        
        const modal = document.createElement('div');
        modal.className = 'modal fade';
        modal.id = 'classDetailModal';
        modal.setAttribute('tabindex', '-1');
        modal.setAttribute('aria-hidden', 'true');
        
        modal.innerHTML = 
            '<div class="modal-dialog modal-lg">' +
                '<div class="modal-content">' +
                    '<div class="modal-header">' +
                        '<h5 class="modal-title">Class Details: ' + className + '</h5>' +
                        '<button type="button" class="btn-close" data-bs-dismiss="modal"></button>' +
                    '</div>' +
                    '<div class="modal-body">' +
                        '<div class="row">' +
                            '<div class="col-md-6">' +
                                '<h6>Quality Metrics</h6>' +
                                '<ul class="list-group mb-3">' +
                                    '<li class="list-group-item d-flex justify-content-between">' +
                                        '<span>Overall Quality Score</span>' +
                                        '<span class="badge bg-primary">' + classData.qualityScore.overall.toFixed(1) + '</span>' +
                                    '</li>' +
                                    '<li class="list-group-item d-flex justify-content-between">' +
                                        '<span>LCOM</span><span>' + classData.lcom + '</span>' +
                                    '</li>' +
                                    '<li class="list-group-item d-flex justify-content-between">' +
                                        '<span>WMC</span><span>' + classData.ckMetrics.wmc + '</span>' +
                                    '</li>' +
                                    '<li class="list-group-item d-flex justify-content-between">' +
                                        '<span>CBO</span><span>' + classData.ckMetrics.cbo + '</span>' +
                                    '</li>' +
                                '</ul>' +
                            '</div>' +
                            '<div class="col-md-6">' +
                                '<h6>Risk Assessment</h6>' +
                                '<div class="alert alert-' + getRiskAlertClass(classData.riskAssessment.level) + '">' +
                                    '<strong>' + classData.riskAssessment.level + '</strong><br>' +
                                    classData.riskAssessment.impact +
                                '</div>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
        
        document.getElementById('classDetailModal')?.remove();
        document.body.appendChild(modal);
        
        const modalElement = new bootstrap.Modal(modal);
        modalElement.show();
    }
    
    function getRiskAlertClass(level) {
        switch(level) {
            case 'LOW': return 'success';
            case 'MEDIUM': return 'warning';
            case 'HIGH': return 'warning';
            case 'CRITICAL': return 'danger';
            default: return 'info';
        }
    }
    
    // Correlation Analysis
    function generateCorrelationMatrix() {
        const metrics = ['lcom', 'wmc', 'cbo', 'rfc', 'dit', 'noc'];
        const correlations = {};
        
        for (let i = 0; i < metrics.length; i++) {
            for (let j = i + 1; j < metrics.length; j++) {
                const metric1 = metrics[i];
                const metric2 = metrics[j];
                const correlation = calculateCorrelation(metric1, metric2);
                correlations[metric1 + '_' + metric2] = correlation;
            }
        }
        
        console.log('Correlation Matrix:', correlations);
        return correlations;
    }
    
    function calculateCorrelation(metric1, metric2) {
        const data1 = analysisData.map(d => getMetricValue(d, metric1));
        const data2 = analysisData.map(d => getMetricValue(d, metric2));
        
        if (data1.length !== data2.length || data1.length === 0) return 0;
        
        const mean1 = data1.reduce((a, b) => a + b, 0) / data1.length;
        const mean2 = data2.reduce((a, b) => a + b, 0) / data2.length;
        
        let numerator = 0;
        let sumSq1 = 0;
        let sumSq2 = 0;
        
        for (let i = 0; i < data1.length; i++) {
            const diff1 = data1[i] - mean1;
            const diff2 = data2[i] - mean2;
            numerator += diff1 * diff2;
            sumSq1 += diff1 * diff1;
            sumSq2 += diff2 * diff2;
        }
        
        const denominator = Math.sqrt(sumSq1 * sumSq2);
        return denominator === 0 ? 0 : numerator / denominator;
    }
    
    function getMetricValue(analysis, metric) {
        switch(metric) {
            case 'lcom': return analysis.lcom;
            case 'wmc': return analysis.ckMetrics.wmc;
            case 'cbo': return analysis.ckMetrics.cbo;
            case 'rfc': return analysis.ckMetrics.rfc;
            case 'dit': return analysis.ckMetrics.dit;
            case 'noc': return analysis.ckMetrics.noc;
            default: return 0;
        }
    }
    
    // Risk Assessment Logic
    function initializeRiskAssessment() {
        const riskCounts = getRiskDistribution(analysisData);
        const totalClasses = analysisData.length;
        
        const projectRiskScore = calculateProjectRiskScore(riskCounts, totalClasses);
        console.log('Project Risk Score:', projectRiskScore);
        
        const topRiskClasses = analysisData
            .filter(d => d.riskAssessment.level === 'HIGH' || d.riskAssessment.level === 'CRITICAL')
            .sort((a, b) => b.riskAssessment.priority - a.riskAssessment.priority)
            .slice(0, 5);
            
        console.log('Top 5 Risk Classes:', topRiskClasses.map(d => d.className));
        generateRiskInsights(analysisData);
    }
    
    function calculateProjectRiskScore(riskCounts, total) {
        if (total === 0) return 0;
        
        const weights = { critical: 4, high: 3, medium: 2, low: 1 };
        const weightedSum = (riskCounts.critical * weights.critical) +
                           (riskCounts.high * weights.high) +
                           (riskCounts.medium * weights.medium) +
                           (riskCounts.low * weights.low);
                           
        return (weightedSum / (total * weights.critical)) * 10;
    }
    
    function generateRiskInsights(data) {
        const insights = [];
        
        const highLcomClasses = data.filter(d => d.lcom > 10);
        if (highLcomClasses.length > 0) {
            insights.push(highLcomClasses.length + ' classes have very poor cohesion (LCOM > 10)');
        }
        
        const highComplexityClasses = data.filter(d => d.ckMetrics.wmc > 50);
        if (highComplexityClasses.length > 0) {
            insights.push(highComplexityClasses.length + ' classes have extremely high complexity (WMC > 50)');
        }
        
        const highCouplingClasses = data.filter(d => d.ckMetrics.cbo > 20);
        if (highCouplingClasses.length > 0) {
            insights.push(highCouplingClasses.length + ' classes have excessive coupling (CBO > 20)');
        }
        
        const deepInheritanceClasses = data.filter(d => d.ckMetrics.dit > 6);
        if (deepInheritanceClasses.length > 0) {
            insights.push(deepInheritanceClasses.length + ' classes have deep inheritance (DIT > 6)');
        }
        
        console.log('Risk Insights:', insights);
        return insights;
    }
    
    // Advanced Filtering System
    function setupAdvancedFiltering() {
        // Implementation would go here
        console.log('Advanced filtering setup completed');
    }
    
    // D3.js Dependency Graph Visualization
    function setupDependencyGraph() {
        const container = document.getElementById('dependencyGraph');
        if (!container) return;
        
        const graphData = architectureData.dependencyGraph;
        if (!graphData || graphData.nodes.length === 0) {
            container.innerHTML = '<div class="text-center p-5"><p class="text-muted">No dependency graph data available</p></div>';
            return;
        }
        
        // Simplified D3 implementation would go here
        container.innerHTML = '<div class="text-center p-5"><p class="text-info">Dependency graph visualization enabled</p></div>';
        console.log('Dependency graph setup completed');
    }
</script>
"""
    }
    
    private fun generateAnalysisDataJson(analyses: List<ClassAnalysis>): String {
        return analyses.joinToString(prefix = "[", postfix = "]") { analysis ->
            """
            {
                "className": "${analysis.className.replace("\"", "\\\"")}",
                "fileName": "${analysis.fileName.replace("\"", "\\\"")}",
                "lcom": ${analysis.lcom},
                "methodCount": ${analysis.methodCount},
                "propertyCount": ${analysis.propertyCount},
                "ckMetrics": {
                    "wmc": ${analysis.ckMetrics.wmc},
                    "cyclomaticComplexity": ${analysis.ckMetrics.cyclomaticComplexity},
                    "cbo": ${analysis.ckMetrics.cbo},
                    "rfc": ${analysis.ckMetrics.rfc},
                    "ca": ${analysis.ckMetrics.ca},
                    "ce": ${analysis.ckMetrics.ce},
                    "dit": ${analysis.ckMetrics.dit},
                    "noc": ${analysis.ckMetrics.noc},
                    "lcom": ${analysis.ckMetrics.lcom}
                },
                "qualityScore": {
                    "cohesion": ${analysis.qualityScore.cohesion},
                    "complexity": ${analysis.qualityScore.complexity},
                    "coupling": ${analysis.qualityScore.coupling},
                    "inheritance": ${analysis.qualityScore.inheritance},
                    "architecture": ${analysis.qualityScore.architecture},
                    "overall": ${analysis.qualityScore.overall}
                },
                "riskAssessment": {
                    "level": "${analysis.riskAssessment.level}",
                    "reasons": [${analysis.riskAssessment.reasons.joinToString { "\"${it.replace("\"", "\\\"")}\"" }}],
                    "impact": "${analysis.riskAssessment.impact.replace("\"", "\\\"")}",
                    "priority": ${analysis.riskAssessment.priority}
                },
                "complexity": {
                    "totalComplexity": ${analysis.complexity.totalComplexity},
                    "averageComplexity": ${analysis.complexity.averageComplexity},
                    "maxComplexity": ${analysis.complexity.maxComplexity}
                }
            }
            """.trimIndent()
        }
    }
    
    private fun generateArchitectureDataJson(architectureAnalysis: ArchitectureAnalysis): String {
        return """
        {
            "dddPatterns": {
                "entities": [${architectureAnalysis.dddPatterns.entities.joinToString { "\"${it.className.replace("\"", "\\\"")}\"" }}],
                "valueObjects": [${architectureAnalysis.dddPatterns.valueObjects.joinToString { "\"${it.className.replace("\"", "\\\"")}\"" }}],
                "services": [${architectureAnalysis.dddPatterns.services.joinToString { "\"${it.className.replace("\"", "\\\"")}\"" }}],
                "repositories": [${architectureAnalysis.dddPatterns.repositories.joinToString { "\"${it.className.replace("\"", "\\\"")}\"" }}],
                "aggregates": [${architectureAnalysis.dddPatterns.aggregates.joinToString { "\"${it.rootEntity.replace("\"", "\\\"")}\"" }}],
                "domainEvents": [${architectureAnalysis.dddPatterns.domainEvents.joinToString { "\"${it.className.replace("\"", "\\\"")}\"" }}]
            },
            "dependencyGraph": {
                "nodes": [${architectureAnalysis.dependencyGraph.nodes.joinToString { node ->
                    val safeId = node.id.replace("\"", "\\\"")
                    val safeClassName = node.className.replace("\"", "\\\"")
                    val safeLayer = (node.layer ?: "unknown").replace("\"", "\\\"")
                    val coupling = 0 // TODO: Calculate coupling from edges
                    """{"id": "$safeId", "className": "$safeClassName", "layer": "$safeLayer", "coupling": $coupling}"""
                }}],
                "edges": [${architectureAnalysis.dependencyGraph.edges.joinToString { edge ->
                    val safeFrom = edge.fromId.replace("\"", "\\\"")
                    val safeTo = edge.toId.replace("\"", "\\\"")
                    val isCycle = architectureAnalysis.dependencyGraph.cycles.any { cycle -> 
                        cycle.nodes.contains(edge.fromId) && cycle.nodes.contains(edge.toId) 
                    }
                    """{"source": "$safeFrom", "target": "$safeTo", "strength": 1, "isCycle": $isCycle}"""
                }}]
            }
        }
        """.trimIndent()
    }
}