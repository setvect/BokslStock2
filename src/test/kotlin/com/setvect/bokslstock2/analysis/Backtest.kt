package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.service.MabsBacktestService
import com.setvect.bokslstock2.analysis.service.MovingAverageService
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.StockRepository
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import javax.transaction.Transactional

@SpringBootTest
@ActiveProfiles("local")
class Backtest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var movingAverageService: MovingAverageService

    @Autowired
    private lateinit var backtestService: MabsBacktestService

    @Test
    @Transactional
    fun 이동평균계산() {
        val movingAverage =
            movingAverageService.getMovingAverage("069500", PeriodType.PERIOD_DAY, listOf(5, 20, 60, 120))
//        movingAverageService.getMovingAverage("069500", PeriodType.PERIOD_WEEK, listOf(5, 20, 60, 120))
//        movingAverageService.getMovingAverage("069500", PeriodType.PERIOD_MONTH, listOf(5, 20, 60, 120))

        movingAverage.forEach {
            val avgInfo = it.average.entries
                .map { entry -> "이동평균(${entry.key}): ${it.average[entry.key]}" }
                .toList()
                .joinToString(", ")
            println("${it.candleDateTime} - O: ${it.openPrice}, H: ${it.highPrice}, L: ${it.lowPrice}, C:${it.closePrice}, ${it.periodType}, $avgInfo")
        }

//        movingAverage.backtest()
    }

    @Test
    @Transactional
    fun 종목과_시세조회() {
        val stockOp = stockRepository.findByCode("069500")
        val stock = stockOp.get()

        log.info("${stock.name}(${stock.code}) ${stock.candleList.size}")
        stock.candleList.forEach {
            log.info("${it.candleDateTime}: close:${String.format("%,d", it.closePrice)}")
        }
        println("끝.")
    }
}