package com.setvect.bokslstock2.analysis.mabs.service

import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult.TotalYield
import com.setvect.bokslstock2.analysis.common.model.CommonTradeReportItem
import com.setvect.bokslstock2.analysis.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.analysis.common.service.TradeService
import com.setvect.bokslstock2.analysis.common.service.TradeService.MakerAnalysisReportResult
import com.setvect.bokslstock2.analysis.common.service.TradeService.MakerTreadReportItem
import com.setvect.bokslstock2.analysis.mabs.entity.MabsConditionEntity
import com.setvect.bokslstock2.analysis.mabs.entity.MabsTradeEntity
import com.setvect.bokslstock2.analysis.mabs.model.MabsAnalysisCondition
import com.setvect.bokslstock2.analysis.mabs.model.MabsAnalysisReportResult
import com.setvect.bokslstock2.analysis.mabs.model.MabsTradeReportItem
import com.setvect.bokslstock2.util.DateRange
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp
import java.time.LocalDateTime
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service


/**
 * 이동평균 돌파 매매 분석
 */
@Service
class MabsAnalysisService(
    reportMakerHelperService: ReportMakerHelperService,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)
    val tradeService =
        TradeService(reportMakerHelperService,
            object :
                MakerTreadReportItem<MabsTradeEntity, MabsTradeReportItem> {
                override fun make(tradeEntity: MabsTradeEntity, common: CommonTradeReportItem): MabsTradeReportItem {
                    return MabsTradeReportItem(tradeEntity, common)
                }
            },
            object :
                MakerAnalysisReportResult<MabsAnalysisCondition, MabsTradeReportItem, MabsAnalysisReportResult> {
                override fun make(
                    analysisCondition: MabsAnalysisCondition,
                    tradeEntity: List<MabsTradeReportItem>,
                    common: CommonAnalysisReportResult
                ): MabsAnalysisReportResult {
                    return MabsAnalysisReportResult(analysisCondition, tradeEntity, common)
                }
            }
        )

    /**
     *  분석 리포트
     */
    fun makeReport(condition: MabsAnalysisCondition) {
        val tradeItemHistory = tradeService.trade(condition)
        val result = tradeService.analysis(tradeItemHistory, condition)
        val summary = getSummary(result)
        println(summary)
        makeReportFile(result)
    }

    /**
     * 분석건에 대한 리포트 파일 만듦
     * @return 엑셀 파일
     */
    private fun makeReportFile(result: MabsAnalysisReportResult): File {
        val reportFileSubPrefix = ReportMakerHelperService.getReportFileSuffix(result)
        val reportFile = File("./backtest-result/mabs-trade-report", "trade_$reportFileSubPrefix")

        XSSFWorkbook().use { workbook ->
            var sheet = createTradeReport(result, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "1. 매매이력")

            sheet = ReportMakerHelperService.createReportEvalAmount(result.common.evaluationAmountHistory, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "2. 일짜별 자산변화")

            sheet = createReportSummary(result, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "3. 매매 요약결과 및 조건")

            FileOutputStream(reportFile).use { ous ->
                workbook.write(ous)
            }
        }
        println("결과 파일:" + reportFile.name)
        return reportFile
    }

    /**
     *  복수개의 조건에 대한 분석 요약 리포트를 만듦
     */
    fun makeSummaryReport(conditionList: List<MabsAnalysisCondition>): File {
        var i = 0
        val resultList = conditionList.map { condition ->
            val tradeItemHistory = tradeService.trade(condition)
            val analysis = tradeService.analysis(tradeItemHistory, condition)
            log.info("분석 진행 ${++i}/${conditionList.size}")
            analysis
        }.toList()


        var rowIdx = 1
        resultList.forEach { result ->
            makeReportFile(result)
            log.info("개별분석파일 생성 ${rowIdx++}/${resultList.size}")
        }

        // 결과 저장
        val reportFile =
            File("./backtest-result", "이평선돌파_전략_백테스트_분석결과_" + Timestamp.valueOf(LocalDateTime.now()).time + ".xlsx")
        XSSFWorkbook().use { workbook ->
            val sheetBacktestSummary = createTotalSummary(workbook, resultList)
            workbook.setSheetName(workbook.getSheetIndex(sheetBacktestSummary), "1. 평가표")

            val sheetCondition = createMultiCondition(workbook, conditionList)
            workbook.setSheetName(workbook.getSheetIndex(sheetCondition), "2. 테스트 조건")
            FileOutputStream(reportFile).use { ous ->
                workbook.write(ous)
            }
        }
        println("결과 파일:" + reportFile.name)
        return reportFile
    }

    /**
     * @return 여러개 백테스트 결과 요약 시트
     */
    private fun createTotalSummary(
        workbook: XSSFWorkbook,
        resultList: List<MabsAnalysisReportResult>
    ): XSSFSheet {
        val sheet = workbook.createSheet()

        val header = "분석기간,분석 아이디," +
                "종목,종목코드,매매주기,단기 이동평균 기간,장기 이동평균 기간," +
                "투자비율,최초 투자금액,하락 매도률,상승 매도률,매수 수수료,매도 수수료," +
                "조건 설명," +
                "매수 후 보유 수익,매수 후 보유 MDD,매수 후 보유 CAGR," +
                "실현 수익,실현 MDD,실현 CAGR,매매 횟수,승률"
        ReportMakerHelperService.applyHeader(sheet, header)
        var rowIdx = 1

        val defaultStyle = ReportMakerHelperService.ExcelStyle.createDate(workbook)
        val dateStyle = ReportMakerHelperService.ExcelStyle.createDate(workbook)
        val commaStyle = ReportMakerHelperService.ExcelStyle.createComma(workbook)
        val percentStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        val percentImportantStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        percentImportantStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        percentImportantStyle.fillForegroundColor = IndexedColors.LEMON_CHIFFON.index

        resultList.forEach { result ->

            val multiCondition = result.mabsAnalysisCondition
            val tradeConditionList = multiCondition.tradeConditionList

            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0
            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.range.toString())
            createCell.cellStyle = dateStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString("|") { it.mabsConditionSeq.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.stock.name })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.stock.code })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.periodType.name })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.shortPeriod.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.longPeriod.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.investRatio)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.cash)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.downSellRate.toString() })
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.upBuyRate.toString() })
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.feeBuy)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.feeSell)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.comment)
            createCell.cellStyle = defaultStyle

            val sumYield: TotalYield = result.common.buyAndHoldYieldTotal

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(sumYield.yield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(sumYield.mdd)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(sumYield.getCagr())
            createCell.cellStyle = percentStyle

            val totalYield: TotalYield = result.common.yieldTotal

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

    /**
     * @return 백테스트 조건 정보를 가지고 있는 시트
     */
    private fun createMultiCondition(
        workbook: XSSFWorkbook,
        conditionList: List<MabsAnalysisCondition>
    ): XSSFSheet {
        val sheet = workbook.createSheet()
        val conditionHeader = "분석 아이디,종목이름,종목코드,매매주기,단기 이동평균 기간,장기 이동평균 기간,하락매도률,상승매도률"
        ReportMakerHelperService.applyHeader(sheet, conditionHeader)

        val mabsConditionList: List<MabsConditionEntity> = conditionList
            .flatMap { it.tradeConditionList }
            .distinct()
            .toList()

        var rowIdx = 1
        val defaultStyle = ReportMakerHelperService.ExcelStyle.createDate(workbook)
        val percentStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        val commaStyle = ReportMakerHelperService.ExcelStyle.createComma(workbook)

        for (condition in mabsConditionList) {
            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0

            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.mabsConditionSeq.toString())
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.stock.name)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.stock.code)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.periodType.name)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.shortPeriod.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.longPeriod.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.downSellRate)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx)
            createCell.setCellValue(condition.upBuyRate)
            createCell.cellStyle = percentStyle
        }

        sheet.createFreezePane(0, 1)
        sheet.defaultColumnWidth = 15

        ReportMakerHelperService.ExcelStyle.applyAllBorder(sheet)
        ReportMakerHelperService.ExcelStyle.applyDefaultFont(sheet)
        return sheet
    }

    /**
     * 분석 요약결과
     */
    private fun getSummary(result: MabsAnalysisReportResult): String {
        val report = StringBuilder()
        val tradeConditionList = result.mabsAnalysisCondition.tradeConditionList

        report.append("----------- Buy&Hold 결과 -----------\n")
        report.append(String.format("합산 동일비중 수익\t %,.2f%%", result.common.buyAndHoldYieldTotal.yield * 100))
            .append("\n")
        report.append(String.format("합산 동일비중 MDD\t %,.2f%%", result.common.buyAndHoldYieldTotal.mdd * 100)).append("\n")
        report.append(String.format("합산 동일비중 CAGR\t %,.2f%%", result.common.buyAndHoldYieldTotal.getCagr() * 100))
            .append("\n")

        for (i in 1..tradeConditionList.size) {
            val tradeCondition = tradeConditionList[i - 1]
            report.append("${i}. 조건번호: ${tradeCondition.mabsConditionSeq}, 종목: ${tradeCondition.stock.name}(${tradeCondition.stock.code}), 단기-장기(${tradeCondition.periodType}): ${tradeCondition.shortPeriod}-${tradeCondition.longPeriod}\n")
            val sumYield = result.common.buyAndHoldYieldCondition[tradeCondition.mabsConditionSeq]
            if (sumYield == null) {
                log.warn("조건에 해당하는 결과가 없습니다. mabsConditionSeq: ${tradeCondition.mabsConditionSeq}")
                break
            }
            report.append(String.format("${i}. 동일비중 수익\t %,.2f%%", sumYield.yield * 100)).append("\n")
            report.append(String.format("${i}. 동일비중 MDD\t %,.2f%%", sumYield.mdd * 100)).append("\n")
        }


        val totalYield: TotalYield = result.common.yieldTotal
        report.append("----------- 전략 결과 -----------\n")
        report.append(String.format("합산 실현 수익\t %,.2f%%", totalYield.yield * 100)).append("\n")
        report.append(String.format("합산 실현 MDD\t %,.2f%%", totalYield.mdd * 100)).append("\n")
        report.append(String.format("합산 매매회수\t %d", result.common.getWinningRateTotal().getTradeCount())).append("\n")
        report.append(String.format("합산 승률\t %,.2f%%", result.common.getWinningRateTotal().getWinRate() * 100))
            .append("\n")
        report.append(String.format("합산 CAGR\t %,.2f%%", totalYield.getCagr() * 100)).append("\n")

        for (i in 1..tradeConditionList.size) {
            val tradeCondition = tradeConditionList[i - 1]
            report.append("${i}. 조건번호: ${tradeCondition.mabsConditionSeq}, 종목: ${tradeCondition.stock.name}(${tradeCondition.stock.code}), 단기-장기(${tradeCondition.periodType}): ${tradeCondition.shortPeriod}-${tradeCondition.longPeriod}\n")

            val winningRate = result.common.winningRateCondition[tradeCondition.mabsConditionSeq]
            if (winningRate == null) {
                log.warn("조건에 해당하는 결과가 없습니다. mabsConditionSeq: ${tradeCondition.mabsConditionSeq}")
                break
            }
            report.append(String.format("${i}. 실현 수익\t %,f", winningRate.invest)).append("\n")
            report.append(String.format("${i}. 매매회수\t %d", winningRate.getTradeCount())).append("\n")
            report.append(String.format("${i}. 승률\t %,.2f%%", winningRate.getWinRate() * 100)).append("\n")
        }
        return report.toString()
    }

    /**
     * 매매 내역을 시트로 만듦
     */
    private fun createTradeReport(result: MabsAnalysisReportResult, workbook: XSSFWorkbook): XSSFSheet {
        val sheet = workbook.createSheet()
        val header =
            "날짜,종목,매매 구분,단기 이동평균,장기 이동평균,매수 수량,매매 금액,체결 가격,최고수익률,최저수익률,실현 수익률,수수료,투자 수익(수수료포함),보유 주식 평가금,매매후 보유 현금,평가금(주식+현금),수익비"
        ReportMakerHelperService.applyHeader(sheet, header)
        var rowIdx = 1

        val defaultStyle = ReportMakerHelperService.ExcelStyle.createDate(workbook)
        val commaStyle = ReportMakerHelperService.ExcelStyle.createComma(workbook)
        val percentStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        val decimalStyle = ReportMakerHelperService.ExcelStyle.createDecimal(workbook)

        result.tradeHistory.forEach { tradeItem: MabsTradeReportItem ->
            val mabsTradeEntity: MabsTradeEntity = tradeItem.mabsTradeEntity
            val mabsConditionEntity: MabsConditionEntity = mabsTradeEntity.mabsConditionEntity
            val tradeDate: LocalDateTime = mabsTradeEntity.tradeDate

            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0
            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeDate)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(mabsConditionEntity.stock.getNameCode())
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(mabsTradeEntity.tradeType.name)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(mabsTradeEntity.maShort)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(mabsTradeEntity.maLong)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.common.qty.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.getBuyAmount())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(mabsTradeEntity.unitPrice)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(mabsTradeEntity.highYield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(mabsTradeEntity.lowYield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(mabsTradeEntity.yield)
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
            createCell.setCellValue(tradeItem.common.getEvalPrice() / result.mabsAnalysisCondition.basic.cash)
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
    private fun createReportSummary(result: MabsAnalysisReportResult, workbook: XSSFWorkbook): XSSFSheet {
        val sheet = workbook.createSheet()
        val summary = getSummary(result)
        log.debug(summary)
        ReportMakerHelperService.textToSheet(summary, sheet)
        val conditionSummary = getConditionSummary(result)
        ReportMakerHelperService.textToSheet(conditionSummary, sheet)

        sheet.defaultColumnWidth = 60
        return sheet
    }


    /**
     * 백테스트 조건 요약 정보
     */
    private fun getConditionSummary(
        result: MabsAnalysisReportResult
    ): String {
        val range: DateRange = result.mabsAnalysisCondition.basic.range
        val condition = result.mabsAnalysisCondition

        val report = StringBuilder()

        report.append("----------- 백테스트 조건 -----------\n")
        report.append(String.format("분석기간\t %s", range)).append("\n")
        report.append(String.format("투자비율\t %,.2f%%", condition.basic.investRatio * 100)).append("\n")
        report.append(String.format("최초 투자금액\t %,f", condition.basic.cash)).append("\n")
        report.append(String.format("매수 수수료\t %,.2f%%", condition.basic.feeBuy * 100)).append("\n")
        report.append(String.format("매도 수수료\t %,.2f%%", condition.basic.feeSell * 100)).append("\n")

        val tradeConditionList: List<MabsConditionEntity> = result.mabsAnalysisCondition.tradeConditionList

        for (i in 1..tradeConditionList.size) {
            val tradeCondition = tradeConditionList[i - 1]
            report.append(String.format("${i}. 조건아이디\t %s", tradeCondition.mabsConditionSeq)).append("\n")
            report.append(String.format("${i}. 분석주기\t %s", tradeCondition.periodType)).append("\n")
            report.append(String.format("${i}. 대상 종목\t %s", tradeCondition.stock.getNameCode())).append("\n")
            report.append(String.format("${i}. 상승 매수률\t %,.2f%%", tradeCondition.upBuyRate * 100)).append("\n")
            report.append(String.format("${i}. 하락 매도률\t %,.2f%%", tradeCondition.downSellRate * 100)).append("\n")
            report.append(String.format("${i}. 단기 이동평균 기간\t %d", tradeCondition.shortPeriod)).append("\n")
            report.append(String.format("${i}. 장기 이동평균 기간\t %d", tradeCondition.longPeriod)).append("\n")
        }
        return report.toString()
    }
}