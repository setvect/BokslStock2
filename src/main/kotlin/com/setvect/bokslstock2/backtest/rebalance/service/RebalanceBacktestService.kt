package com.setvect.bokslstock2.backtest.rebalance.service

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.backtest.common.model.TradeNeo
import com.setvect.bokslstock2.backtest.common.service.BacktestTradeService
import com.setvect.bokslstock2.backtest.rebalance.model.RebalanceBuyStock
import com.setvect.bokslstock2.backtest.rebalance.model.RebalanceBacktestCondition
import com.setvect.bokslstock2.backtest.rebalance.model.RebalanceTrade
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import okhttp3.internal.toImmutableList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

/**
 * 리벨런싱 백테스트
 */
@Service
class RebalanceBacktestService(
    private val stockRepository: StockRepository,
    private val backtestTradeService: BacktestTradeService,
    private val movingAverageService: MovingAverageService,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun runTest(condition: RebalanceBacktestCondition): List<TradeNeo> {
        val rebalanceTradeHistory = makeRebalance(condition)
        return makeTrades(condition, rebalanceTradeHistory)
    }

    private fun makeRebalance(condition: RebalanceBacktestCondition): MutableList<RebalanceTrade> {
        val stockCodes = condition.listStock()

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

        var beforeTrade = RebalanceTrade(date = current, rebalanceBuyStocks = listOf(), cash = condition.cash, false)

        val rebalanceTradeHistory = mutableListOf<RebalanceTrade>()

        while (current.isBefore(condition.range.toDate) || current.isEqual(condition.range.to.toLocalDate())) {
            val changeDeviation = beforeTrade.deviation() >= condition.rebalanceFacter.threshold
            // 편차가 기준치 이상 리벨런싱
            if (beforeTrade.rebalanceBuyStocks.isEmpty() || changeDeviation) {
                // 원래는 부분적으로 샀다 팔아야 되는데 개발하기 귄찮아 전체 매도후 매수하는 방법으로 함
                val (buyStocks, afterCash) = rebalance(beforeTrade, condition, stockPriceIndex, current)

                rebalanceTradeHistory.add(RebalanceTrade(current, buyStocks, afterCash, true))
            }
            // 편차가 기준이 미만이면 종목 가격만 교체
            else {
                val rebalanceBuyStocks = beforeTrade.rebalanceBuyStocks.map {
                    val candleDto: CandleDto = stockPriceIndex[it.candle.stockCode]!![current]!!
                    RebalanceBuyStock(candleDto, it.qty, it.weight)
                }
                rebalanceTradeHistory.add(RebalanceTrade(current, rebalanceBuyStocks, beforeTrade.cash, false))
            }
            current = incrementDate(periodType, current)
            beforeTrade = rebalanceTradeHistory.last()
        }

        rebalanceTradeHistory.forEach { trade ->
            log.info(
                "날짜: ${trade.date}, " +
                        "종가 평가가격: ${trade.getEvalPriceClose()}, " +
                        "편차: ${String.format("%,.4f", trade.deviation())}, " +
                        "리벨런싱: ${trade.rebalance}"
            )
            trade.rebalanceBuyStocks.forEach { stock ->
                log.info(
                    "\t종목:${stock.candle.stockCode}, " +
                            "수량: ${stock.qty}, " +
                            "종가: ${stock.candle.closePrice}, " +
                            "평가금액: ${stock.getEvalPriceClose()}, " +
                            "설정비중: ${stock.weight}%, " +
                            "현재비중: ${String.format("%,.3f%%", stock.realWeight(trade.getEvalPriceCloseWithoutCash()))}",
                )
            }
        }
        return rebalanceTradeHistory
    }

    private fun makeTrades(condition: RebalanceBacktestCondition, rebalanceTradeHistory: List<RebalanceTrade>): List<TradeNeo> {
        val tradeItemHistory = mutableListOf<TradeNeo>()
        // <종목코드, 종목정보>
        val codeByStock =
            condition.stockCodes.map { it.stockCode }.associateWith { stockRepository.findByCode(it.code).get() }
        // <종목코드, 직전 preTrade>
        val buyStock = HashMap<StockCode, TradeNeo>()

        rebalanceTradeHistory.forEach { rebalanceItem ->
            // 리벨런싱 됐을 때만 매매
            if (!rebalanceItem.rebalance) {
                return@forEach
            }
            if (buyStock.isNotEmpty()) {
                // ---------- 매도
                rebalanceItem.rebalanceBuyStocks.forEach { rebalStock ->
                    val candle: CandleDto = rebalStock.candle
                    val stock = codeByStock[candle.stockCode]!!
                    val buyTrade = buyStock[candle.stockCode] ?: throw RuntimeException("${candle.stockCode} 매수 내역이 없습니다.")

                    buyStock.remove(rebalStock.candle.stockCode)

                    val tradeReportItem = TradeNeo(
                        stockCode = StockCode.findByCode(stock.code),
                        tradeType = TradeType.SELL,
                        price = candle.openPrice,
                        qty = buyTrade.qty,
                        tradeDate = candle.candleDateTimeStart,
                    )
                    tradeItemHistory.add(tradeReportItem)

                }
            }

            // ---------- 매수
            rebalanceItem.rebalanceBuyStocks.forEach { rebalStock ->
                val candle: CandleDto = rebalStock.candle
                val stock = codeByStock[candle.stockCode]!!

                // 매수후 현금
                val tradeReportItem = TradeNeo(
                    stockCode = stock.convertStockCode(),
                    tradeType = TradeType.BUY,
                    price = candle.openPrice,
                    qty = rebalStock.qty,
                    tradeDate = candle.candleDateTimeStart,
                )
                tradeItemHistory.add(tradeReportItem)
                buyStock[StockCode.findByCode(stock.code)] = tradeReportItem
            }
        }
        return tradeItemHistory.toImmutableList()
    }

    /**
     * 전체 종목을 일괄 매도후 비중에 맞게 다시 매수
     */
    private fun rebalance(
        beforeTrade: RebalanceTrade,
        condition: RebalanceBacktestCondition,
        stockPriceIndex: Map<StockCode, Map<LocalDate, CandleDto>>,
        current: LocalDate
    ): Pair<List<RebalanceBuyStock>, Double> {
        // 시작 지점 시가 기준 매도
        val sellAmount =
            beforeTrade.rebalanceBuyStocks.sumOf {
                val candleDto: CandleDto = stockPriceIndex[it.candle.stockCode]!![current]!!
                it.qty * candleDto.openPrice
            }
        val currentCash = sellAmount + beforeTrade.cash

        val rebalanceBuyStocks = condition.stockCodes.map { stock ->
            val candleDto: CandleDto = stockPriceIndex[stock.stockCode]!![current]!!

            val buyPrice = currentCash * (stock.weight / 100.0)
            val quantify = (buyPrice / candleDto.openPrice).toInt()
            RebalanceBuyStock(candleDto, quantify, stock.weight)
        }
        val afterCash = currentCash - rebalanceBuyStocks.sumOf { it.getEvalPriceOpen() }
        return Pair(rebalanceBuyStocks, afterCash)
    }

    private fun incrementDate(
        periodType: PeriodType,
        current: LocalDate
    ): LocalDate {
        return when (periodType) {
            PeriodType.PERIOD_WEEK -> current.plusWeeks(1)
            PeriodType.PERIOD_MONTH -> current.plusMonths(1)
            PeriodType.PERIOD_QUARTER -> current.plusMonths(3)
            PeriodType.PERIOD_HALF -> current.plusMonths(6)
            PeriodType.PERIOD_YEAR -> current.plusYears(1)
            else -> current
        }
    }


    private fun checkValidate(rebalanceBacktestCondition: RebalanceBacktestCondition) {
        val sumWeight = rebalanceBacktestCondition.stockCodes.sumOf { it.weight }
        if (sumWeight != 100) {
            throw RuntimeException("비중 합계는 100이여야 됩니다.")
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