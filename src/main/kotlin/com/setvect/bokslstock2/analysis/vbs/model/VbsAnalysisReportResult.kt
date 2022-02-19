package com.setvect.bokslstock2.analysis.vbs.model

import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult

/**
 * 멀티 종목 매매 백테스트 분석 결과
 */
data class VbsAnalysisReportResult(
    /**
     * 리포트 조건
     */
    val vbsAnalysisCondition: VbsAnalysisCondition,

    /**
     * 매매 이력
     */
    val tradeHistory: List<VbsTradeReportItem>,

    /**
     * 매매 결과
     */
    val common: CommonAnalysisReportResult,
)