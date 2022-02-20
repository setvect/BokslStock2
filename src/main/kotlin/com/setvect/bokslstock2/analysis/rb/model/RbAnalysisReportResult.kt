package com.setvect.bokslstock2.analysis.rb.model

import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.common.entity.AnalysisCondition
import com.setvect.bokslstock2.common.entity.AnalysisReportResult

/**
 * 멀티 종목 매매 백테스트 분석 결과
 */
data class RbAnalysisReportResult(
    /**
     * 리포트 조건
     */
    val rbAnalysisCondition: RbAnalysisCondition,

    /**
     * 매매 이력
     */
    override val tradeHistory: List<RbTradeReportItem>,

    /**
     * 매매 결과
     */
    override val common: CommonAnalysisReportResult
): AnalysisReportResult {
    override val analysisCondition: AnalysisCondition
        get() = rbAnalysisCondition
}