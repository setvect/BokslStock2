package com.setvect.bokslstock2.analysis.rebalance.service

import com.setvect.bokslstock2.analysis.common.model.PreTrade
import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.analysis.common.model.Trade
import com.setvect.bokslstock2.analysis.common.service.BacktestTradeService
import com.setvect.bokslstock2.analysis.rebalance.model.RebalanceBacktestCondition
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.math.abs

/**
 * 리벨런싱 백테스트
 */
@Service
class RebalanceAnalysisService(
    private val stockRepository: StockRepository,
    private val backtestTradeService: BacktestTradeService,
    private val movingAverageService: MovingAverageService,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)


    fun makeSummaryReport(conditionList: List<RebalanceBacktestCondition>): File {
        var i = 0

        val conditionResults = conditionList.map { backtestCondition ->
            checkValidate(backtestCondition)
            val range = backtestTradeService.fitBacktestRange(
                backtestCondition.stockCodes.map { it.stockCode },
                backtestCondition.tradeCondition.range
            )
            log.info("범위 조건 변경: ${backtestCondition.tradeCondition.range} -> $range")
            backtestCondition.tradeCondition.range = range

            val trades = processRebalance(backtestCondition)
            val result = backtestTradeService.analysis(
                trades,
                backtestCondition.tradeCondition,
                backtestCondition.stockCodes.map { it.stockCode }
            )
            val summary = RebalanceReportHelper.getSummary(backtestCondition, result.common)
            log.info(summary)

            log.info("분석 진행 ${++i}/${conditionList.size}")
            Pair(backtestCondition, result)
        }

        for (idx in conditionResults.indices) {
            val conditionResult = conditionResults[idx]
            RebalanceReportHelper.makeReportFile(conditionResult.first, conditionResult.second)
            log.info("개별분석파일 생성 ${idx + 1}/${conditionList.size}")
        }

        // 결과 저장
        val reportFile =
            File("./backtest-result", "리벨런싱_전략_백테스트_분석결과_" + Timestamp.valueOf(LocalDateTime.now()).time + ".xlsx")
        XSSFWorkbook().use { workbook ->
            val sheetBacktestSummary = RebalanceReportHelper.createTotalSummary(workbook, conditionResults)
            workbook.setSheetName(workbook.getSheetIndex(sheetBacktestSummary), "1. 평가표")

            FileOutputStream(reportFile).use { ous ->
                workbook.write(ous)
            }
        }
        println("결과 파일:" + reportFile.name)
        return reportFile
    }


    private fun processRebalance(condition: RebalanceBacktestCondition): MutableList<Trade> {
        val rebalanceTradeHistory = makeRebalance(condition)
        return makeTrades(condition, rebalanceTradeHistory)
    }

    private fun makeRebalance(condition: RebalanceBacktestCondition): MutableList<TradeInfo> {
        val stockCodes = condition.listStock()
        // <종목코드, <날짜, 캔들>>
        val periodType = condition.rebalanceFacter.periodType

        val stockPriceIndex = getStockPriceIndex(stockCodes, periodType)

        var current =
            ApplicationUtil.fitStartDate(condition.rebalanceFacter.periodType, condition.tradeCondition.range.fromDate)
        var beforeTrade = TradeInfo(date = current, buyStocks = listOf(), cash = condition.tradeCondition.cash, false)

        val rebalanceTradeHistory = mutableListOf<TradeInfo>()

        while (current.isBefore(condition.tradeCondition.range.toDate) || current.isEqual(condition.tradeCondition.range.to.toLocalDate())) {
            val changeDeviation = beforeTrade.deviation() >= condition.rebalanceFacter.threshold
            // 편차가 기준치 이상 리벨런싱
            if (beforeTrade.buyStocks.isEmpty() || changeDeviation) {
                // 원래는 부분적으로 샀다 팔아야 되는데 개발하기 귄찮아 전체 매도후 매수하는 방법으로 함
                val (buyStocks, afterCash) = rebalance(beforeTrade, condition, stockPriceIndex, current)

                rebalanceTradeHistory.add(TradeInfo(current, buyStocks, afterCash, true))
            }
            // 편차가 기준이 미만이면 종목 가격만 교체
            else {
                val buyStocks = beforeTrade.buyStocks.map {
                    val candleDto: CandleDto = stockPriceIndex[it.candle.stockCode]!![current]!!
                    BuyStock(candleDto, it.qty, it.weight)
                }
                rebalanceTradeHistory.add(TradeInfo(current, buyStocks, beforeTrade.cash, false))
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
            trade.buyStocks.forEach { stock ->
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

    private fun makeTrades(
        condition: RebalanceBacktestCondition,
        rebalanceTradeHistory: MutableList<TradeInfo>
    ): MutableList<Trade> {
        val tradeItemHistory = mutableListOf<Trade>()
        // <종목코드, 종목정보>
        val codeByStock =
            condition.stockCodes.map { it.stockCode }.associateWith { stockRepository.findByCode(it.code).get() }
        // <종목코드, 직전 preTrade>
        val buyStock = HashMap<StockCode, Trade>()

        var currentCash = 0.0

        rebalanceTradeHistory.forEach { rebalanceItem ->
            // 리벨런싱 됐을 때만 매매
            if (!rebalanceItem.rebalance) {
                return@forEach
            }
            if (buyStock.isNotEmpty()) {
                // ---------- 매도
                rebalanceItem.buyStocks.forEach { rebalStock ->
                    val candle: CandleDto = rebalStock.candle
                    val stock = codeByStock[candle.stockCode]!!

                    val buyTrade = buyStock[candle.stockCode]
                        ?: throw RuntimeException("${candle.stockCode} 매수 내역이 없습니다.")

                    val preTrade = PreTrade(
                        stockCode = StockCode.findByCode(stock.code),
                        tradeType = TradeType.SELL,
                        yield = ApplicationUtil.getYield(buyTrade.preTrade.unitPrice, candle.openPrice),
                        unitPrice = candle.openPrice,
                        tradeDate = candle.candleDateTimeStart,
                    )


                    buyStock.remove(StockCode.findByCode(preTrade.stockCode.code))
                    val sellPrice = buyTrade.getBuyAmount() * (1 + preTrade.yield)
                    val sellFee = sellPrice * condition.tradeCondition.feeSell
                    val gains = sellPrice - buyTrade.getBuyAmount()

                    // 매매후 현금
                    currentCash += sellPrice - sellFee
                    val stockEvalPrice =
                        buyStock.entries.map { it.value }.sumOf { it.preTrade.unitPrice * it.qty }
                    val tradeReportItem = Trade(
                        preTrade = preTrade,
                        qty = 0,
                        cash = currentCash,
                        feePrice = sellFee,
                        gains = gains,
                        stockEvalPrice = stockEvalPrice
                    )
                    tradeItemHistory.add(tradeReportItem)
                }
                currentCash += rebalanceItem.cash
            } else {
                currentCash = condition.tradeCondition.cash
            }


            // ---------- 매수
            rebalanceItem.buyStocks.forEach { rebalStock ->
                val candle: CandleDto = rebalStock.candle
                val stock = codeByStock[candle.stockCode]!!

                val preTrade = PreTrade(
                    stockCode = StockCode.findByCode(stock.code),
                    tradeType = TradeType.BUY,
                    yield = 0.0,
                    unitPrice = candle.openPrice,
                    tradeDate = candle.candleDateTimeStart,
                )
                // 매수 금액
                val buyAmount = rebalStock.getEvalPriceOpen()
                val feePrice = condition.tradeCondition.feeBuy * buyAmount

                // 매수후 현금
                currentCash -= buyAmount + feePrice

                // 현재 매수 종목 포함해 보유하고 있는 주식 평가 금액
                val stockEvalPrice = buyStock.entries.map { it.value }
                    .sumOf { it.preTrade.unitPrice * it.qty } + rebalStock.qty * preTrade.unitPrice

                val tradeReportItem = Trade(
                    preTrade = preTrade,
                    qty = rebalStock.qty,
                    cash = currentCash,
                    feePrice = feePrice,
                    gains = 0.0,
                    stockEvalPrice = stockEvalPrice
                )
                tradeItemHistory.add(tradeReportItem)
                buyStock[StockCode.findByCode(preTrade.stockCode.code)] = tradeReportItem
            }
        }
        return tradeItemHistory
    }

    /**
     * 전체 종목을 일괄 매도후 비중에 맞게 다시 매수
     */
    private fun rebalance(
        beforeTrade: TradeInfo,
        condition: RebalanceBacktestCondition,
        stockPriceIndex: Map<StockCode, Map<LocalDate, CandleDto>>,
        current: LocalDate
    ): Pair<List<BuyStock>, Double> {
        // 시작 지점 시가 기준 매도
        val sellAmount =
            beforeTrade.buyStocks.sumOf {
                val candleDto: CandleDto = stockPriceIndex[it.candle.stockCode]!![current]!!
                it.qty * candleDto.openPrice
            }
        val currentCash = sellAmount + beforeTrade.cash

        val buyStocks = condition.stockCodes.map { stock ->
            val candleDto: CandleDto = stockPriceIndex[stock.stockCode]!![current]!!

            val buyPrice = currentCash * (stock.weight / 100.0)
            val quantify = (buyPrice / candleDto.openPrice).toInt()
            BuyStock(candleDto, quantify, stock.weight)
        }
        val afterCash = currentCash - buyStocks.sumOf { it.getEvalPriceOpen() }
        return Pair(buyStocks, afterCash)
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
            movingAverageService.getMovingAverage(
                code,
                PeriodType.PERIOD_DAY,
                periodType,
                Collections.emptyList(),
            )
                .associateBy { it.candleDateTimeStart.toLocalDate().withDayOfMonth(1) }
        }
        return stockPriceIndex
    }

    /**
     * 리빌런싱 단위별 매매 내역
     * @property date 리벨런싱 날짜
     * @property buyStocks 매수 종목
     * @property cash 매수 후 남은 현금
     * @property rebalance 해당 매매 주기에서 리벨런싱을 했는지 여부
     */
    data class TradeInfo(
        val date: LocalDate,
        val buyStocks: List<BuyStock>,
        val cash: Double,
        val rebalance: Boolean
    ) {
        /**
         * @return 시초가 기준 현금포함 평가금액
         */
        fun getEvalPriceOpen(): Double {
            return buyStocks.sumOf { it.getEvalPriceOpen() } + cash
        }

        /**
         * @return 종가 기준 현금포함 평가금액
         */
        fun getEvalPriceClose(): Double {
            return buyStocks.sumOf { it.getEvalPriceClose() } + cash
        }

        /**
         * @return 종가 기준 평가금액
         */
        fun getEvalPriceCloseWithoutCash(): Double {
            return buyStocks.sumOf { it.getEvalPriceClose() }
        }

        /**
         * 예를들어 4종목 비중이 모두 25%이면 분산 값은 0
         *
         * A: 0.2
         * B: 0.3
         * C: 0.24
         * D: 0.26
         * 이면 분산값은 0.12
         *
         * @return 종가기준 편차(현금 포함하지 않음)
         */
        fun deviation(): Double {
            val sum = buyStocks.sumOf { it.getEvalPriceClose() }

            return buyStocks.sumOf {
                val d = it.weight / 100.0
                abs(d - (it.getEvalPriceClose() / sum))
            }
        }
    }


    /**
     * @property candle 매수 종목 정보
     * @property qty 매수 수량
     * @property weight 해당 종목의 전체 투자대비 비중(%)
     */
    data class BuyStock(val candle: CandleDto, val qty: Int, val weight: Int) {

        fun getEvalPriceOpen(): Double {
            return candle.openPrice * qty
        }

        fun getEvalPriceClose(): Double {
            return candle.closePrice * qty
        }

        /**
         * 종가기준 전체 [totalPrice] 대비 비중(%)
         */
        fun realWeight(totalPrice: Double): Double {
            return getEvalPriceClose() / totalPrice * 100
        }
    }
}