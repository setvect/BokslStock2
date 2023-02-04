package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.NumberUtil.percent
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.LocalDate

/**
 * 다양한 추세 추종 방법 테스트
 */
@SpringBootTest
@ActiveProfiles("local")
class TrendFollowingBacktest {
    @Autowired
    private lateinit var movingAverageService: MovingAverageService

    // KODEX_200에서는 의미 있음
    @Test
    @DisplayName("오늘 종가 매수, 다음날 시가 매도")
    fun test1() {
        // 매매 수수료
        // 수수료 적용은 시키지 않았다. 그냥 유튜브 말이 맞는지 검증하기 위해서다
        val fee = 0.0

        val dateRange = DateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2022, 1, 31))
        val candleList = movingAverageService.getMovingAverage(
            StockCode.KODEX_200_069500,
            PeriodType.PERIOD_DAY,
            PeriodType.PERIOD_DAY,
            listOf(1),
            dateRange
        )

        val tradeHistory = mutableListOf<TradeStock>()
        var cumulativeYield = 1.0
        val yieldHistory = mutableListOf<Double>()
        val yieldCumulativeHistory = mutableListOf<Double>()
        for (candle in candleList) {
            val yieldRate = ApplicationUtil.getYield(candle.beforeClosePrice, candle.openPrice) - (fee * 2)
            yieldHistory.add(yieldRate)
            cumulativeYield *= (1 + yieldRate)
            yieldCumulativeHistory.add(cumulativeYield)

            println(
                "[${candle.candleDateTimeStart}], " +
                        "O: ${candle.openPrice} H:${candle.highPrice} L:${candle.lowPrice} C:${candle.closePrice}, " +
                        "시초가 수익률:${percent(candle.getOpenYield() * 100)}, " +
                        "당일 수익률: ${percent(candle.getTodayYield() * 100)}, " +
                        "수익률: ${percent(candle.getYield() * 100)}"
            )
            println(
                "[$candle.candleDateTime}] 매수: ${candle.beforeClosePrice}, 매도: ${candle.openPrice}," +
                        " 수익률: ${percent(yieldRate * 100)}"
            )

        }

        println("=== 전략 결과 ===")
        println("누적 수익률: ${percent((cumulativeYield - 1) * 100)}")
        val between = Duration.between(dateRange.from, dateRange.to)
        val cagr = ApplicationUtil.getCagr(1.0, cumulativeYield, between.toDays().toInt())
        println("샤프지수: ${ApplicationUtil.getSharpeRatio(yieldHistory)}")
        println("CAGR: ${percent(cagr * 100)}")
        println("MDD: ${percent(ApplicationUtil.getMdd(yieldCumulativeHistory) * 100)}")
        val odds = yieldHistory.count { it > 0 }.toDouble() / yieldHistory.size.toDouble()
        println("승률: ${percent(odds * 100)}")
        println("매수 + 매도 횟수: ${candleList.size}")
        println("=================\n")
        println()
        println("=== Buy & Hold 결과 ===")
        val startPrice = candleList.first().closePrice
        val endPrice = candleList.last().closePrice
        val holdCagr = ApplicationUtil.getCagr(startPrice, endPrice, between.toDays().toInt())

        val closePriceHistory = candleList.map { it.closePrice }
        println("누적 수익률: ${percent(ApplicationUtil.getYield(startPrice, endPrice) * 100)}")
        println("샤프지수: ${ApplicationUtil.getSharpeRatio(ApplicationUtil.calcPriceYield(closePriceHistory))}")
        println("CAGR: ${percent(holdCagr * 100)}")
        println("MDD: ${percent(ApplicationUtil.getMdd(closePriceHistory) * 100)}")
        println("=================\n")
    }


    @Test
    @DisplayName("오늘 종가가 양봉이면 종가 매수, 다음날 종가 매도")
    fun test2() {
        // 매매 수수료
        val fee = 0.00015

    }

    data class TradeStock(val tradeType: TradeType, val candle: CandleDto)

    enum class TradeType {
        BUY, SELL
    }
}