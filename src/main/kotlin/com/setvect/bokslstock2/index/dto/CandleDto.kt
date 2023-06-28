package com.setvect.bokslstock2.index.dto

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.util.ApplicationUtil
import java.time.LocalDateTime

/**
 * @see CandleEntity
 */
data class CandleDto(
    val stockCode: StockCode,
    /** 캔들 시작 시간을 의미함 */
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
    /**
     *
     * @return 수익률(오늘 종가 / 오늘 시가)
     */
    fun getTodayYield(): Double {
        return ApplicationUtil.getYield(openPrice, closePrice)
    }
}
