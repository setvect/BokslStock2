package com.setvect.bokslstock2.analysis.price

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.analysis.common.model.StockCode.*
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.DateUtil
import com.setvect.bokslstock2.util.NumberUtil.percent
import okhttp3.internal.toImmutableList
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.LocalDate

/**
 * 다양한 추세 추종 방법 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class TrendFollowingBacktest {
    @Autowired
    private lateinit var movingAverageService: MovingAverageService

    @Autowired
    private lateinit var candleRepository: CandleRepository
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    // KODEX_200에서는 의미 있음
    @Test
    @DisplayName("오늘 종가 매수, 다음날 시가 매도")
    fun test1() {
        // 매매 수수료
        // 수수료 적용은 시키지 않았다. 그냥 유튜브 말이 맞는지 검증하기 위해서다
        val fee = 0.0

        val dateRange = DateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2022, 1, 31))
        val candleList = movingAverageService.getMovingAverage(
            KODEX_200_069500,
            PeriodType.PERIOD_DAY,
            PeriodType.PERIOD_DAY,
            listOf(1),
            dateRange
        )

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

        report(cumulativeYield, dateRange, yieldHistory, yieldCumulativeHistory, candleList)
    }

    // 결과: 쓰지 마
    @Test
    @DisplayName("오늘 종가가 양봉이면 종가 매수, 다음날 종가 매도")
    fun test2() {
        val dateRange = DateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2022, 12, 31))
        val fee = 0.0
        val stockCode = KODEX_200_069500

        val backTester = object : FollowingBacktest {
            override fun trading(candleList: List<CandleDto>): List<TradeStock> {
                val tradeHistory = mutableListOf<TradeStock>()
                for (candle in candleList) {
                    printCandleInfo(candle)
                    val sell = tradeHistory.isNotEmpty() && tradeHistory.last().tradeType == TradeType.BUY
                    if (sell) {
                        tradeHistory.add(TradeStock(TradeType.SELL, candle))

                    } else {
                        if (candle.getTodayYield() > 0.0) {
                            tradeHistory.add(TradeStock(TradeType.BUY, candle))
                        }
                    }
                }
                return tradeHistory.toImmutableList()
            }

            override fun calcYield(buy: CandleDto, sell: CandleDto, fee: Double): Double {
                return ApplicationUtil.getYield(buy.closePrice, sell.closePrice) - fee * 2
            }
        }
        calcTrade(backTester, stockCode, dateRange, fee)
    }

    // 결과: 쓰지 마
    @Test
    @DisplayName("오늘 종가가 양봉이면 종가 매수, 다음날 시가 매도. 당일 매도 후 매수 가능")
    fun test3() {
        val dateRange = DateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2022, 12, 31))
        val fee = 0.0
        val stockCode = KODEX_200_069500

        val backTester = object : FollowingBacktest {
            override fun trading(candleList: List<CandleDto>): List<TradeStock> {
                val tradeHistory = mutableListOf<TradeStock>()
                for (candle in candleList) {
                    printCandleInfo(candle)
                    val sell = tradeHistory.isNotEmpty() && tradeHistory.last().tradeType == TradeType.BUY
                    if (sell) {
                        tradeHistory.add(TradeStock(TradeType.SELL, candle))
                    }
                    if (candle.getTodayYield() > 0.0) {
                        tradeHistory.add(TradeStock(TradeType.BUY, candle))
                    }
                }
                return tradeHistory.toImmutableList()
            }

            override fun calcYield(buy: CandleDto, sell: CandleDto, fee: Double): Double {
                val rateReturn = ApplicationUtil.getYield(buy.closePrice, sell.openPrice) - fee * 2
                println(
                    "[${DateUtil.format(buy.candleDateTimeStart, "yyyy.MM.dd")}] " +
                            "매수: ${buy.closePrice}, " +
                            "[${DateUtil.format(sell.candleDateTimeStart, "yyyy.MM.dd")}] " +
                            "매도: ${sell.openPrice}, " +
                            "수익률: ${percent(rateReturn * 100)}"
                )
                return rateReturn
            }
        }
        calcTrade(backTester, stockCode, dateRange, fee)
    }

    // 결과: 쓰지 마
    @Test
    @DisplayName("n일 전 종가 대비 오늘 종가가 높으면 종가 매수, 다음날 종가 매도.")
    fun test4() {
        val dateRange = DateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2022, 12, 31))
        val fee = 0.0

        for (stockCode in listOf<StockCode>(
            KODEX_200_069500,
            KODEX_IV_2X_252670,
            KODEX_KOSDAQ_229200,
            KODEX_KOSDAQ_IV_251340,
            KODEX_BANK_091170
        )) {
            for (n in 1..5) {
                println(" === ${stockCode.name},  $n  ===")
                val backTester = object : FollowingBacktest {
                    override fun trading(candleList: List<CandleDto>): List<TradeStock> {
                        val tradeHistory = mutableListOf<TradeStock>()
                        for (i in n until candleList.size) {
                            val compareTargetCandle = candleList[i - n]
                            val todayCandle = candleList[i]

                            val sell = tradeHistory.isNotEmpty() && tradeHistory.last().tradeType == TradeType.BUY
                            if (sell) {
                                tradeHistory.add(TradeStock(TradeType.SELL, todayCandle))
                            } else if (compareTargetCandle.closePrice < todayCandle.closePrice) {
                                tradeHistory.add(TradeStock(TradeType.BUY, todayCandle))
                            }
                        }
                        return tradeHistory.toImmutableList()
                    }

                    override fun calcYield(buy: CandleDto, sell: CandleDto, fee: Double): Double {
                        val rateReturn = ApplicationUtil.getYield(buy.closePrice, sell.closePrice) - fee * 2
//                        println(
//                            "[${DateUtil.format(buy.candleDateTimeStart, "yyyy.MM.dd")}] " +
//                                    "매수: ${buy.closePrice}, " +
//                                    "[${DateUtil.format(sell.candleDateTimeStart, "yyyy.MM.dd")}] " +
//                                    "매도: ${sell.closePrice}, " +
//                                    "수익률: ${percent(rateReturn * 100)}"
//                        )
                        return rateReturn
                    }
                }
                calcTrade(backTester, stockCode, dateRange, fee)
            }
        }
    }

    // 결과: 쓰지마
    @Test
    @DisplayName(
        "매수조건: 오늘 종가가 5일 평균 이평선 보다 높고 && n일 전 종가 대비 오늘 종가가 높으면 종가 매수 " +
                " 매도 조건: 다음날 종가 매도"
    )
    fun test5() {
        val dateRange = DateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2022, 12, 31))
        val fee = 0.0

        for (stockCode in listOf<StockCode>(
            KODEX_200_069500,
            KODEX_IV_2X_252670,
            KODEX_KOSDAQ_229200,
            KODEX_KOSDAQ_IV_251340,
            KODEX_BANK_091170,
            OS_CODE_SPY
        )) {
            for (n in 1..5) {
                println(" === ${stockCode.name},  $n  ===")
                val backTester = object : FollowingBacktest {
                    override fun trading(candleList: List<CandleDto>): List<TradeStock> {
                        val tradeHistory = mutableListOf<TradeStock>()
                        for (i in n until candleList.size) {
                            val compareTargetCandle = candleList[i - n]
                            val todayCandle = candleList[i]

                            val sell = tradeHistory.isNotEmpty() && tradeHistory.last().tradeType == TradeType.BUY
                            val buy = compareTargetCandle.closePrice < todayCandle.closePrice
                                    && todayCandle.average[5] != null
                                    && todayCandle.average[5]!! < todayCandle.closePrice
                            if (sell) {
                                tradeHistory.add(TradeStock(TradeType.SELL, todayCandle))
                            } else if (buy) {
                                tradeHistory.add(TradeStock(TradeType.BUY, todayCandle))
                            }
                        }
                        return tradeHistory.toImmutableList()
                    }

                    override fun calcYield(buy: CandleDto, sell: CandleDto, fee: Double): Double {
                        val rateReturn = ApplicationUtil.getYield(buy.closePrice, sell.closePrice) - fee * 2
//                        println(
//                            "[${DateUtil.format(buy.candleDateTimeStart, "yyyy.MM.dd")}] " +
//                                    "매수: ${buy.closePrice}, " +
//                                    "[${DateUtil.format(sell.candleDateTimeStart, "yyyy.MM.dd")}] " +
//                                    "매도: ${sell.closePrice}, " +
//                                    "수익률: ${percent(rateReturn * 100)}"
//                        )
                        return rateReturn
                    }
                }
                calcTrade(backTester, stockCode, dateRange, fee)
            }
        }
    }

    // 의미는 있어 보이지만 기간에 따라 수익률이 들쑥날쑥함
    // 레버리지 종목은 처참함
    @Test
    @DisplayName(
        "매수: 당일 주가가 최근 n일 최고가보다 높은 경우 종가 매수\n" +
                "매도: 당일 주가가 최근 n일 최고가보다 낮은 경우 종가 매도\n"
    )
    fun test6() {
        val targetRange = DateRange("2001-01-01T00:00:00", "2023-01-01T00:00:00")

        val stockCode = StockCode.KODEX_200_069500
        val dateRange = candleRepository.findByCandleDateTimeBetween(
            listOf(stockCode.code),
            PeriodType.PERIOD_DAY,
            targetRange.from,
            targetRange.to
        )
        log.info("대상기간 변경: $targetRange -> $dateRange")
        val fee = 0.0

        val backTester = object : FollowingBacktest {
            override fun trading(candleList: List<CandleDto>): List<TradeStock> {
                val period = 20
                val periodCandleList = CircularFifoQueue<CandleDto>(period)
                // 최초 값 입력
                candleList.stream().limit(period.toLong()).forEach {
                    periodCandleList.add(it)
                }

                val tradeHistory = mutableListOf<TradeStock>()
                val yieldHistory = mutableListOf<Double>()

                var i = period
                while (i < candleList.size) {
                    val todayCandle = candleList[i]
                    val candleListSortByClosePrice =
                        periodCandleList.stream().sorted { o1, o2 -> o1.closePrice.compareTo(o2.closePrice) }.toList()

                    var max = candleListSortByClosePrice.last()
                    var min = candleListSortByClosePrice.first()
//                    println(
//                        "기준날짜: ${todayCandle.candleDateTimeStart}, 최근 $period 거래일(오늘제외) 종가 최고가: ${max.closePrice}(${max.candleDateTimeStart}), " +
//                                "종가 최적가: ${min.closePrice}(${min.candleDateTimeStart})"
//                    )
                    periodCandleList.add(todayCandle)
                    i++
                    val sellable = tradeHistory.isNotEmpty() && tradeHistory.last().tradeType == TradeType.BUY
                    if (sellable) {
                        // 오늘 종가가 기간 최저가 이하면 매도
                        if (todayCandle.closePrice <= min.closePrice) {
                            tradeHistory.add(TradeStock(TradeType.SELL, todayCandle))
                        }
                    }
                    // 오늘 종가가 기간 최고가 이상이면 매수
                    else if (todayCandle.closePrice >= max.closePrice) {
                        tradeHistory.add(TradeStock(TradeType.BUY, todayCandle))
                    }
                }
                return tradeHistory.toImmutableList()
            }

            override fun calcYield(buy: CandleDto, sell: CandleDto, fee: Double): Double {
                val rateReturn = ApplicationUtil.getYield(buy.closePrice, sell.closePrice) - fee * 2
                println(
                    "[${DateUtil.format(buy.candleDateTimeStart, "yyyy.MM.dd")}] " +
                            "매수: ${buy.closePrice}, " +
                            "[${DateUtil.format(sell.candleDateTimeStart, "yyyy.MM.dd")}] " +
                            "매도: ${sell.closePrice}, " +
                            "수익률: ${percent(rateReturn * 100)}"
                )
                return rateReturn
            }
        }

        calcTrade(backTester, stockCode, dateRange, fee)
    }


    @Test
    @DisplayName("현금(또는 채권) 1 : 1 혼합 전략")
    fun test7() {

    }


    private fun calcTrade(backTester: FollowingBacktest, stockCode: StockCode, dateRange: DateRange, fee: Double) {
        // 매매 수수료
        val candleList = movingAverageService.getMovingAverage(
            stockCode,
            PeriodType.PERIOD_DAY,
            PeriodType.PERIOD_DAY,
            listOf(5, 10, 20),
            dateRange
        )
        val tradeHistory = backTester.trading(candleList)

        var cumulativeYield = 1.0
        val yieldCumulativeHistory = mutableListOf<Double>()
        val yieldHistory = mutableListOf<Double>()
        for (i in tradeHistory.indices step 2) {
            if (tradeHistory.size <= i + 1) {
                break
            }
            val buy = tradeHistory[i]
            val sell = tradeHistory[i + 1]
            if (buy.tradeType != TradeType.BUY) {
                throw RuntimeException("매도 내역이 아님 $buy")
            }

            if (sell.tradeType != TradeType.SELL) {
                throw RuntimeException("매수 내역이 아님 $sell")
            }

            val rateReturn = backTester.calcYield(buy.candle, sell.candle, fee)
            yieldHistory.add(rateReturn)
            cumulativeYield *= (1 + rateReturn)
            yieldCumulativeHistory.add(cumulativeYield)
        }
        report(cumulativeYield, dateRange, yieldHistory, yieldCumulativeHistory, candleList)
    }

    private fun printCandleInfo(candle: CandleDto) {
        println(
            "[${candle.candleDateTimeStart}], " +
                    "O: ${candle.openPrice} H:${candle.highPrice} L:${candle.lowPrice} C:${candle.closePrice}, " +
                    "시초가 수익률: ${percent(candle.getOpenYield() * 100)}, " +
                    "당일 수익률: ${percent(candle.getTodayYield() * 100)}, " +
                    "수익률: ${percent(candle.getYield() * 100)}"
        )
    }

    private fun report(
        cumulativeYield: Double,
        dateRange: DateRange,
        yieldHistory: MutableList<Double>,
        yieldCumulativeHistory: MutableList<Double>,
        candleList: List<CandleDto>
    ) {
        println("=== 전략 결과 ===")
        println("누적 수익률: ${percent((cumulativeYield - 1) * 100)}")
        val between = Duration.between(dateRange.from, dateRange.to)
        val cagr = ApplicationUtil.getCagr(1.0, cumulativeYield, between.toDays().toInt())
        println("샤프지수: ${ApplicationUtil.getSharpeRatio(yieldHistory)}")
        println("CAGR: ${percent(cagr * 100)}")
        println("MDD: ${percent(ApplicationUtil.getMdd(yieldCumulativeHistory) * 100)}")
        val odds = yieldHistory.count { it > 0 }.toDouble() / yieldHistory.size.toDouble()
        println("승률: ${percent(odds * 100)}")
        println("매수/매도 쌍 건수: ${yieldHistory.size}")
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

    data class TradeStock(val tradeType: TradeType, val candle: CandleDto)

    interface FollowingBacktest {
        /**
         * @retrun 매수, 매도 이력
         */
        fun trading(candleList: List<CandleDto>): List<TradeStock>
        fun calcYield(buy: CandleDto, sell: CandleDto, fee: Double): Double
    }
}