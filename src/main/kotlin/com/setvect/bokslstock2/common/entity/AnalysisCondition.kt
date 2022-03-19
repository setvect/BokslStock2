package com.setvect.bokslstock2.common.entity

import com.setvect.bokslstock2.analysis.common.model.TradeCondition

interface AnalysisCondition {
    /**
     * 분석 조건
     */
    val conditionList: List<ConditionEntity>

    /**
     * 매매 기본 조건
     */
    val basic: TradeCondition
}