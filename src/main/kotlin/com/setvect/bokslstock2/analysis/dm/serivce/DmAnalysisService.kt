package com.setvect.bokslstock2.analysis.dm.serivce

import com.setvect.bokslstock2.analysis.common.model.AnalysisResult
import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.analysis.common.model.PreTrade
import com.setvect.bokslstock2.analysis.common.model.TradeType
import com.setvect.bokslstock2.analysis.common.service.BacktestTradeService
import com.setvect.bokslstock2.analysis.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.analysis.dm.model.DmBacktestCondition
import com.setvect.bokslstock2.analysis.common.model.Stock
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateUtil
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.util.*
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 듀얼모멘텀 백테스트
 */
@Service
class DmAnalysisService(
    private val stockRepository: StockRepository,
    private val movingAverageService: MovingAverageService,
    private val backtestTradeService: BacktestTradeService,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun runTest(dmBacktestCondition: DmBacktestCondition) {
        checkValidate(dmBacktestCondition)
        val preTrades = processDualMomentum(dmBacktestCondition)
        var sumYield = 1.0
        preTrades.forEach {
            log.info("${it.tradeType}\t${it.tradeDate}\t${it.stock.name}(${it.stock.code})\t${it.yield}")
            sumYield *= (it.yield + 1)
        }
        log.info("수익률: ${String.format("%.2f%%", (sumYield - 1) * 100)}")

        val trades = backtestTradeService.trade(dmBacktestCondition.tradeCondition, preTrades)
        val result = backtestTradeService.analysis(trades, dmBacktestCondition.tradeCondition, dmBacktestCondition.listStock())
        val summary = getSummary(dmBacktestCondition, result.common)
        println(summary)
        makeReportFile(dmBacktestCondition, result)
    }

    private fun processDualMomentum(condition: DmBacktestCondition): List<PreTrade> {
        val stockCodes = condition.listStock()
        // <종목코드, 종목정보>
        val codeByStock = stockCodes.associateWith { stockRepository.findByCode(it).get() }

        // <종목코드, <날짜, 캔들>>
        val stockPriceIndex = getStockPriceIndex(stockCodes, condition)

        var current =
            DateUtil.fitMonth(condition.tradeCondition.range.from.withDayOfMonth(1), condition.periodType.getDeviceMonth())

        var beforeBuyTrade: PreTrade? = null

        val tradeList = mutableListOf<PreTrade>()
        while (current.isBefore(condition.tradeCondition.range.to)) {
            val stockByRate = calculateRate(stockPriceIndex, current, condition)

            val existBeforeBuy = beforeBuyTrade != null

            // 듀얼 모멘텀 매수 대상 종목이 없으면
            if (stockByRate.isEmpty()) {
                val changeBuyStock = beforeBuyTrade != null && beforeBuyTrade.stock.code != condition.holdCode
                val existHoldCode = condition.holdCode != null

                if (existBeforeBuy && changeBuyStock) {
                    val stockPrice = stockPriceIndex[beforeBuyTrade!!.stock.code]!![current]!!
                    val sellTrade = makeSellTrade(stockPrice, current, beforeBuyTrade)
                    tradeList.add(sellTrade)
                    beforeBuyTrade = null
                }
                if (existHoldCode && (beforeBuyTrade == null || beforeBuyTrade.stock.code != condition.holdCode)) {
                    val stockPrice = stockPriceIndex[condition.holdCode]!![current]!!
                    val stock = codeByStock[condition.holdCode]!!
                    val buyTrade = makeBuyTrade(stockPrice, current, stock)
                    tradeList.add(buyTrade)
                    beforeBuyTrade = buyTrade
                } else if (existHoldCode) {
                    log.info("매수 유지: $current, ${getStockName(codeByStock, condition.holdCode!!)}(${condition.holdCode})")
                }
            } else {
                val buyStockRate = stockByRate[0]
                val stockCode = buyStockRate.first
                val changeBuyStock = beforeBuyTrade != null && beforeBuyTrade.stock.code != stockCode

                if (existBeforeBuy && changeBuyStock) {
                    val stockPrice = stockPriceIndex[beforeBuyTrade!!.stock.code]!![current]!!
                    val sellTrade = makeSellTrade(stockPrice, current, beforeBuyTrade)
                    tradeList.add(sellTrade)
                }
                if (!existBeforeBuy || changeBuyStock) {
                    val stockPrice = stockPriceIndex[stockCode]!![current]!!
                    val stock = codeByStock[stockCode]!!
                    val buyTrade = makeBuyTrade(stockPrice, current, stock)
                    tradeList.add(buyTrade)
                    beforeBuyTrade = buyTrade
                } else {
                    log.info("매수 유지: $current, ${beforeBuyTrade!!.stock.name}(${beforeBuyTrade.stock.code})")
                }
            }

//            stockByRate.forEach {
//                log.info("$current - ${getStockName(codeByStock, it.first)}(${it.first}) : ${it.second}")
//            }

//            if (stockByRate.isEmpty()) {
//                log.info("$current - empty")
//            }
            current = current.plusMonths(condition.periodType.getDeviceMonth().toLong())
        }


        return tradeList
    }

    private fun makeBuyTrade(
        stockPrice: CandleDto,
        current: LocalDateTime,
        stock: StockEntity
    ): PreTrade {
        val buyTrade = PreTrade(
            stock = Stock.of(stock),
            tradeType = TradeType.BUY,
            yield = 0.0,
            unitPrice = stockPrice.openPrice,
            tradeDate = current,
        )
        log.info("매수: ${buyTrade.tradeDate}, ${buyTrade.stock.name}(${buyTrade.stock.code})")
        return buyTrade
    }


    private fun makeSellTrade(
        stockPrice: CandleDto,
        current: LocalDateTime,
        beforeBuyTrade: PreTrade
    ): PreTrade {
        val sellTrade = PreTrade(
            stock = beforeBuyTrade.stock,
            tradeType = TradeType.SELL,
            yield = ApplicationUtil.getYield(beforeBuyTrade.unitPrice, stockPrice.openPrice),
            unitPrice = stockPrice.openPrice,
            tradeDate = current,
        )
        log.info("매도: ${sellTrade.tradeDate}, ${sellTrade.stock.name}(${sellTrade.stock.code}), 수익: ${sellTrade.yield}")
        return sellTrade
    }

    /**
     * 듀얼 모멘터 대상 종목을 구함
     * [stockPriceIndex] <종목코드, <날짜, 캔들>>
     * @return <종목코드, 현재가격/모멘텀평균 가격>
     */
    private fun calculateRate(
        stockPriceIndex: Map<String, Map<LocalDateTime, CandleDto>>,
        current: LocalDateTime,
        condition: DmBacktestCondition,
    ): List<Pair<String, Double>> {
        val stockByRate = stockPriceIndex.entries.map { stockEntry ->
            val currentCandle = stockEntry.value[current]
                ?: throw RuntimeException("${stockEntry.key}에 대한 $current 시세가 없습니다.")

            val average = condition.timeWeight.entries.sumOf { timeWeight ->
                val delta = timeWeight.key
                val weight = timeWeight.value
                val deltaCandle = stockEntry.value[current.minusMonths(delta.toLong())]!!
                deltaCandle.closePrice * weight
            }

            val rate = currentCandle.openPrice / average
            stockEntry.key to rate
        }
            .filter { it.second >= 1 && it.first != condition.holdCode }
            .sortedByDescending { it.second }
        return stockByRate
    }

    private fun getStockName(codeByStock: Map<String, StockEntity>, code: String): String {
        return codeByStock[code]!!.name
    }

    /**
     * @return <종목코드, <날짜, 캔들>>
     */
    private fun getStockPriceIndex(
        stockCodes: List<String>,
        dmCondition: DmBacktestCondition
    ): Map<String, Map<LocalDateTime, CandleDto>> {
        val stockPriceIndex = stockCodes.associateWith { code ->
            movingAverageService.getMovingAverage(
                code,
                dmCondition.periodType,
                Collections.emptyList()
            )
                .associateBy { it.candleDateTimeStart.withDayOfMonth(1) }
        }
        return stockPriceIndex
    }

    private fun checkValidate(dmCondition: DmBacktestCondition) {
        val sumWeight = dmCondition.timeWeight.entries.sumOf { it.value }
        if (sumWeight != 1.0) {
            throw RuntimeException("가중치의 합계가 100이여 합니다. 현재 가중치 합계: $sumWeight")
        }
    }

    /**
     * 분석건에 대한 리포트 파일 만듦
     * @return 엑셀 파일
     */
    private fun makeReportFile(dmBacktestCondition: DmBacktestCondition, analysisResult: AnalysisResult): File {
        val reportFileSubPrefix =
            ReportMakerHelperService.getReportFileSuffix(dmBacktestCondition.tradeCondition, dmBacktestCondition.listStock())
        val reportFile = File("./backtest-result/dm-trade-report", "dm_trade_$reportFileSubPrefix")

        XSSFWorkbook().use { workbook ->
            var sheet = ReportMakerHelperService.createTradeReport(analysisResult, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "1. 매매이력")

            sheet = ReportMakerHelperService.createReportEvalAmount(analysisResult.common.evaluationAmountHistory, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "2. 일짜별 자산비율 변화")

            sheet = ReportMakerHelperService.createReportRangeReturn(analysisResult.common.getMonthlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "3. 월별 수익률")

            sheet = ReportMakerHelperService.createReportRangeReturn(analysisResult.common.getYearlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "4. 년별 수익률")

            sheet = createReportSummary(dmBacktestCondition, analysisResult, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "5. 매매 요약결과 및 조건")

            FileOutputStream(reportFile).use { ous ->
                workbook.write(ous)
            }
        }
        println("결과 파일:" + reportFile.name)
        return reportFile
    }


    /**
     * 매매 결과 요약 및 조건 시트 만듦
     */
    private fun createReportSummary(
        dmBacktestCondition: DmBacktestCondition,
        analysisResult: AnalysisResult,
        workbook: XSSFWorkbook
    ): XSSFSheet {
        val sheet = workbook.createSheet()
        val summary = getSummary(dmBacktestCondition, analysisResult.common)
        ReportMakerHelperService.textToSheet(summary, sheet)
        sheet.defaultColumnWidth = 60
        return sheet
    }

    /**
     * 분석 요약결과
     */
    private fun getSummary(dmBacktestCondition: DmBacktestCondition, commonAnalysisReportResult: CommonAnalysisReportResult): String {
        val report = StringBuilder()

        report.append("----------- Buy&Hold 결과 -----------\n")
        report.append(String.format("합산 동일비중 수익\t %,.2f%%", commonAnalysisReportResult.buyHoldYieldTotal.yield * 100))
            .append("\n")
        report.append(String.format("합산 동일비중 MDD\t %,.2f%%", commonAnalysisReportResult.buyHoldYieldTotal.mdd * 100)).append("\n")
        report.append(String.format("합산 동일비중 CAGR\t %,.2f%%", commonAnalysisReportResult.buyHoldYieldTotal.getCagr() * 100))
            .append("\n")
        report.append(String.format("샤프지수\t %,.2f", commonAnalysisReportResult.getBuyHoldSharpeRatio())).append("\n")


        val totalYield: CommonAnalysisReportResult.TotalYield = commonAnalysisReportResult.yieldTotal
        report.append("----------- 전략 결과 -----------\n")
        report.append(String.format("합산 실현 수익\t %,.2f%%", totalYield.yield * 100)).append("\n")
        report.append(String.format("합산 실현 MDD\t %,.2f%%", totalYield.mdd * 100)).append("\n")
        report.append(String.format("합산 매매회수\t %d", commonAnalysisReportResult.getWinningRateTotal().getTradeCount())).append("\n")
        report.append(String.format("합산 승률\t %,.2f%%", commonAnalysisReportResult.getWinningRateTotal().getWinRate() * 100))
            .append("\n")
        report.append(String.format("합산 CAGR\t %,.2f%%", totalYield.getCagr() * 100)).append("\n")
        report.append(String.format("샤프지수\t %,.2f", commonAnalysisReportResult.getBacktestSharpeRatio())).append("\n")

        report.append("----------- 테스트 조건 -----------\n")
        val stockName = dmBacktestCondition.stockCodes.joinToString(", ") { code ->
            stockRepository
                .findByCode(code)
                .map { "${it.code}[${it.name}]" }
                .orElse("")
        }
        val holdStockName = Optional.ofNullable(dmBacktestCondition.holdCode).map { code ->
            stockRepository
                .findByCode(code)
                .map { "${it.code}[${it.name}]" }
                .orElse("")
        }.orElse("")
        val timeWeight = dmBacktestCondition.timeWeight.entries
            .sortedBy { it.key }
            .joinToString(", ") { "${it.key}월:${String.format("%.2f%%", it.value * 100)}" }

        val tradeCondition = dmBacktestCondition.tradeCondition
        report.append("모멘텀 대상종목\t${stockName}").append("\n")
        report.append("홀드 종목\t$holdStockName").append("\n")
        report.append("거래주기\t${dmBacktestCondition.periodType}").append("\n")
        report.append("기간별 가중치\t$timeWeight").append("\n")
        report.append("분석 대상 기간\t${tradeCondition.range}").append("\n")
        report.append("투자 비율\t${String.format("%,.2f%%", tradeCondition.investRatio * 100)}").append("\n")
        report.append("최초 투자금액\t${String.format("%,.0f", tradeCondition.cash)}").append("\n")
        report.append("매수 수수료\t${String.format("%,.2f%%", tradeCondition.feeBuy * 100)}").append("\n")
        report.append("매도 수수료\t${String.format("%,.2f%%", tradeCondition.feeSell * 100)}").append("\n")
        report.append("설명\t${tradeCondition.comment}").append("\n")

        return report.toString()
    }
}