package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.model.*
import com.setvect.bokslstock2.analysis.common.util.StockByDateCandle
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.deepCopyWithSerialization
import okhttp3.internal.toImmutableList
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 매매 계좌
 * 매매 내역을 추상화 시켜 다양한 백테스트의 결과를 파악할 때 활용할 수 있음
 */
class AccountService(
    private val stockCommonFactory: StockCommonFactory,
    private val accountCondition: AccountCondition,
    private val backtestCondition: BacktestCondition
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 거래 내역
     * 매매 순서를 오름차순으로 기록
     */
    private val tradeHistory = mutableListOf<TradeNeo>()

    /**
     * 백테스트 매매 결과
     */
    private lateinit var tradeResult: MutableList<TradeResult>

    /**
     * 평가 금액 변동 내역
     */
    private lateinit var evaluationAmountHistory: List<EvaluationRateItem>

    /**
     * 종목별 종가 이력
     */
    private lateinit var stockClosePriceHistory: MutableMap<StockCode, MutableMap<LocalDate, Double>>

    fun addTrade(tradeNeo: TradeNeo) {
        tradeHistory.add(tradeNeo)
    }

    fun addTrade(tradeNeo: List<TradeNeo>) {
        tradeHistory.addAll(tradeNeo)
    }

    /**
     * @return 매매 내역 바탕으로 건별 매매 결과 목록
     */
    fun calcTradeResult(): List<TradeResult> {
        tradeResult = mutableListOf()
        // 종목 잔고
        val averagePriceMap = mutableMapOf<StockCode, StockAccount>()
        var cash = accountCondition.cash
        // 거래 시작일과 종료일 범위를 구함
        val from = tradeHistory.first().tradeDate.with(LocalTime.MIN)
        val to = tradeHistory.last().tradeDate.with(LocalTime.MAX)
        val tradeDateRange = DateRange(from, to)

        val stockCodes = tradeHistory.map { it.stockCode }.toSet()
        val stockByDateCandle: StockByDateCandle = stockCommonFactory.createStockByDateCandle(stockCodes, tradeDateRange)

        tradeHistory.forEach { tradeNeo ->
            val stockAccount = averagePriceMap.getOrDefault(tradeNeo.stockCode, StockAccount(0, 0.0))
            averagePriceMap[tradeNeo.stockCode] = stockAccount
            var yieldRate = 0.0
            var feePrice = 0.0
            var gains = 0.0
            if (tradeNeo.tradeType == TradeType.BUY) {
                feePrice = tradeNeo.price * tradeNeo.qty * accountCondition.feeBuy
                stockAccount.buy(tradeNeo.price, tradeNeo.qty)
                cash -= tradeNeo.price * tradeNeo.qty + feePrice
            } else if (tradeNeo.tradeType == TradeType.SELL) {
                feePrice = tradeNeo.price * tradeNeo.qty * accountCondition.feeSell
                val averagePrice = stockAccount.getAveragePrice()
                yieldRate = (tradeNeo.price - averagePrice) / averagePrice
                stockAccount.sell(tradeNeo.qty)
                cash += tradeNeo.price * tradeNeo.qty - feePrice
                gains = (tradeNeo.price - averagePrice) * tradeNeo.qty
            }

            val purchasePrice = averagePriceMap.map { it.value.totalBuyPrice }.sum()
            // 거래 날짜 종가 기준으로 주식 평가 금액을 계산
            val stockEvalPrice = averagePriceMap.map { (stockCode, stockAccount) ->
                // 해당 종목, 날짜의 종가를 구함
                val candle = stockByDateCandle.getNearCandle(stockCode, tradeNeo.tradeDate.toLocalDate())
                val price = candle.closePrice
                price * stockAccount.qty
            }.sum()

            tradeResult.add(
                TradeResult(
                    stockCode = tradeNeo.stockCode,
                    tradeType = tradeNeo.tradeType,
                    yieldRate = yieldRate,
                    price = tradeNeo.price,
                    qty = tradeNeo.qty,
                    feePrice = feePrice,
                    gains = gains,
                    tradeDate = tradeNeo.tradeDate,
                    cash = cash,
                    purchasePrice = purchasePrice,
                    stockEvalPrice = stockEvalPrice,
                    profitRate = (stockEvalPrice + cash) / accountCondition.cash,
                    stockAccount = averagePriceMap.deepCopyWithSerialization(StockCode::class.java),
                    backtestConditionName = tradeNeo.getBacktestConditionName(),
                    memo = tradeNeo.memo
                )
            )
        }
        return tradeResult.toImmutableList()
    }

    fun calcEvaluationRate(): List<EvaluationRateItem> {
        evaluationAmountHistory = evaluationRateItems(getStockCodes(), backtestCondition.benchmarkStockCode)
        return evaluationAmountHistory.toImmutableList()
    }

    /**
     * @return 특정 기간을 기준으로 가장 최근 매매 상태
     */
    private fun getNearTrade(date: LocalDate): TradeResult? {
        return tradeResult.lastOrNull { it.tradeDate <= date.atTime(LocalTime.MAX) }
    }

    /**
     * @return 모든 매매 종목을 반환
     */
    private fun getStockCodes(): Set<StockCode> {
        return tradeHistory.map { it.stockCode }.toSet()
    }

    /**
     * @return 모든 매매 백테스트 조건 이름
     */
    private fun getBacktestNames(): Set<String> {
        return tradeHistory.map { it.getBacktestConditionName() }.toSet()
    }


    /**
     * [holdStockCodes] 보유 종목 - 동일비중으로 계산
     * [benchmarkStockCode] 밴치마크
     * @return 매매 전략, 동일 비중 종목 매매, 밴치마크 각 일자별 수익률
     */
    private fun evaluationRateItems(
        holdStockCodes: Set<StockCode>,
        benchmarkStockCode: StockCode
    ): List<EvaluationRateItem> {
        val evaluationAmountHistory = mutableListOf<EvaluationRateItem>()

        val stockCodes = getStockCodes() union holdStockCodes union setOf(benchmarkStockCode)

        val backtestPeriod = backtestCondition.backtestPeriod

        stockClosePriceHistory = getClosePriceByStockCodes(stockCodes, backtestPeriod)

        var buyHoldLastRate = 1.0
        var benchmarkLastRate = 1.0
        var backtestLastRate = 1.0

        // 시작 날짜부터 하루씩 증가 시켜 종료날짜 까지 루프
        var date = backtestPeriod.from
        while (date <= backtestPeriod.to) {

            // 1. 벤치마크 평가금액 비율
            val benchmarkBeforeRate = benchmarkLastRate
            val beforeClosePrice = stockClosePriceHistory[benchmarkStockCode]!![date.toLocalDate().minusDays(1)]
            val currentClosePrice = stockClosePriceHistory[benchmarkStockCode]!![date.toLocalDate()]
            if (beforeClosePrice != null && currentClosePrice != null) {
                benchmarkLastRate = benchmarkLastRate * currentClosePrice / beforeClosePrice
            }
            val benchmarkYield = ApplicationUtil.getYield(benchmarkBeforeRate, benchmarkLastRate)

            // 2. buy & hold 평가금액 비율
            val buyHoldBeforeRate = buyHoldLastRate
            //  holdStockCodes 종목의 직전 종가와 현재 종가의 비율을 계산하여 평균을 구함
            buyHoldLastRate *= holdStockCodes.map {
                val bClosePrice = stockClosePriceHistory[it]!![date.toLocalDate().minusDays(1)]
                val cClosePrice = stockClosePriceHistory[it]!![date.toLocalDate()]
                if (bClosePrice != null && cClosePrice != null) {
                    cClosePrice / bClosePrice
                } else {
                    1.0
                }
            }.average()
            val buyHoldYield = ApplicationUtil.getYield(buyHoldBeforeRate, buyHoldLastRate)

            // 3. 백테스트 평가금액 비율
            val nearTrade = getNearTrade(date.toLocalDate())
            val backtestBeforeRate = backtestLastRate
            var backtestYield = 0.0
            if (nearTrade != null) {
                // 종가 기준으로 보유 주식의 평가금액을 구함
                val stockEvalPrice = nearTrade.stockAccount.entries.sumOf { (stockCode, stockAccount) ->
                    stockAccount.qty * stockClosePriceHistory[stockCode]!![date.toLocalDate()]!!
                }
                val backtestCurrentRate = (stockEvalPrice + nearTrade.cash) / accountCondition.cash
                backtestLastRate = backtestCurrentRate
                backtestYield = ApplicationUtil.getYield(backtestBeforeRate, backtestLastRate)
            }

            evaluationAmountHistory.add(
                EvaluationRateItem(
                    baseDate = date,
                    buyHoldRate = buyHoldLastRate,
                    benchmarkRate = benchmarkLastRate,
                    backtestRate = backtestLastRate,
                    buyHoldYield = buyHoldYield,
                    benchmarkYield = benchmarkYield,
                    backtestYield = backtestYield
                )
            )

            date = date.plusDays(1)
        }
        return evaluationAmountHistory

    }

    fun makeReport(reportFile: File) {
        if (tradeResult.isEmpty()) {
            throw IllegalArgumentException("거래 내역이 없습니다.")
        }
        if (evaluationAmountHistory.isEmpty()) {
            throw IllegalArgumentException("자산변화 정보가 없습니다.")
        }
        XSSFWorkbook().use { workbook ->
            var sheet = ReportMakerHelperService.createTradeReport(tradeResult, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "1. 매매이력")

            sheet = ReportMakerHelperService.createReportEvalAmount(evaluationAmountHistory, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "2. 일자별 자산변화")

            sheet = ReportMakerHelperService.createReportRangeReturn(getMonthlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "3. 월별 수익률")

            sheet = ReportMakerHelperService.createReportRangeReturn(getYearlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "4. 년별 수익률")

            sheet = createReportSummary(workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "5. 매매 요약결과 및 조건")

            FileOutputStream(reportFile).use { ous ->
                workbook.write(ous)
            }
        }
    }

    /**
     * @return 종목별 날짜별 종가 <종목, <날짜, 종가>>
     */
    private fun getClosePriceByStockCodes(
        stockCodes: Set<StockCode>,
        backtestPeriod: DateRange
    ): MutableMap<StockCode, MutableMap<LocalDate, Double>> {
        val stockByDateCandle: StockByDateCandle = stockCommonFactory.createStockByDateCandle(stockCodes, backtestPeriod)

        // 시작 날짜부터 하루씩 증가 시켜 종료날짜 까지 루프, 각 종목을 기준으로 날짜별 종가를 구함
        val stockClosePriceHistory: MutableMap<StockCode, MutableMap<LocalDate, Double>> = stockCodes.associateWith {
            var date = backtestPeriod.from

            val stockClosePrice: MutableMap<LocalDate, Double> = mutableMapOf()

            // 해당 날짜에 시세가 없으면 직전 시세로 대체
            while (date <= backtestPeriod.to) {
                val candle = stockByDateCandle.getCandle(it, date.toLocalDate())
                if (candle != null) {
                    stockClosePrice[date.toLocalDate()] = candle.closePrice
                } else if (stockClosePrice.keys.isNotEmpty()) {
                    // 가장 마지막 종가로 대체
                    stockClosePrice[date.toLocalDate()] = stockClosePrice[stockClosePrice.keys.last()]!!
                }
                date = date.plusDays(1)
            }
            stockClosePrice
        }.toMutableMap()
        return stockClosePriceHistory
    }

    /**
     * @return 월별 buy&hold 수익률, 전략 수익률 정보
     */
    fun getMonthlyYield(): List<YieldRateItem> {
        val monthEval = evaluationAmountHistory.groupBy { it.baseDate.withDayOfMonth(1) }
        return groupByYield(monthEval)
    }

    /**
     * @return 년별 buy&hold 수익률, 전략 수익률 정보
     */
    fun getYearlyYield(): List<YieldRateItem> {
        val yearEval = evaluationAmountHistory.groupBy { it.baseDate.withMonth(1).withDayOfMonth(1) }
        return groupByYield(yearEval)
    }

    private fun groupByYield(monthEval: Map<LocalDateTime, List<EvaluationRateItem>>): List<YieldRateItem> {
        return monthEval.entries.map {
            YieldRateItem(
                baseDate = it.key,
                buyHoldYield = ApplicationUtil.getYield(it.value.first().buyHoldRate, it.value.last().buyHoldRate),
                benchmarkYield = ApplicationUtil.getYield(
                    it.value.first().benchmarkRate,
                    it.value.last().benchmarkRate
                ),
                backtestYield = ApplicationUtil.getYield(it.value.first().backtestRate, it.value.last().backtestRate),
            )
        }.toList()
    }

    private fun createReportSummary(workbook: XSSFWorkbook): XSSFSheet {
        val sheet = workbook.createSheet()
        val summary = getSummary()
        ReportMakerHelperService.textToSheet(summary, sheet)
        log.info(summary)

        sheet.defaultColumnWidth = 60
        return sheet
    }

    private fun getSummary(): String {
        val report = StringBuilder()
        val compareTotalYield = calculateTotalCompareYield()

        report.append("----------- 전략 결과 -----------\n")
        val totalYield: CommonAnalysisReportResult.TotalYield =
            ReportMakerHelperService.calculateTotalYield(evaluationAmountHistory, backtestCondition.backtestPeriod)
        val winningRate = calculateCoinInvestment()
        val winningRateTotal = getWinningRateTotal(winningRate)
        val backtestSharpeRatio = ApplicationUtil.getSharpeRatio(evaluationAmountHistory.stream().map { it.backtestYield }.toList())
        report.append(String.format("합산 실현 수익\t %,.2f%%", totalYield.yield * 100)).append("\n")
        report.append(String.format("합산 실현 MDD\t %,.2f%%", totalYield.mdd * 100)).append("\n")
        report.append(String.format("합산 매매회수\t %d", winningRateTotal.getTradeCount())).append("\n")
        report.append(String.format("합산 승률\t %,.2f%%", winningRateTotal.getWinRate() * 100)).append("\n")
        report.append(String.format("합산 CAGR\t %,.2f%%", totalYield.getCagr() * 100)).append("\n")
        report.append(String.format("샤프지수\t %,.2f", backtestSharpeRatio)).append("\n")

        val backtestNames = getBacktestNames()

        backtestNames.forEach { backtestName ->
            val winningRateItem = winningRate[backtestName]
            if (winningRateItem == null) {
                log.warn("조건에 해당하는 결과가 없습니다. $backtestName")
                return@forEach
            }
            report.append(String.format("${backtestName}. 실현 수익(수수료제외)\t %,.0f", winningRateItem.invest)).append("\n")
            report.append(String.format("${backtestName}. 수수료\t %,.0f", winningRateItem.fee)).append("\n")
            report.append(String.format("${backtestName}. 매매회수\t %d", winningRateItem.getTradeCount())).append("\n")
            report.append(String.format("${backtestName}. 승률\t %,.2f%%", winningRateItem.getWinRate() * 100)).append("\n")
        }

        report.append("----------- Buy&Hold 결과 -----------\n")
        val buyAndHoldSharpeRatio = ApplicationUtil.getSharpeRatio(evaluationAmountHistory.stream().map { it.buyHoldYield }.toList())
        val stockCodes = getStockCodes()
        val buyAndHoldYieldByCode: Map<StockCode, CommonAnalysisReportResult.YieldMdd> = calculateBenchmarkYield(stockCodes)
        val buyHoldText = ReportMakerHelperService.makeSummaryCompareStock(
            compareTotalYield.buyHoldTotalYield, buyAndHoldSharpeRatio, buyAndHoldYieldByCode
        )
        report.append(buyHoldText)

        report.append("----------- Benchmark 결과 -----------\n")
        val benchmarkSharpeRatio = ApplicationUtil.getSharpeRatio(evaluationAmountHistory.stream().map { it.benchmarkYield }.toList())
        val benchmarkYieldByCode: Map<StockCode, CommonAnalysisReportResult.YieldMdd> =
            calculateBenchmarkYield(setOf(backtestCondition.benchmarkStockCode))
        val benchmarkText = ReportMakerHelperService.makeSummaryCompareStock(
            compareTotalYield.benchmarkTotalYield, benchmarkSharpeRatio, benchmarkYieldByCode
        )
        report.append(benchmarkText)

        report.append("----------- 백테스트 조건 -----------\n")
        val range: DateRange = backtestCondition.backtestPeriod
        report.append(String.format("분석기간\t %s", range)).append("\n")
        report.append(String.format("최초 투자금액\t %,.0f", accountCondition.cash)).append("\n")
        report.append(String.format("매수 수수료\t %,.2f%%", accountCondition.feeBuy * 100)).append("\n")
        report.append(String.format("매도 수수료\t %,.2f%%", accountCondition.feeSell * 100)).append("\n")
        report.append(backtestCondition.specialInfo)
        return report.toString()
    }

    /**
     * <조건이름, 단위 수익 정보>
     */
    private fun calculateCoinInvestment(): Map<String, CommonAnalysisReportResult.WinningRate> {
        val sellList = tradeResult.filter { it.tradeType == TradeType.SELL }.toList()
        val groupBy = sellList.groupBy { it.backtestConditionName }

        // <조건이름, 수수료합>
        val feeMap = tradeResult.groupBy { it.backtestConditionName }.entries.associate { entity ->
            entity.key to entity.value.sumOf { it -> it.feePrice }
        }

        return groupBy.entries.associate { entity ->
            val totalInvest = entity.value.sumOf { it.gains }
            val gainCount = entity.value.count { it.gains > 0 }
            val winningRate = CommonAnalysisReportResult.WinningRate(
                gainCount,
                entity.value.size - gainCount,
                totalInvest,
                feeMap[entity.key]!!
            )
            entity.key to winningRate
        }
    }


    /**
     * @return <종목 아이디, 투자 종목에 대한 Buy & Hold시 수익 정보>
     */
    private fun calculateBenchmarkYield(stockCodes: Collection<StockCode>): Map<StockCode, CommonAnalysisReportResult.YieldMdd> {
        return stockCodes.associateWith { stockCode ->
            val closePriceHistory = stockClosePriceHistory[stockCode]

            // closePriceHistory key 값 오름차순 값 리스트로 가져옴
            val priceHistory = closePriceHistory?.entries?.sortedBy { it.key }?.map { it.value }?.toMutableList()

            val yieldMdd = CommonAnalysisReportResult.YieldMdd(
                ApplicationUtil.getYield(priceHistory!!),
                ApplicationUtil.getMdd(priceHistory)
            )
            yieldMdd
        }
    }

    private fun getWinningRateTotal(winningRateTarget: Map<String, CommonAnalysisReportResult.WinningRate>): CommonAnalysisReportResult.WinningRate {
        return CommonAnalysisReportResult.WinningRate(
            gainCount = winningRateTarget.values.sumOf { it.gainCount },
            lossCount = winningRateTarget.values.sumOf { it.lossCount },
            invest = winningRateTarget.values.sumOf { it.invest },
            fee = winningRateTarget.values.sumOf { it.fee },
        )
    }

    /**
     * 전체 투자 종목에 대한 수익 정보
     * @return <buyAndHold 종목 수익, 밴치마크 종목 수익>
     */
    private fun calculateTotalCompareYield(): CompareTotalYield {
        val buyHold = evaluationAmountHistory.map { it.buyHoldRate }.toList()
        val buyHoldTotalYield = CommonAnalysisReportResult.TotalYield(
            ApplicationUtil.getYield(buyHold),
            ApplicationUtil.getMdd(buyHold),
            backtestCondition.backtestPeriod.diffDays.toInt()
        )
        val benchmark = evaluationAmountHistory.map { it.benchmarkRate }.toList()
        val benchmarkTotalYield = CommonAnalysisReportResult.TotalYield(
            ApplicationUtil.getYield(benchmark),
            ApplicationUtil.getMdd(benchmark),
            backtestCondition.backtestPeriod.diffDays.toInt()
        )
        return CompareTotalYield(buyHoldTotalYield, benchmarkTotalYield)
    }


    // 초기 현금, 매매 수수료 정보
    data class AccountCondition(
        /**  투자금액 */
        val cash: Double,
        /** 수 수수료 */
        val feeBuy: Double,
        /** 도 수수료 */
        val feeSell: Double,
    )

    // 백테스트 조건
    data class BacktestCondition(
        val backtestPeriod: DateRange,
        val benchmarkStockCode: StockCode,
        /**
         * 백테스트에 대한 추가적인 조건을 넣는다.
         * 보통 '항목'과 '값'을 나타낸다. 구분값을 탭과 개행으로 구분한다.
         * ex)
         * 최대 보유 종목 수\t10
         * 최대 보유 일수\t30
         * 분석주기\t1MONTH
         */
        val specialInfo: String? = null
    )
}
