package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.model.*
import com.setvect.bokslstock2.analysis.common.util.StockByDateCandle
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.deepCopyWithSerialization
import okhttp3.internal.toImmutableList
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime

/**
 * 매매 계좌
 */
class AccountService(
    private val stockCommonFactory: StockCommonFactory,
    private val accountCondition: AccountCondition
) {

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
                    memo = tradeNeo.memo
                )
            )
        }
        return tradeResult.toImmutableList()
    }

    fun calcEvaluationRate(backtestPeriod: DateRange, benchmarkStockCode: StockCode): List<EvaluationRateItem> {
        evaluationAmountHistory = evaluationRateItems(backtestPeriod, getStockCodes(), benchmarkStockCode)
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
    fun getStockCodes(): Set<StockCode> {
        return tradeHistory.map { it.stockCode }.toSet()
    }

    fun addTrade(tradeNeo: TradeNeo) {
        tradeHistory.add(tradeNeo)
    }

    /**
     * [backtestPeriod] 백테스트 기간
     * [holdStockCodes] 보유 종목 - 동일비중으로 계산
     * [benchmarkStockCode] 밴치마크
     * @return 매매 전략, 동일 비중 종목 매매, 밴치마크 각 일자별 수익률
     */
    private fun evaluationRateItems(
        backtestPeriod: DateRange,
        holdStockCodes: Set<StockCode>,
        benchmarkStockCode: StockCode
    ): List<EvaluationRateItem> {
        val evaluationAmountHistory = mutableListOf<EvaluationRateItem>()

        val stockCodes = getStockCodes() union holdStockCodes union setOf(benchmarkStockCode)

        val stockClosePriceHistory = getClosePriceByStockCodes(stockCodes, backtestPeriod)

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
                benchmarkLastRate = currentClosePrice / beforeClosePrice
            }
            val benchmarkYield = ApplicationUtil.getYield(benchmarkBeforeRate, benchmarkLastRate)

            // 2. buy & hold 평가금액 비율
            val buyHoldBeforeRate = buyHoldLastRate
            //  holdStockCodes 종목의 직전 종가와 현재 종가의 비율을 계산하여 평균을 구함
            buyHoldLastRate = holdStockCodes.map {
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
        XSSFWorkbook().use { workbook ->
            var sheet = ReportMakerHelperService.createTradeReport(tradeResult, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "1. 매매이력")
            FileOutputStream(reportFile).use { ous ->
                workbook.write(ous)
            }

            sheet = ReportMakerHelperService.createReportEvalAmount(evaluationAmountHistory, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "2. 일자별 자산변화")
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

    // 초기 현금, 매매 수수료 정보
    data class AccountCondition(
        /**  투자금액 */
        val cash: Double,
        /** 수 수수료 */
        val feeBuy: Double,
        /** 도 수수료 */
        val feeSell: Double,
    )


}
