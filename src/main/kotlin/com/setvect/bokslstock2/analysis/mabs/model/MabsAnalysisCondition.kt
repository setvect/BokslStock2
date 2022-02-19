package com.setvect.bokslstock2.analysis.mabs.model

import com.setvect.bokslstock2.analysis.common.model.BasicAnalysisCondition
import com.setvect.bokslstock2.analysis.mabs.entity.MabsConditionEntity
import com.setvect.bokslstock2.common.entity.AnalysisCondition
import com.setvect.bokslstock2.common.entity.ConditionEntity

/**
 * 이동평균돌파 백테스트
 */
data class MabsAnalysisCondition(
    /**
     * 분석 조건
     */
    val tradeConditionList: List<MabsConditionEntity>,

    /**
     * 매매 기본 조건
     */
    override val basic: BasicAnalysisCondition,
): AnalysisCondition {
    override val conditionList: List<ConditionEntity>
        get() = tradeConditionList
}