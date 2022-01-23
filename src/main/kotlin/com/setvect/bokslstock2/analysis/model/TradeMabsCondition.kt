package com.setvect.bokslstock2.analysis.model

import com.setvect.bokslstock2.index.model.PeriodType

/**
 * 이동평균돌파 백테스트
 */
data class TradeMabsCondition(
    /**
     * 분석주기
     */
    val periodType: PeriodType,
    /**
     * 대상 종목코드
     */
    val stockCode: String,
    /**
     * 최대매매 종목수
     */
    val maxBuyCount: Int,
    /**
     * 하락매도율
     */
    val upBuyRate: Double,
    /**
     * 상승매도율
     */
    val downSellRate: Double,
)