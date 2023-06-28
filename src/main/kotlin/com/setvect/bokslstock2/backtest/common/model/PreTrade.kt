package com.setvect.bokslstock2.backtest.common.model

import com.setvect.bokslstock2.common.model.TradeType
import java.time.LocalDateTime

/**
 * 매매 내역
 */
@Deprecated("TradeNeo로 대체, 사용하지 않도록 함")
data class PreTrade(
    /**
     * ConditionEntity.name
     */
    val conditionName: String = "",
    /**
     * 매매 종목
     */
    val stockCode: StockCode,

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
){
    fun getTradeName(): String {
        return conditionName.ifEmpty { stockCode.name }
    }
}