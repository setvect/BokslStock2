package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.StockCode
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_WEEK
import com.setvect.bokslstock2.index.service.MovingAverageService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("local")
class MovingAverageTest {
    @Autowired
    private lateinit var movingAverageService: MovingAverageService

    @Test
    @Transactional
    fun 이동평균계산() {
        val movingAverage =
            movingAverageService.getMovingAverage(StockCode.CODE_KODEX_KOSDAQ_2X_233740, PERIOD_WEEK, listOf(1))

        movingAverage.forEach {
            val avgInfo = it.average.entries
                .map { entry -> "이동평균(${entry.key}): ${it.average[entry.key]}" }
                .toList()
                .joinToString(", ")
            println("${it.candleDateTimeStart}~${it.candleDateTimeEnd} - O: ${it.openPrice}, H: ${it.highPrice}, L: ${it.lowPrice}, C:${it.closePrice}, ${it.periodType}, $avgInfo")
        }
    }
}