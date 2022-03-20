package com.setvect.bokslstock2.analysis.dm.model

import com.setvect.bokslstock2.analysis.common.model.TradeCondition

/**
 * 듀얼모멘텀 백테스트
 */
@Deprecated("안씀")
data class DmAnalysisCondition(
    /**
     * 분석 조건
     */
    val tradeConditionList: List<DmConditionEntity>,

    /**
     * 매매 기본 조건
     */
    val basic: TradeCondition,
)