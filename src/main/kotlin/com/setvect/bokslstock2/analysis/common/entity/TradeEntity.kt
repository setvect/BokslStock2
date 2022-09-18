package com.setvect.bokslstock2.analysis.common.entity

import com.setvect.bokslstock2.common.model.TradeType
import java.time.LocalDateTime

interface TradeEntity {
    fun getConditionEntity(): ConditionEntity

    /**
     * @return 매수/매도
     */
    val tradeType: TradeType

    /**
     * 매도시 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     * 매수는 0으로 표현
     */
    val yield: Double

    /**
     * 거래 단가
     * - 매수일 경우 매수 단가
     * - 매도일 경우 매도 단가
     */
    val unitPrice: Double

    /**
     * 거래시간
     */
    val tradeDate: LocalDateTime
}