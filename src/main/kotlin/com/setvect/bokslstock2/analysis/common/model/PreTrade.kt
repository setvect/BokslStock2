package com.setvect.bokslstock2.analysis.common.model

import com.setvect.bokslstock2.analysis.dm.model.Stock
import java.time.LocalDateTime

/**
 * 매매 내역
 */
data class PreTrade(
    /**
     * 매매 종목
     */
    val stock: Stock,

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
)