package com.setvect.bokslstock2.analysis.dm.model

import com.setvect.bokslstock2.analysis.common.model.TradeType
import java.time.LocalDateTime

/**
 * 듀얼모멘텀 매매 내역
 */
data class DmTrade(
    /**
     * 매매 기본 조건
     */
    val stockCode: String,

    /**
     * 매수/매도
     */
    val tradeType: TradeType,

    /**
     * 매도시 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     * 매수는 0으로 표현
     */
    val yield: Double,

    /**
     * 거래 단가
     * - 매수일 경우 매수 단가
     * - 매도일 경우 매도 단가
     */
    val unitPrice: Double,

    /**
     * 거래시간
     */
    val tradeDate: LocalDateTime,
    ) {
}