package com.setvect.bokslstock2.koreainvestment.ws

import com.setvect.bokslstock2.util.ApplicationUtil.getYield
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 *
 */
data class TradeResult(
    val type: String,
    val code: String,
    val tradePrice: Double,
    val tradeVolume: Double,
    val timestamp: Long,
    val tradeDate: LocalDate,
    val tradeTime: LocalTime,
    val prevClosingPrice: Double,
) {

    val tradeDateTimeUtc: LocalDateTime
        get() = LocalDateTime.of(tradeDate, tradeTime)
    val tradeDateTimeKst: LocalDateTime
        get() = tradeDateTimeUtc.plusHours(9)

    /**
     * @return 거래량 * 금액
     */
    val totalPrice: Double
        get() = tradePrice * tradeVolume

    /**
     * @return 일일 수익률
     */
    val yieldDay: Double
        get() = getYield(prevClosingPrice, tradePrice)

    /**
     * @return 현 서버 타임스템프 - 시세 타임스템프
     */
    val timestampDiff: Long
        get() = System.currentTimeMillis() - timestamp
}