package com.setvect.bokslstock2.analysis.common.util

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.util.DateRange
import java.time.LocalDate

/**
 * 주식 종목별 일봉 정보
 */
class StockByDateCandle(
    candleRepository: CandleRepository,
    stockCodes: Set<StockCode>,
    dateRange: DateRange
) {
    private val stockCandleMap = stockCodes
        .associateWith { stockCode ->
            val list = candleRepository.findByRange(stockCode.code, PeriodType.PERIOD_DAY, dateRange.from, dateRange.to)
            list.associateBy { it.candleDateTime.toLocalDate() }
        }

    /**
     * @return 기간, 종목에 대한 일봉, 만약 해당 날짜에 시세가 없으면 가장 가까운 시세를 구함.
     */
    fun getNearCandle(
        stockCode: StockCode,
        localDate: LocalDate
    ): CandleEntity {
        // 해당 종목, 날짜의 종가를 구함. 종목 정보가 없으면 예외 발생.
        val candleMap = stockCandleMap[stockCode] ?: throw IllegalArgumentException("종목 정보가 없음. 종목코드: ${stockCode.code}")
        // 날짜가 없으면 최대 10일 전까지 찾음. 10일 이후에도 없으면 예외 발생
        for (i in 0..10) {
            return candleMap[localDate.minusDays(i.toLong())] ?: continue
        }
        throw IllegalArgumentException("종목 정보가 없음. 종목코드: ${stockCode.code}, 날짜: $localDate")
    }

    /**
     * @return 기잔, 종목에 대한 일봉. 해당 기간에 시세가 없으면 null 반환
     */
    fun getCandle(
        stockCode: StockCode,
        localDate: LocalDate
    ): CandleEntity? {
        // 해당 종목, 날짜의 종가를 구함. 종목 정보가 없으면 예외 발생.
        val candleMap = stockCandleMap[stockCode] ?: throw IllegalArgumentException("종목 정보가 없음. 종목코드: ${stockCode.code}")
        return candleMap[localDate]
    }
}