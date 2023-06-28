package com.setvect.bokslstock2.analysis.price

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.DateRange
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class LongShortAnalysisTest {
    @Autowired
    private lateinit var candleRepository: CandleRepository

    @Autowired
    private lateinit var movingAverageService: MovingAverageService

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    @DisplayName("11월~4월 매수, 5월~10월 매도")
    fun test1() {
        val targetRange = DateRange("2010-01-01T00:00:00", "2023-04-16T00:00:00")

        val stock_11_4 = StockCode.KODEX_KOSDAQ_229200
        val stock_5_10 = StockCode.KODEX_KOSDAQ_IV_251340
        val range = candleRepository.findByCandleDateTimeBetween(
            listOf(stock_11_4.code, stock_5_10.code),
            PeriodType.PERIOD_DAY,
            targetRange.from,
            targetRange.to
        )
        log.info("대상기간 변경: $targetRange -> $range")

        val stock_candle_11_4 = movingAverageService.getMovingAverage(
            stock_11_4,
            PeriodType.PERIOD_DAY,
            PeriodType.PERIOD_MONTH,
            listOf(1),
            range
        ).associateBy { it.candleDateTimeStart.withDayOfMonth(1).toLocalDate() }

        val stock_candle_5_10 = movingAverageService.getMovingAverage(
            stock_5_10,
            PeriodType.PERIOD_DAY,
            PeriodType.PERIOD_MONTH,
            listOf(1),
            range
        ).associateBy { it.candleDateTimeStart.withDayOfMonth(1).toLocalDate() }


        var startDate = range.fromDate.withDayOfMonth(1)
        while (startDate.isBefore(range.toDate)) {
            println(startDate)
            if(startDate.monthValue in 11..12 || startDate.monthValue in 1..4) {
                val candle = stock_candle_11_4[startDate]
            } else {
                val candle = stock_candle_5_10[startDate]
            }
            startDate = startDate.plusMonths(1)
        }

        
        // TODO 추가 작업해야됨

    }
}


