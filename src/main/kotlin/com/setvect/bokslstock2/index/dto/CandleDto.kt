package com.setvect.bokslstock2.index.dto

import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.model.PeriodType
import java.time.LocalDateTime

/**
 * @see CandleEntity
 */
data class CandleDto(
    val candleDateTime: LocalDateTime,
    val periodType: PeriodType,
    val openPrice: Int,
    val highPrice: Int,
    val lowPrice: Int,
    val closePrice: Int
) {
    /**
     * Key: 이동평균 단위, Value: 가격
     */
    var average = HashMap<Int, Int>()
}
