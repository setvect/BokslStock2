package com.setvect.bokslstock2.analysis.price

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.DateRange
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class BreakoutTradingTest {
    @Autowired
    private lateinit var candleRepository: CandleRepository

    @Autowired
    private lateinit var movingAverageService: MovingAverageService

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    @DisplayName(
        "매수: 당일 주가가 최근 20일 최고가보다 높은 경우\n" +
                "매도: 당일 주가가 최근 20일 최고가보다 낮은 경우\n"
    )
    fun test1() {
        val targetRange = DateRange("2020-01-01T00:00:00", "2023-01-01T00:00:00")

        val stockCode = StockCode.KODEX_200_069500
        val range = candleRepository.findByCandleDateTimeBetween(
            listOf(stockCode.code),
            PeriodType.PERIOD_DAY,
            targetRange.from,
            targetRange.to
        )
        log.info("대상기간 변경: $targetRange -> $range")

        val candleDtoList = movingAverageService.getMovingAverage(
            stockCode,
            PeriodType.PERIOD_DAY,
            PeriodType.PERIOD_DAY,
            listOf(1),
            range
        )

        val period = 20
        val periodCandleList = CircularFifoQueue<CandleDto>(period)
        candleDtoList.stream().limit(period.toLong()).forEach {
            periodCandleList.add(it)
        }

        var i = period
        while (i < candleDtoList.size) {
            val current = candleDtoList[i]
            val candleListSortByClosePrice = periodCandleList.stream().sorted { o1, o2 -> o1.closePrice.compareTo(o2.closePrice) }.toList()

            var max = candleListSortByClosePrice.last()
            var min = candleListSortByClosePrice.first()

            println("기준날짜: ${current.candleDateTimeStart}, 최근 $period 거래일(오늘제외) 종가 최고가: ${max.closePrice}(${max.candleDateTimeStart}), " +
                    "종가 최적가: ${min.closePrice}(${min.candleDateTimeStart})")
            periodCandleList.add(current)
            i++
        }
        println("끝.")
    }
}


