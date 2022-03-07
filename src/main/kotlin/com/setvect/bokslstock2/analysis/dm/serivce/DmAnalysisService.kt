package com.setvect.bokslstock2.analysis.dm.serivce

import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.analysis.common.model.CommonTradeReportItem
import com.setvect.bokslstock2.analysis.common.model.TradeType
import com.setvect.bokslstock2.analysis.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.analysis.common.service.TradeService
import com.setvect.bokslstock2.analysis.dm.model.DmAnalysisCondition
import com.setvect.bokslstock2.analysis.dm.model.DmAnalysisReportResult
import com.setvect.bokslstock2.analysis.dm.model.DmBacktestCondition
import com.setvect.bokslstock2.analysis.dm.model.DmConditionEntity
import com.setvect.bokslstock2.analysis.dm.model.DmTrade
import com.setvect.bokslstock2.analysis.dm.model.DmTradeReportItem
import com.setvect.bokslstock2.common.entity.ConditionEntity
import com.setvect.bokslstock2.common.entity.TradeEntity
import com.setvect.bokslstock2.common.entity.TradeReportItem
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
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
    reportMakerHelperService: ReportMakerHelperService,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)
    val tradeService = TradeService(
        reportMakerHelperService,
        object :
            TradeService.MakerTreadReportItem<DmTrade, DmTradeReportItem> {
            override fun make(tradeEntity: DmTrade, common: CommonTradeReportItem): DmTradeReportItem {
                return DmTradeReportItem(tradeEntity, common)
            }
        },
        object :
            TradeService.MakerAnalysisReportResult<DmAnalysisCondition, DmTradeReportItem, DmAnalysisReportResult> {
            override fun make(
                analysisCondition: DmAnalysisCondition,
                tradeEntity: List<DmTradeReportItem>,
                common: CommonAnalysisReportResult
            ): DmAnalysisReportResult {
                return DmAnalysisReportResult(analysisCondition, tradeEntity, common)
            }
        }
    )


    fun runTest(condition: DmBacktestCondition) {
        checkValidate(condition)
        val tradeList = processDualMomentum(condition)
        var sumYield = 1.0
        tradeList.forEach {
            log.info("${it.tradeType}\t${it.tradeDate}\t${it.stock.name}(${it.stock.code})\t${it.yield}")
            sumYield *= (it.yield + 1)
        }
        log.info("수익률: ${String.format("%.2f%%", (sumYield - 1) * 100)}")

        val dmConditionEntity = DmConditionEntity(
            // TODO 수정해야됨.
            stock = tradeList[0].stock,
            tradeList = tradeList
        )

        val analysisCondition = DmAnalysisCondition(
            tradeConditionList = listOf(dmConditionEntity),
            basic = condition.basic
        )
        val trades = tradeService.trade(analysisCondition)
        println(trades.size)

        val result = tradeService.analysis(trades, analysisCondition)
        makeReportFile(result)
    }

    private fun processDualMomentum(condition: DmBacktestCondition): List<DmTrade> {
        val stockCodes = condition.listStock()
        // <종목코드, 종목정보>
        val codeByStock = stockCodes.associateWith { stockRepository.findByCode(it).get() }

        // <종목코드, <날짜, 캔들>>
        val stockPriceIndex = getStockPriceIndex(stockCodes, condition)

        var current =
            DateUtil.fitMonth(condition.basic.range.from.withDayOfMonth(1), condition.periodType.getDeviceMonth())

        var beforeBuyTrade: DmTrade? = null

        val tradeList = mutableListOf<DmTrade>()
        var tradeSeq = 0L
        while (current.isBefore(condition.basic.range.to)) {
            val stockByRate = calculateRate(stockPriceIndex, current, condition, codeByStock)

            val existBeforeBuy = beforeBuyTrade != null

            // 듀얼 모멘텀 매수 대상 종목이 없으면
            if (stockByRate.isEmpty()) {
                val changeBuyStock = beforeBuyTrade != null && beforeBuyTrade.stock.code != condition.holdCode
                val existHoldCode = condition.holdCode != null

                if (existBeforeBuy && changeBuyStock) {
                    val stockPrice = stockPriceIndex[beforeBuyTrade!!.stock.code]!![current]!!
                    val sellTrade = makeSellTrade(stockPrice, current, ++tradeSeq, beforeBuyTrade)
                    tradeList.add(sellTrade)
                    beforeBuyTrade = null
                }
                if (existHoldCode && (beforeBuyTrade == null || beforeBuyTrade.stock.code != condition.holdCode)) {
                    val stockPrice = stockPriceIndex[condition.holdCode]!![current]!!
                    val stock = codeByStock[condition.holdCode]!!
                    val buyTrade = makeBuyTrade(stockPrice, current, ++tradeSeq, stock)
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
                    val sellTrade = makeSellTrade(stockPrice, current, ++tradeSeq, beforeBuyTrade)
                    tradeList.add(sellTrade)
                }
                if (!existBeforeBuy || changeBuyStock) {
                    val stockPrice = stockPriceIndex[stockCode]!![current]!!
                    val stock = codeByStock[stockCode]!!
                    val buyTrade = makeBuyTrade(stockPrice, current, ++tradeSeq, stock)
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

        tradeList.map { trade ->
            object : TradeReportItem {
                override val tradeEntity: TradeEntity
                    get() = trade
                override val common: CommonTradeReportItem
                    get() = TODO("Not yet implemented")

                override fun getBuyAmount(): Double {
                    return 100.0
                }
            }
        }


        return tradeList
    }

    private fun makeBuyTrade(
        stockPrice: CandleDto,
        current: LocalDateTime,
        tradeSeq: Long,
        stock: StockEntity
    ): DmTrade {
        val buyTrade = DmTrade(
            stock = stock,
            tradeType = TradeType.BUY,
            yield = 0.0,
            unitPrice = stockPrice.openPrice,
            tradeDate = current,
            tradeId = tradeSeq,
            condition = object : ConditionEntity {
                override fun getConditionId(): Long {
                    return 0
                }

                override val stock: StockEntity
                    get() = stock
                override val tradeList: List<TradeEntity>
                    get() = emptyList()
            }
        )
        log.info("매수: ${buyTrade.tradeDate}, ${buyTrade.stock.name}(${buyTrade.stock.code})")
        return buyTrade
    }


    private fun makeSellTrade(
        stockPrice: CandleDto,
        current: LocalDateTime,
        tradeSeq: Long,
        beforeBuyTrade: DmTrade
    ): DmTrade {
        val sellTrade = DmTrade(
            stock = beforeBuyTrade.stock,
            tradeType = TradeType.SELL,
            yield = ApplicationUtil.getYield(beforeBuyTrade.unitPrice, stockPrice.openPrice),
            unitPrice = stockPrice.openPrice,
            tradeDate = current,
            tradeId = tradeSeq,
            condition = object : ConditionEntity {
                override fun getConditionId(): Long {
                    return 0
                }

                override val stock: StockEntity
                    get() = beforeBuyTrade.stock
                override val tradeList: List<TradeEntity>
                    get() = emptyList()
            }
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
        codeByStock: Map<String, StockEntity>
    ): List<Pair<String, Double>> {
        val stockByRate = stockPriceIndex.entries.map { stockEntry ->
            val stock = codeByStock[stockEntry.key]!!

            val currentCandle = stockEntry.value[current]
                ?: throw RuntimeException("${stockEntry.key}에 대한 $current 시세가 없습니다.")

//            log.info(
//                "\t${current}: ${stock.name}(${stock.code}): ${currentCandle.candleDateTimeStart}~${currentCandle.candleDateTimeEnd} - " +
//                        "O: ${currentCandle.openPrice}, H: ${currentCandle.highPrice}, L: ${currentCandle.lowPrice}, C:${currentCandle.closePrice}, ${currentCandle.periodType}"
//            )

            val average = condition.timeWeight.entries.sumOf { timeWeight ->
                val delta = timeWeight.key
                val weight = timeWeight.value
                val deltaCandle = stockEntry.value[current.minusMonths(delta.toLong())]!!
//                log.info("\t\t[${delta}] ${stock.code} - ${deltaCandle.candleDateTimeStart} - C: ${deltaCandle.closePrice}")
//                log.info(
//                    "\t\t$delta -   ${stock.name}(${stock.code}): ${deltaCandle.candleDateTimeStart}~${deltaCandle.candleDateTimeEnd} - " +
//                            "O: ${deltaCandle.openPrice}, H: ${deltaCandle.highPrice}, L: ${deltaCandle.lowPrice}, C:${deltaCandle.closePrice}, ${deltaCandle.periodType}"
//                )

                deltaCandle.closePrice * weight
            }

            val rate = currentCandle.openPrice / average
//            log.info("\t${average}: \t${rate}")
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
     * 분석 요약결과
     */
    private fun getSummary(result: DmAnalysisReportResult): String {
        val report = StringBuilder()
        val tradeConditionList = result.dmAnalysisCondition.tradeConditionList

        report.append("----------- Buy&Hold 결과 -----------\n")
        report.append(String.format("합산 동일비중 수익\t %,.2f%%", result.common.buyHoldYieldTotal.yield * 100))
            .append("\n")
        report.append(String.format("합산 동일비중 MDD\t %,.2f%%", result.common.buyHoldYieldTotal.mdd * 100)).append("\n")
        report.append(String.format("합산 동일비중 CAGR\t %,.2f%%", result.common.buyHoldYieldTotal.getCagr() * 100))
            .append("\n")
        report.append(String.format("샤프지수\t %,.2f", result.common.getBuyHoldSharpeRatio())).append("\n")

//        for (i in 1..tradeConditionList.size) {
//            val tradeCondition = tradeConditionList[i - 1]
//            report.append(
//                "${i}. 조건번호: ${tradeCondition.conditionSeq}, 종목: ${tradeCondition.stock.name}(${tradeCondition.stock.code}), " +
//                        "매매주기: ${tradeCondition.periodType}, 변동성 비율: ${tradeCondition.kRate}, 이동평균 단위:${tradeCondition.maPeriod}, " +
//                        "갭상승 통과: ${tradeCondition.gapRisenSkip}, 하루에 한번 거래: ${tradeCondition.onlyOneDayTrade}\n"
//            )
//            val sumYield = result.common.buyHoldYieldCondition[tradeCondition.conditionSeq]
//            if (sumYield == null) {
//                log.warn("조건에 해당하는 결과가 없습니다. vbsConditionSeq: ${tradeCondition.conditionSeq}")
//                break
//            }
//            report.append(String.format("${i}. 동일비중 수익\t %,.2f%%", sumYield.yield * 100)).append("\n")
//            report.append(String.format("${i}. 동일비중 MDD\t %,.2f%%", sumYield.mdd * 100)).append("\n")
//        }

        val totalYield: CommonAnalysisReportResult.TotalYield = result.common.yieldTotal
        report.append("----------- 전략 결과 -----------\n")
        report.append(String.format("합산 실현 수익\t %,.2f%%", totalYield.yield * 100)).append("\n")
        report.append(String.format("합산 실현 MDD\t %,.2f%%", totalYield.mdd * 100)).append("\n")
        report.append(String.format("합산 매매회수\t %d", result.common.getWinningRateTotal().getTradeCount())).append("\n")
        report.append(String.format("합산 승률\t %,.2f%%", result.common.getWinningRateTotal().getWinRate() * 100))
            .append("\n")
        report.append(String.format("합산 CAGR\t %,.2f%%", totalYield.getCagr() * 100)).append("\n")
        report.append(String.format("샤프지수\t %,.2f", result.common.getBacktestSharpeRatio())).append("\n")

        for (i in 1..tradeConditionList.size) {
            val tradeCondition = tradeConditionList[i - 1]
//            report.append(
//                "${i}. 조건번호: ${tradeCondition.conditionSeq}, 종목: ${tradeCondition.stock.name}(${tradeCondition.stock.code}), " +
//                        "매매주기: ${tradeCondition.periodType}, 변동성 비율: ${tradeCondition.kRate}, 이동평균 단위:${tradeCondition.maPeriod}, " +
//                        "갭상승 통과: ${tradeCondition.gapRisenSkip}, 하루에 한번 거래: ${tradeCondition.onlyOneDayTrade}\n"
//            )
//
//            val winningRate = result.common.winningRateCondition[tradeCondition.conditionSeq]
//            if (winningRate == null) {
//                log.warn("조건에 해당하는 결과가 없습니다. vbsConditionSeq: ${tradeCondition.conditionSeq}")
//                break
//            }
//            report.append(String.format("${i}. 실현 수익\t %,f", winningRate.invest)).append("\n")
//            report.append(String.format("${i}. 매매회수\t %d", winningRate.getTradeCount())).append("\n")
//            report.append(String.format("${i}. 승률\t %,.2f%%", winningRate.getWinRate() * 100)).append("\n")
        }
        return report.toString()
    }
    /**
     * 분석건에 대한 리포트 파일 만듦
     * @return 엑셀 파일
     */
    private fun makeReportFile(result: DmAnalysisReportResult): File {
        val reportFileSubPrefix = ReportMakerHelperService.getReportFileSuffix(result)
        val reportFile = File("./backtest-result/dm-trade-report", "dm_trade_$reportFileSubPrefix")

        XSSFWorkbook().use { workbook ->
            var sheet = createTradeReport(result, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "1. 매매이력")

            sheet = ReportMakerHelperService.createReportEvalAmount(result.common.evaluationAmountHistory, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "2. 일짜별 자산비율 변화")

            sheet = ReportMakerHelperService.createReportRangeReturn(result.common.getMonthlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "3. 월별 수익률")

            sheet = ReportMakerHelperService.createReportRangeReturn(result.common.getYearlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "4. 년별 수익률")

            sheet = createReportSummary(result, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "5. 매매 요약결과 및 조건")

            FileOutputStream(reportFile).use { ous ->
                workbook.write(ous)
            }
        }
        println("결과 파일:" + reportFile.name)
        return reportFile
    }

    /**
     * 매매 내역을 시트로 만듦
     */
    private fun createTradeReport(result: DmAnalysisReportResult, workbook: XSSFWorkbook): XSSFSheet {
        val sheet = workbook.createSheet()
        val header =
            "날짜,종목,매매 구분,매수 수량,매매 금액,체결 가격,실현 수익률,수수료,투자 수익(수수료포함),보유 주식 평가금,매매후 보유 현금,평가금(주식+현금),수익비"
        ReportMakerHelperService.applyHeader(sheet, header)
        var rowIdx = 1

        val defaultStyle = ReportMakerHelperService.ExcelStyle.createDate(workbook)
        val commaStyle = ReportMakerHelperService.ExcelStyle.createComma(workbook)
        val percentStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        val decimalStyle = ReportMakerHelperService.ExcelStyle.createDecimal(workbook)

        result.tradeHistory.forEach { tradeItem: DmTradeReportItem ->
            val dmTrade = tradeItem.dmTrade
            val dmConditionEntity = dmTrade.getConditionEntity()
            val tradeDate: LocalDateTime = dmTrade.tradeDate

            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0
            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeDate)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(dmConditionEntity.stock.getNameCode())
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(dmTrade.tradeType.name)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.common.qty.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.getBuyAmount())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(dmTrade.unitPrice)
            if (dmTrade.unitPrice > 100) {
                createCell.cellStyle = commaStyle
            } else {
                createCell.cellStyle = decimalStyle
            }

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(dmTrade.yield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.common.feePrice)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.common.gains)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.common.stockEvalPrice)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.common.cash)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.common.getEvalPrice())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx)
            createCell.setCellValue(tradeItem.common.getEvalPrice() / result.dmAnalysisCondition.basic.cash)
            createCell.cellStyle = decimalStyle
        }

        sheet.createFreezePane(0, 1)
        sheet.defaultColumnWidth = 12
        sheet.setColumnWidth(0, 4000)
        sheet.setColumnWidth(1, 4000)
        sheet.setColumnWidth(12, 4000)
        sheet.setColumnWidth(13, 4000)
        sheet.setColumnWidth(14, 4000)
        sheet.setColumnWidth(15, 4000)

        ReportMakerHelperService.ExcelStyle.applyAllBorder(sheet)
        ReportMakerHelperService.ExcelStyle.applyDefaultFont(sheet)
        return sheet
    }


    /**
     * 매매 결과 요약 및 조건 시트 만듦
     */
    private fun createReportSummary(result: DmAnalysisReportResult, workbook: XSSFWorkbook): XSSFSheet {
        val sheet = workbook.createSheet()
        val summary = getSummary(result)
        ReportMakerHelperService.textToSheet(summary, sheet)
        log.debug(summary)

        val conditionSummary = getConditionSummary(result)
        ReportMakerHelperService.textToSheet(conditionSummary, sheet)
        sheet.defaultColumnWidth = 60
        return sheet
    }


    /**
     * 백테스트 조건 요약 정보
     */
    private fun getConditionSummary(
        result: DmAnalysisReportResult
    ): String {
        val range: DateRange = result.dmAnalysisCondition.basic.range
        val condition = result.dmAnalysisCondition

        val report = StringBuilder()

        report.append("----------- 백테스트 조건 -----------\n")
        report.append(String.format("분석기간\t %s", range)).append("\n")
        report.append(String.format("투자비율\t %,.2f%%", condition.basic.investRatio * 100)).append("\n")
        report.append(String.format("최초 투자금액\t %,f", condition.basic.cash)).append("\n")
        report.append(String.format("매수 수수료\t %,.2f%%", condition.basic.feeBuy * 100)).append("\n")
        report.append(String.format("매도 수수료\t %,.2f%%", condition.basic.feeSell * 100)).append("\n")

        val tradeConditionList = result.dmAnalysisCondition.tradeConditionList

        for (i in 1..tradeConditionList.size) {
            val tradeCondition = tradeConditionList[i - 1]
            report.append(String.format("${i}. 조건아이디\t %s", tradeCondition.getConditionId())).append("\n")
            report.append(String.format("${i}. 대상 종목\t %s", tradeCondition.stock.getNameCode())).append("\n")
        }
        return report.toString()
    }

}