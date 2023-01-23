package com.setvect.bokslstock2.analysis.price

import com.setvect.bokslstock2.analysis.common.model.StockCode
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
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("local")
class CancelAnalysisTest {
    @Autowired
    private lateinit var candleRepository: CandleRepository

    @Autowired
    private lateinit var movingAverageService: MovingAverageService

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    @DisplayName("갭 상승일 때 매도 타이밍 테스트 ")
    fun test1() {
        val targetRange = DateRange("2022-01-01T00:00:00", "2023-01-01T00:00:00")

        val stockCode = StockCode.KODEX_KOSDAQ_2X_233740
        val range = candleRepository.findByCandleDateTimeBetween(
            listOf(stockCode.code),
            PeriodType.PERIOD_MINUTE_5,
            targetRange.from,
            targetRange.to
        )
        log.info("대상기간 변경: $targetRange -> $range")

        val candleDtoList = movingAverageService.getMovingAverage(
            stockCode,
            PeriodType.PERIOD_MINUTE_5,
            PeriodType.PERIOD_DAY,
            listOf(1),
            range
        )

        log.info("크기: ${candleDtoList.size}")

        var gapRiseOpenCount = 0
        var stayGapRiseCount = 0
        // TODO 어떤 목적으로 만들었는지 모르겠다. ㅡㅡ;
        candleDtoList.forEach {
            if (it.getOpenYield() > 0) {
                gapRiseOpenCount++
                if (it.getYield() > 0) {
                    stayGapRiseCount++
                }
            }
        }
        log.info("갭 상승: $gapRiseOpenCount, 갭 상승 유지: $stayGapRiseCount")
    }

    @Test
    @DisplayName("갭 상승일 때 매도 타이밍 테스트")
    fun test2() {
        val targetRange = DateRange("2015-01-01T00:00:00", "2023-01-01T00:00:00")

        val stockCode = StockCode.KODEX_BANK_091170
        val range = candleRepository.findByCandleDateTimeBetween(
            listOf(stockCode.code),
            PeriodType.PERIOD_MINUTE_5,
            targetRange.from,
            targetRange.to
        )
        log.info("대상기간 변경: $targetRange -> $range")

        val candleDtoList = movingAverageService.getMovingAverage(
            stockCode,
            PeriodType.PERIOD_MINUTE_5,
            PeriodType.PERIOD_MINUTE_5,
            listOf(1),
            range
        )

        log.info("사이즈: ${candleDtoList.size}")

        var current: LocalDate = candleDtoList[0].candleDateTimeStart.toLocalDate()
        var dayCount = 0
        var gapRiseOpenCount = 0
        var gapRiseKeepCount = 0

        candleDtoList.forEach {
            if (current == it.candleDateTimeStart.toLocalDate()) {
                return@forEach
            }

            current = it.candleDateTimeStart.toLocalDate()
            dayCount++

            if (it.getOpenYield() > 0) {
                gapRiseOpenCount++
                if (it.closePrice - it.openPrice > 0) {
                    gapRiseKeepCount++
                }
            }
        }
        log.info("[$stockCode] 총 날짜: $dayCount, 갭 상승: $gapRiseOpenCount, 수익 건수: $gapRiseKeepCount")
    }
}


// 갭 상승 시 - [KODEX_KOSDAQ_2X_233740] 총 날짜: 1244, 갭 상승: 661, 수익 건수: 328 = 0.496
// 갭 하락 시 - [KODEX_KOSDAQ_2X_233740] 총 날짜: 1244, 갭 하락: 583, 수익 건수: 285 = 0.488

// 갭 상승 시 - [KODEX_BANK_091170] 총 날짜: 1244, 갭 상승: 607, 수익 건수: 272 = 0.448
// 갭 하락 시 - [KODEX_BANK_091170] 총 날짜: 1244, 갭 하락: 637, 수익 건수: 226 = 0.354

