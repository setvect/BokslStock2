package com.setvect.bokslstock2.analysis.common.model

data class CompareYieldCode(
    /**
     * 투자 종목에 대한 Buy & Hold <종목 아이디, 수익 정보>
     */
    val buyHoldYieldByCode: Map<String, CommonAnalysisReportResult.YieldMdd>,
    /**
     * 밴치마크 <종목 아이디, 수익 정보>
     */
    val benchmarkYieldByCode: Map<String, CommonAnalysisReportResult.YieldMdd>
)
