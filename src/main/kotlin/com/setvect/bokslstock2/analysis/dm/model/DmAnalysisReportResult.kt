package com.setvect.bokslstock2.analysis.dm.model

import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.common.entity.AnalysisCondition
import com.setvect.bokslstock2.common.entity.AnalysisReportResult

class DmAnalysisReportResult(
    /**
     * 리포트 조건
     */
    val dmAnalysisCondition: DmAnalysisCondition,

    /**
     * 매매 이력
     */
    override val tradeHistory: List<DmTradeReportItem>,

    /**
     * 매매 결과
     */
    override val common: CommonAnalysisReportResult,
) : AnalysisReportResult {
    override val analysisCondition: AnalysisCondition
        get() = dmAnalysisCondition
}