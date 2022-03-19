package com.setvect.bokslstock2.analysis.vbs.model

import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.vbs.entity.VbsConditionEntity
import com.setvect.bokslstock2.common.entity.AnalysisCondition
import com.setvect.bokslstock2.common.entity.ConditionEntity

/**
 * 변동성돌파 백테스트
 */
data class VbsAnalysisCondition(
    /**
     * 분석 조건
     */
    val tradeConditionList: List<VbsConditionEntity>,

    /**
     * 매매 기본 조건
     */
    override val basic: TradeCondition,
) : AnalysisCondition {
    override val conditionList: List<ConditionEntity>
        get() = tradeConditionList
}