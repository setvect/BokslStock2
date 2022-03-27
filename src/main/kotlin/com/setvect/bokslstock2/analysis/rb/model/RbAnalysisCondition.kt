package com.setvect.bokslstock2.analysis.rb.model

import com.setvect.bokslstock2.analysis.common.model.AnalysisCondition
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.rb.entity.RbConditionEntity

/**
 * 이동평균돌파 백테스트
 */
data class RbAnalysisCondition(
    /**
     * 분석 조건
     */
    override val tradeConditionList: List<RbConditionEntity>,

    /**
     * 매매 기본 조건
     */
    val basic: TradeCondition,
) : AnalysisCondition() {
    val conditionList: List<RbConditionEntity>
        get() = tradeConditionList
}