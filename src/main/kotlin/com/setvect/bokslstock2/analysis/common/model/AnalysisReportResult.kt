package com.setvect.bokslstock2.analysis.common.model

data class AnalysisResult(
    /**
     * 리포트 조건
     */
    val analysisCondition: TradeCondition,

    /**
     * 매매 이력
     */
    val tradeHistory: List<Trade>,

    /**
     * 매매 결과
     */
    val common: CommonAnalysisReportResult2
)