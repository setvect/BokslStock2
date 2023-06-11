package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.model.*
import com.setvect.bokslstock2.analysis.common.util.StockByDateCandle
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.deepCopyWithSerialization
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
    private var tradeResult = mutableListOf<TradeResult>()

    /**
     * @return 매매 내역 바탕으로 건별 매매 결과 목록
     */
    fun calculateTradeResult(): List<TradeResult> {
        tradeResult.clear()
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
                    stockAccount = averagePriceMap.deepCopyWithSerialization(),
                    memo = tradeNeo.memo
                )
            )
        }
        return tradeResult
    }

    /**
     * @return 특정 기간을 기준으로 가장 최근 매매 상태
     */
    private fun getNearTrade(date: LocalDate): TradeResult {
        return tradeResult.last { it.tradeDate <= date.atTime(LocalTime.MAX) }
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

//            val evaluationAmountHistory = evaluationRateItems()
//            sheet = ReportMakerHelperService.createReportEvalAmount(evaluationAmountHistory, workbook)
//            workbook.setSheetName(workbook.getSheetIndex(sheet), "2. 일자별 자산변화")
        }
    }

    /**
     * [backtestPeriod] 백테스트 기간
     * [holdStockCodes] 보유 종목 - 동일비중으로 계산
     * [benchmark] 밴치마크
     * @return 매매 전략, 동일 비중 종목 매매, 밴치마크 각 일자별 수익률
     */
    private fun evaluationRateItems(backtestPeriod: DateRange, holdStockCodes: Set<StockCode>, benchmark: StockCode): List<EvaluationRateItem> {
        val evaluationAmountHistory = listOf<EvaluationRateItem>()

        val stockCodes = getStockCodes() union holdStockCodes union setOf(benchmark)
        val stockByDateCandle: StockByDateCandle = stockCommonFactory.createStockByDateCandle(stockCodes, backtestPeriod)
        // 마지막 종가 가격
        var closePriceByStockCode = mutableMapOf<StockCode, Double>()


        var buyHoldLastRate = 1.0
        var benchmarkLastRate = 1.0
        var backtestLastRate = 1.0
        // 백테스트 현금 기록
        var backtestLastCash = accountCondition.cash

        // 시작 날짜부터 하루씩 증가 시켜 종료날짜 까지 루프
        var date = backtestPeriod.from
        while (date <= backtestPeriod.to) {
            val currentCandle = stockCodes.associateWith { stockCode ->
                val candle = stockByDateCandle.getCandle(stockCode, date.toLocalDate())
                candle
            }

            // 마지막 종가 계산
            currentCandle.filter { it.value != null }
                .forEach {
                    closePriceByStockCode[it.key] = it.value!!.closePrice
                }

//            여기 작업해

            date = date.plusDays(1)
        }
        return evaluationAmountHistory
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
