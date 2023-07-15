package com.setvect.bokslstock2.backtest.laa.service

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.backtest.common.model.TradeNeo
import com.setvect.bokslstock2.backtest.common.service.BacktestTradeService
import com.setvect.bokslstock2.backtest.laa.model.LaaBacktestCondition
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.crawl.unemployment.model.UnemploymentData
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.GsonUtil
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*
import kotlin.math.abs

/**
 * LAA 전략 백테스트
 */
@Service
class LaaBacktestService(
    private val backtestTradeService: BacktestTradeService,
    private val movingAverageService: MovingAverageService,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun runTest(condition: LaaBacktestCondition): List<TradeNeo> {
        val stockCodes = condition.listStock(). toMutableList()

        val range = backtestTradeService.fitBacktestRange(
            condition.stockCodes.map { it.stockCode }, condition.range
        )
        log.info("범위 조건 변경: ${condition.range} -> $range")
        condition.range = range

        // <종목코드, <날짜, 캔들>>
        val periodType = condition.rebalanceFacter.periodType
        val stockPriceIndex = getStockPriceIndex(stockCodes, periodType)
        var current = ApplicationUtil.fitStartDate(condition.rebalanceFacter.periodType, condition.range.fromDate)

        // 기간이 부족하면 다음 기간으로 이동
        if (current.isBefore(condition.range.fromDate)) {
            current = ApplicationUtil.fitEndDate(condition.rebalanceFacter.periodType, condition.range.fromDate).plusDays(1)
        }
        var currentCash = condition.cash
        var beforeTradeList = mutableListOf<TradeNeo>()
        var beforeOffense = false

        val tradeList = mutableListOf<TradeNeo>()

        var deviation = 0.0
        while (current.isBefore(condition.range.toDate) || current.isEqual(condition.range.to.toLocalDate())) {
            val changeDeviation = deviation >= condition.rebalanceFacter.threshold
            val offense = isOffense(current, condition.testStockCode, condition.testMa)
            log.info("$current 공격자산 여부: $offense")
            val isChangeStock = beforeOffense != offense
            val stockByWeight = condition.stockCodes.associate { it.stockCode to it.weight }.toMutableMap()
            stockByWeight[condition.offenseCode] = condition.laaWeight
            stockByWeight[condition.defenseCode] = condition.laaWeight

            // 편차가 기준치 이상 리벨런싱
            if (beforeTradeList.isEmpty() || changeDeviation || isChangeStock) {
                // 원래는 부분적으로 샀다 팔아야 되는데 개발하기 귄찮아 전체 매도후 매수하는 방법으로 함
                // 시작 지점 시가 기준 매도
                val totalEvalPrice = beforeTradeList.sumOf {
                    val openPrice = stockPriceIndex[it.stockCode]!![current]!!.openPrice
                    it.qty * openPrice
                }
                beforeTradeList.forEach {
                    val candleDto = stockPriceIndex[it.stockCode]!![current]!!
                    val openPrice = candleDto.openPrice
                    val currentRate = ApplicationUtil.truncateDecimal(it.qty * openPrice / totalEvalPrice * 100.0, 2)
                    tradeList.add(
                        TradeNeo(
                            stockCode = it.stockCode,
                            tradeType = TradeType.SELL,
                            price = openPrice,
                            qty = it.qty,
                            tradeDate = candleDto.candleDateTimeStart,
                            memo = "요구비율: ${stockByWeight[it.stockCode]}%, 현재 비율: $currentRate%"
                        )
                    )
                    currentCash += it.qty * openPrice
                }

                val buyStockList = condition.stockCodes.associate { it.stockCode to it.weight }.toMutableMap()
                if (offense) {
                    buyStockList[condition.offenseCode] = condition.laaWeight
                } else {
                    buyStockList[condition.defenseCode] = condition.laaWeight
                }

                // 매수 하기
                beforeTradeList = buyStockList.map { entry ->
                    val candleDto: CandleDto = stockPriceIndex[entry.key]!![current]!!
                    val buyPrice = currentCash * (entry.value / 100.0)
                    val quantify = (buyPrice / candleDto.openPrice).toInt()
                    val currentRate = ApplicationUtil.truncateDecimal(quantify * candleDto.openPrice / currentCash * 100.0, 2)
                    TradeNeo(
                        stockCode = entry.key,
                        tradeType = TradeType.BUY,
                        price = candleDto.openPrice,
                        qty = quantify,
                        tradeDate = candleDto.candleDateTimeStart,
                        // currentCash으로 나눠서 계산하면 정확한 비율이 나오지는 않음. 그래도 오차가 별로 없기 때문에 그냥 적용하기로 함
                        memo = "요구비율: ${stockByWeight[entry.key]}%, 현재 비율: $currentRate%, 공격자산 여부: $offense"
                    )
                }.toMutableList()
                tradeList.addAll(beforeTradeList)
                currentCash -= beforeTradeList.sumOf { it.qty * it.price }
            }

            // 날짜 증가시키기 전 종가 기준 편차 계산
            deviation = deviation(beforeTradeList, stockPriceIndex, stockByWeight, current)
            current = ApplicationUtil.incrementDate(periodType, current)
            beforeOffense = offense
        }
        return tradeList
    }

    /**
     * [currentBuyTrade] 매수하고 있는 거래 내역
     * [stockPriceIndex] <종목코드, <날짜, 캔들>> 가격
     * [stockByWeight] 종목별 보유 가중치
     * [current] 현재 날짜
     * @return 종가 기준 편차 계산
     */
    private fun deviation(
        currentBuyTrade: MutableList<TradeNeo>,
        stockPriceIndex: Map<StockCode, Map<LocalDate, CandleDto>>,
        stockByWeight: Map<StockCode, Int>,
        current: LocalDate
    ): Double {
        // 현재 평가 가격을 모두 더함
        val sum = currentBuyTrade.sumOf { getEvalPrice(stockPriceIndex, it, current) }

        return currentBuyTrade.sumOf {
            val d = stockByWeight[it.stockCode]!! / 100.0
            val evalPrice = getEvalPrice(stockPriceIndex, it, current)
            abs(d - (evalPrice / sum))
        }
    }

    /**
     * @return 평가금액
     */
    private fun getEvalPrice(
        stockPriceIndex: Map<StockCode, Map<LocalDate, CandleDto>>,
        it: TradeNeo,
        current: LocalDate
    ) = stockPriceIndex[it.stockCode]!![current]!!.closePrice * it.qty

    /**
     * 공격자산 수비자산 판단 기준
     */
    private fun isOffense(current: LocalDate, testStockCode: StockCode, testMa: Int): Boolean {
        val movingAverage = movingAverageService.getMovingAverage(
            testStockCode,
            PeriodType.PERIOD_DAY,
            PeriodType.PERIOD_DAY,
            listOf(testMa),
            DateRange(current.minusMonths(13), current)
        )

        val maPrice = movingAverage.last().average[testMa]!!
        // 공격자산 판단 지수가 이동평균보다 높으면 공격자산 선택
        if (maPrice < movingAverage.last().openPrice) {
            return true
        }

        // 실업률 통계
        val unemploymentData: UnemploymentData = getUnemploymentData()
        val observations = unemploymentData.observations
        // 현재 날짜보다 1개월 이전 실업률 데이터
        val beforeMonth = current.minusMonths(1)
        val currentRate = observations.last { it.date.isBefore(beforeMonth) }.value.toDouble()

        // 현재 날짜보다 1개월 이전 부터 13개월 이전 사이의 실업률 평균
        val averageRate = observations.filter {
            it.date.isAfter(current.minusMonths(13)) && it.date.isBefore(beforeMonth)
        }.map { it.value.toDouble() }.average()

        // 최근 실업률이 12개월 평균 실업률보다 낮으면 공격자산 선택
        log.info("조회 날짜: $current, 최근 실업률 날짜: $beforeMonth, 최근 실업률: $currentRate%, 12개월 평균 실업률: ${ApplicationUtil.truncateDecimal(averageRate, 2)}%")
        return currentRate < averageRate
    }

    companion object {
        /**
         * @return 미국 실업률 통계
         */
        private fun getUnemploymentData(): UnemploymentData {
            val file = FileUtils.getFile("crawl/미국실업률통계", "/unemployment_rate.json")
            val json = FileUtils.readFileToString(file, "UTF-8");
            return GsonUtil.GSON.fromJson(json, UnemploymentData::class.java)
        }
    }

    /**
     * @return <종목코드, <날짜, 캔들>>
     */
    private fun getStockPriceIndex(
        stockCodes: List<StockCode>,
        periodType: PeriodType,
    ): Map<StockCode, Map<LocalDate, CandleDto>> {
        val stockPriceIndex = stockCodes.associateWith { code ->
            val movingAverage = movingAverageService.getMovingAverage(
                code,
                PeriodType.PERIOD_DAY,
                periodType,
                Collections.emptyList(),
            )
            movingAverage.associateBy { it.candleDateTimeStart.toLocalDate().withDayOfMonth(1) }
        }
        return stockPriceIndex
    }
}