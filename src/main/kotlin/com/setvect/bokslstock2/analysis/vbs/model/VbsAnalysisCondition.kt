package com.setvect.bokslstock2.analysis.vbs.model

import com.setvect.bokslstock2.analysis.common.model.AnalysisCondition
import com.setvect.bokslstock2.analysis.common.model.TradeCondition

/**
 * 변동성돌파 백테스트
 */
data class VbsAnalysisCondition(
    /**
     * 분석 조건
     */
    override val tradeConditionList: List<VbsCondition>,

    /**
     * 매매 기본 조건
     */
    val basic: TradeCondition,
) : AnalysisCondition() {
    val conditionList: List<VbsCondition>
        get() = tradeConditionList
}