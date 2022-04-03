package com.setvect.bokslstock2.index.dto

import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.model.PeriodType
import java.time.LocalDateTime

/**
 * @see CandleEntity
 */
data class CandleDto(
    val candleDateTimeStart: LocalDateTime,
    val candleDateTimeEnd: LocalDateTime,
    val periodType: PeriodType,
    // 직전종가
    val beforeClosePrice: Double,
    val openPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val closePrice: Double
) {
    /**
     * Key: 이동평균 단위, Value: 가격
     */
    var average = HashMap<Int, Double>()

    /**
     *
     * @return 시가대비 종가 수익률, 현재 종가 / 직전 종가
     */
    fun getYield(): Double {
        return closePrice / beforeClosePrice
    }
}
