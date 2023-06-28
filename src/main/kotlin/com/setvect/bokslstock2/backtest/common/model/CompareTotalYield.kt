package com.setvect.bokslstock2.backtest.common.model

data class CompareTotalYield(
    val buyHoldTotalYield: CommonAnalysisReportResult.TotalYield,
    val benchmarkTotalYield: CommonAnalysisReportResult.TotalYield
) {
}