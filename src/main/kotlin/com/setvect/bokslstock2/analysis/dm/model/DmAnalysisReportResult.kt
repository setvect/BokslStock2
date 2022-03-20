package com.setvect.bokslstock2.analysis.dm.model

import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult

class DmAnalysisReportResult(
    /**
     * 리포트 조건
     */
    val dmAnalysisCondition: DmAnalysisCondition,

    /**
     * 매매 이력
     */
    val tradeHistory: List<DmTradeReportItem>,

    /**
     * 매매 결과
     */
    val common: CommonAnalysisReportResult,
)