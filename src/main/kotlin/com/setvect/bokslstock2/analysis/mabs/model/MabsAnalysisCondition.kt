package com.setvect.bokslstock2.analysis.mabs.model

import com.setvect.bokslstock2.analysis.common.model.AnalysisCondition
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.mabs.entity.MabsConditionEntity

/**
 * 이동평균돌파 백테스트
 */
data class MabsAnalysisCondition(
    /**
     * 분석 조건
     */
    override val tradeConditionList: List<MabsConditionEntity>,

    /**
     * 매매 기본 조건
     */
    val basic: TradeCondition,
) : AnalysisCondition() {
    val conditionList: List<MabsConditionEntity>
        get() = tradeConditionList
}