package com.setvect.bokslstock2.analysis.mabs.model

import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.common.entity.AnalysisCondition
import com.setvect.bokslstock2.common.entity.AnalysisReportResult

/**
 * 멀티 종목 매매 백테스트 분석 결과
 */
data class MabsAnalysisReportResult(
    /**
     * 리포트 조건
     */
    val mabsAnalysisCondition: MabsAnalysisCondition,

    /**
     * 매매 이력
     */
    override val tradeHistory: List<MabsTradeReportItem>,

    /**
     * 매매 결과
     */
    override val common: CommonAnalysisReportResult
): AnalysisReportResult {
    override val analysisCondition: AnalysisCondition
        get() = mabsAnalysisCondition
}