package com.setvect.bokslstock2.index.dto

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.util.ApplicationUtil
import java.time.LocalDateTime

/**
 * @see CandleEntity
 */
data class CandleDto(
    val stockCode: StockCode,
    val candleDateTimeStart: LocalDateTime,
    val candleDateTimeEnd: LocalDateTime,
    val periodType: PeriodType,
    // 직전거래일 날짜
    val beforeCandleDateTimeEnd: LocalDateTime,
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
     * @return 수익률(현재 종가 / 직전 종가)
     */
    fun getYield(): Double {
        return ApplicationUtil.getYield(beforeClosePrice, closePrice)
    }

    /**
     *
     * @return 수익률(현재 시가 / 직전 종가)
     */
    fun getOpenYield(): Double {
        return ApplicationUtil.getYield(beforeClosePrice, openPrice)
    }
}
