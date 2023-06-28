package com.setvect.bokslstock2.backtest.common.model

data class CompareYieldCode(
    /**
     * 투자 종목에 대한 Buy & Hold <종목 아이디, 수익 정보>
     */
    val buyHoldYieldByCode: Map<StockCode, CommonAnalysisReportResult.YieldMdd>,
    /**
     * 밴치마크 <종목 아이디, 수익 정보>
     */
    val benchmarkYieldByCode: Map<StockCode, CommonAnalysisReportResult.YieldMdd>
)
