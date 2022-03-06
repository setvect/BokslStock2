package com.setvect.bokslstock2.analysis.dm.model

import com.setvect.bokslstock2.analysis.common.model.BasicAnalysisCondition
import com.setvect.bokslstock2.common.entity.AnalysisCondition
import com.setvect.bokslstock2.common.entity.ConditionEntity

/**
 * 듀얼모멘텀 백테스트
 */
data class DmAnalysisCondition(
    /**
     * 분석 조건
     */
    val tradeConditionList: List<DmConditionEntity>,

    /**
     * 매매 기본 조건
     */
    override val basic: BasicAnalysisCondition,
) : AnalysisCondition {
    override val conditionList: List<ConditionEntity>
        get() = tradeConditionList
}