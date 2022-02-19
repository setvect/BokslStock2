package com.setvect.bokslstock2.common.entity

import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult

/**
 * 멀티 종목 매매 백테스트 분석 결과
 */
interface AnalysisReportResult {
    /**
     * 리포트 조건
     */
    val analysisCondition: AnalysisCondition

    /**
     * 매매 이력
     */
    val tradeHistory: List<TradeReportItem>

    /**
     * 매매 결과
     */
    val common: CommonAnalysisReportResult
}