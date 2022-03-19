package com.setvect.bokslstock2.analysis.rb.model

import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.rb.entity.RbConditionEntity
import com.setvect.bokslstock2.common.entity.AnalysisCondition
import com.setvect.bokslstock2.common.entity.ConditionEntity

/**
 * 이동평균돌파 백테스트
 */
data class RbAnalysisCondition(
    /**
     * 분석 조건
     */
    val tradeConditionList: List<RbConditionEntity>,

    /**
     * 매매 기본 조건
     */
    override val basic: TradeCondition,
): AnalysisCondition {
    override val conditionList: List<ConditionEntity>
        get() = tradeConditionList
}