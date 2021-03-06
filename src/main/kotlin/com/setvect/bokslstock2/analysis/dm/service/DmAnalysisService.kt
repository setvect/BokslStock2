package com.setvect.bokslstock2.analysis.dm.service

import com.setvect.bokslstock2.analysis.common.model.AnalysisResult
import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.analysis.common.model.PreTrade
import com.setvect.bokslstock2.analysis.common.model.Stock
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.common.model.TradeType
import com.setvect.bokslstock2.analysis.common.service.BacktestTradeService
import com.setvect.bokslstock2.analysis.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.analysis.dm.model.DmBacktestCondition
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.DateUtil
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
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
        val tradeCondition = makeTradeDateCorrection(dmBacktestCondition, preTrades)
        val trades = backtestTradeService.trade(tradeCondition, preTrades)
        val result = backtestTradeService.analysis(trades, tradeCondition, dmBacktestCondition.stockCodes)
        val summary = getSummary(dmBacktestCondition, result.common)
        println(summary)
        makeReportFile(dmBacktestCondition, result)
    }

    /**
     * 나도 이렇게 하기 싫다.
     * 비주얼포트폴리오 매매 전략과 동일하게 맞추기 위해서 직전 종가 기준으로 매매가 이루어져야 되기 때문에 백테스트 시작 시점 조정이 필요하다.
     */
    private fun makeTradeDateCorrection(
        dmBacktestCondition: DmBacktestCondition,
        preTrades: List<PreTrade>
    ): TradeCondition {
        val temp = dmBacktestCondition.tradeCondition
        val from = if (temp.range.from.isBefore(preTrades.first().tradeDate)) temp.range.from else preTrades.first().tradeDate
        val to = if (temp.range.to.isAfter(preTrades.last().tradeDate)) temp.range.to else preTrades.last().tradeDate
        return TradeCondition(DateRange(from, to), temp.investRatio, temp.cash, temp.feeBuy, temp.feeSell, temp.comment)
    }

    private fun processDualMomentum(condition: DmBacktestCondition): List<PreTrade> {
        val stockCodes = condition.listStock()
        // <종목코드, 종목정보>
        val codeByStock = stockCodes.associateWith { stockRepository.findByCode(it).get() }

        // <종목코드, <날짜, 캔들>>
        val stockPriceIndex = getStockPriceIndex(stockCodes, condition)
        // 듀얼모멘텀 대상 종목 <종목코드, <날짜, 캔들>>
        val stockPriceIndexForMomentumStock =
            stockPriceIndex.entries.filter { it.key != condition.holdCode }.associate { it.key to it.value }

        var current =
            DateUtil.fitMonth(condition.tradeCondition.range.from.withDayOfMonth(1), condition.periodType.getDeviceMonth())

        var beforeBuyTrade: PreTrade? = null

        val tradeList = mutableListOf<PreTrade>()
        while (current.isBefore(condition.tradeCondition.range.to)) {
            val stockByRate = calculateRate(stockPriceIndexForMomentumStock, current, condition)

            // 듀얼 모멘텀 매수 대상 종목이 없으면, hold 종목 매수 또는 현금 보유
            if (stockByRate.isEmpty()) {
                val changeBuyStock = beforeBuyTrade != null && beforeBuyTrade.stock.code != condition.holdCode
                val existHoldCode = condition.holdCode != null

                if (changeBuyStock) {
                    // 보유 종목 매도
                    val sellStock = stockPriceIndex[beforeBuyTrade!!.stock.code]!![current]!!
                    val sellTrade = makeSellTrade(sellStock, beforeBuyTrade)
                    tradeList.add(sellTrade)
                    beforeBuyTrade = null
                }
                if (existHoldCode && (beforeBuyTrade == null || beforeBuyTrade.stock.code != condition.holdCode)) {
                    // hold 종목 매수
                    val buyStock = stockPriceIndex[condition.holdCode]!![current]!!
                    val stock = codeByStock[condition.holdCode]!!
                    val buyTrade = makeBuyTrade(buyStock, stock)
                    tradeList.add(buyTrade)
                    beforeBuyTrade = buyTrade
                } else if (existHoldCode) {
                    log.info("매수 유지: $current, ${getStockName(codeByStock, condition.holdCode!!)}(${condition.holdCode})")
                }
            } else {
                val buyStockRate = stockByRate[0]
                val stockCode = buyStockRate.first
                val changeBuyStock = beforeBuyTrade != null && beforeBuyTrade.stock.code != stockCode

                if (changeBuyStock) {
                    // 보유 종목 매도
                    val sellStock = stockPriceIndex[beforeBuyTrade!!.stock.code]!![current]!!
                    val sellTrade = makeSellTrade(sellStock, beforeBuyTrade)
                    tradeList.add(sellTrade)
                }
                if (beforeBuyTrade == null || changeBuyStock) {
                    // 새운 종목 매수
                    val buyStock = stockPriceIndex[stockCode]!![current]!!
                    val stock = codeByStock[stockCode]!!
                    val buyTrade = makeBuyTrade(buyStock, stock)
                    tradeList.add(buyTrade)
                    beforeBuyTrade = buyTrade
                } else {
                    log.info("매수 유지: $current, ${beforeBuyTrade.stock.name}(${beforeBuyTrade.stock.code})")
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

        // 마지막 보유 종목 매도
        if (condition.endSell && beforeBuyTrade != null) {
            val sellStock = stockPriceIndex[beforeBuyTrade.stock.code]!![current]!!
            val sellTrade = makeSellTrade(sellStock, beforeBuyTrade)
            tradeList.add(sellTrade)
        }

        return tradeList
    }

    private fun makeBuyTrade(
        targetStock: CandleDto,
        stock: StockEntity
    ): PreTrade {
        val buyTrade = PreTrade(
            stock = Stock.of(stock),
            tradeType = TradeType.BUY,
            yield = 0.0,
            unitPrice = targetStock.openPrice,
            tradeDate = targetStock.candleDateTimeStart,
        )
        log.info("매수: ${targetStock.candleDateTimeStart}(${buyTrade.tradeDate}), ${buyTrade.stock.name}(${buyTrade.stock.code})")
        return buyTrade
    }


    private fun makeSellTrade(
        targetStock: CandleDto,
        beforeBuyTrade: PreTrade
    ): PreTrade {
        val sellTrade = PreTrade(
            stock = beforeBuyTrade.stock,
            tradeType = TradeType.SELL,
            yield = ApplicationUtil.getYield(beforeBuyTrade.unitPrice, targetStock.openPrice),
            unitPrice = targetStock.openPrice,
            tradeDate = targetStock.candleDateTimeStart,
        )
        log.info("매도: ${targetStock.candleDateTimeStart}(${sellTrade.tradeDate}), ${sellTrade.stock.name}(${sellTrade.stock.code}), 수익: ${sellTrade.yield}")
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

            log.info(
                "\t현재 날짜: ${current}: ${stockEntry.key}: ${currentCandle.candleDateTimeStart}~${currentCandle.candleDateTimeEnd} - " +
                        "O: ${currentCandle.openPrice}, H: ${currentCandle.highPrice}, L: ${currentCandle.lowPrice}, C:${currentCandle.closePrice}, ${currentCandle.periodType}"
            )

            // 모멘텀평균 가격(가중치 적용 종가 평균)
            val average = condition.timeWeight.entries.sumOf { timeWeight ->
                val delta = timeWeight.key
                val weight = timeWeight.value
                val deltaCandle = stockEntry.value[current.minusMonths(delta.toLong())]!!
                log.info("\t\t비교 날짜: [${delta}] ${stockEntry.key} - ${deltaCandle.candleDateTimeStart} - C: ${deltaCandle.closePrice}")
                log.info(
                    "\t\t$delta -   ${stockEntry.key}: ${deltaCandle.candleDateTimeStart}~${deltaCandle.candleDateTimeEnd} - " +
                            "직전종가: ${deltaCandle.beforeClosePrice}, O: ${deltaCandle.openPrice}, H: ${deltaCandle.highPrice}, L: ${deltaCandle.lowPrice}, C:${deltaCandle.closePrice}, ${deltaCandle.periodType}, 수익률: ${deltaCandle.getYield()}"
                )

                deltaCandle.openPrice * weight
            }

            val rate = currentCandle.beforeClosePrice / average
            // 수익률 = 현재 날짜 시가 / 모멘텀평균 가격
            log.info("\t수익률: ${current}: ${stockEntry.key} = ${currentCandle.openPrice}/$average = $rate")

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
        val append = "_${dmBacktestCondition.timeWeight.entries.map { it.key }.joinToString(",")}"
        val reportFileSubPrefix =
            ReportMakerHelperService.getReportFileSuffix(dmBacktestCondition.tradeCondition, dmBacktestCondition.listStock(), append)
        val reportFile = File(
            "./backtest-result/dm-trade-report",
            "dm_trade_${reportFileSubPrefix}"
        )

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

    fun makeSummaryReport(conditionList: List<DmBacktestCondition>): File {
        var i = 0
        val conditionResults = conditionList.map { dmBacktestCondition ->
            checkValidate(dmBacktestCondition)
            val preTrades = processDualMomentum(dmBacktestCondition)
            val tradeCondition = makeTradeDateCorrection(dmBacktestCondition, preTrades)
            val trades = backtestTradeService.trade(tradeCondition, preTrades)
            val analysisResult = backtestTradeService.analysis(trades, tradeCondition, dmBacktestCondition.stockCodes)
            log.info("분석 진행 ${++i}/${conditionList.size}")
            Pair(dmBacktestCondition, analysisResult)
        }.toList()

        for (idx in conditionResults.indices) {
            val conditionResult = conditionResults[idx]
            makeReportFile(conditionResult.first, conditionResult.second)
            log.info("개별분석파일 생성 ${idx + 1}/${conditionList.size}")
        }

        // 결과 저장
        val reportFile =
            File("./backtest-result", "듀얼모멘텀_전략_백테스트_분석결과_" + Timestamp.valueOf(LocalDateTime.now()).time + ".xlsx")
        XSSFWorkbook().use { workbook ->
            val sheetBacktestSummary = createTotalSummary(workbook, conditionResults)
            workbook.setSheetName(workbook.getSheetIndex(sheetBacktestSummary), "1. 평가표")

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


    /**
     * @return 여러개 백테스트 결과 요약 시트
     */
    private fun createTotalSummary(
        workbook: XSSFWorkbook,
        conditionResults: List<Pair<DmBacktestCondition, AnalysisResult>>
    ): XSSFSheet {
        val sheet = workbook.createSheet()

        val header = "분석기간,거래종목,홀드종목,가중치기간 및 비율,투자비율,최초 투자금액,매수 수수료,매도 수수료," +
                "조건 설명," +
                "매수 후 보유 수익,매수 후 보유 MDD,매수 후 보유 CAGR,샤프지수," +
                "실현 수익,실현 MDD,실현 CAGR,샤프지수,매매 횟수,승률"
        ReportMakerHelperService.applyHeader(sheet, header)
        var rowIdx = 1

        val defaultStyle = ReportMakerHelperService.ExcelStyle.createDefault(workbook)
        val dateStyle = ReportMakerHelperService.ExcelStyle.createDate(workbook)
        val commaStyle = ReportMakerHelperService.ExcelStyle.createComma(workbook)
        val percentStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        val decimalStyle = ReportMakerHelperService.ExcelStyle.createDecimal(workbook)
        val percentImportantStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        percentImportantStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        percentImportantStyle.fillForegroundColor = IndexedColors.LEMON_CHIFFON.index


        conditionResults.forEach { conditionResult ->
            val multiCondition = conditionResult.first

            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0
            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.tradeCondition.range.toString())
            createCell.cellStyle = dateStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.stockCodes.joinToString(","))
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.holdCode)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            val timeWeight = multiCondition.timeWeight.entries.map { "${it.key}월: ${it.value * 100}%" }.joinToString(", ")
            createCell.setCellValue(timeWeight)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.tradeCondition.investRatio)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.tradeCondition.cash)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.tradeCondition.feeBuy)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.tradeCondition.feeSell)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.tradeCondition.comment)
            createCell.cellStyle = defaultStyle

            val result = conditionResult.second

            val sumYield: CommonAnalysisReportResult.TotalYield = result.common.buyHoldYieldTotal
            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(sumYield.yield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(sumYield.mdd)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(sumYield.getCagr())
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(result.common.getBuyHoldSharpeRatio())
            createCell.cellStyle = decimalStyle

            val totalYield: CommonAnalysisReportResult.TotalYield = result.common.yieldTotal

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(totalYield.yield)
            createCell.cellStyle = percentImportantStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(totalYield.mdd)
            createCell.cellStyle = percentImportantStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(totalYield.getCagr())
            createCell.cellStyle = percentImportantStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(result.common.getBacktestSharpeRatio())
            createCell.cellStyle = decimalStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(result.common.getWinningRateTotal().getTradeCount().toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx)
            createCell.setCellValue(result.common.getWinningRateTotal().getWinRate())
            createCell.cellStyle = percentStyle
        }
        sheet.createFreezePane(0, 1)
        sheet.defaultColumnWidth = 14
        sheet.setColumnWidth(0, 12000)
        sheet.setColumnWidth(1, 5000)
        sheet.setColumnWidth(2, 5000)

        ReportMakerHelperService.ExcelStyle.applyAllBorder(sheet)
        ReportMakerHelperService.ExcelStyle.applyDefaultFont(sheet)

        return sheet
    }

}