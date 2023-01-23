package com.setvect.bokslstock2.analysis.vbs.service

import com.setvect.bokslstock2.analysis.common.model.AnalysisResult
import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult.TotalYield
import com.setvect.bokslstock2.analysis.common.service.BacktestTradeService
import com.setvect.bokslstock2.analysis.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.analysis.vbs.entity.VbsConditionEntity
import com.setvect.bokslstock2.analysis.vbs.model.VbsAnalysisCondition
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp
import java.time.LocalDateTime


/**
 * 변동성 돌파 매매 분석
 */
@Service
class VbsAnalysisService(
    val backtestTradeService: BacktestTradeService
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     *  분석 리포트
     */
    fun makeReport(vbsAnalysisCondition: VbsAnalysisCondition) {
        val trades =
            backtestTradeService.tradeBundle(vbsAnalysisCondition.basic, vbsAnalysisCondition.getPreTradeBundles())
        val analysisResult =
            backtestTradeService.analysis(trades, vbsAnalysisCondition.basic, vbsAnalysisCondition.getStockCodes())
        val summary = getSummary(vbsAnalysisCondition, analysisResult)
        println(summary)
        makeReportFile(vbsAnalysisCondition, analysisResult)
    }

    /**
     * 분석건에 대한 리포트 파일 만듦
     * @return 엑셀 파일
     */
    private fun makeReportFile(vbsAnalysisCondition: VbsAnalysisCondition, analysisResult: AnalysisResult): File {
        val reportFileSubPrefix =
            ReportMakerHelperService.getReportFileSuffix(
                vbsAnalysisCondition.basic,
                vbsAnalysisCondition.getStockCodes()
            )
        val reportFile = File("./backtest-result/vbs-trade-report", "vbs_trade_$reportFileSubPrefix")

        XSSFWorkbook().use { workbook ->
            var sheet = ReportMakerHelperService.createTradeReport(analysisResult, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "1. 매매이력")

            sheet =
                ReportMakerHelperService.createReportEvalAmount(analysisResult.common.evaluationAmountHistory, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "2. 일자별 자산비율 변화")

            sheet = ReportMakerHelperService.createReportRangeReturn(analysisResult.common.getMonthlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "3. 월별 수익률")

            sheet = ReportMakerHelperService.createReportRangeReturn(analysisResult.common.getYearlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "4. 년별 수익률")

            sheet = createReportSummary(vbsAnalysisCondition, analysisResult, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "5. 매매 요약결과 및 조건")

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
    fun makeSummaryReport(conditionList: List<VbsAnalysisCondition>): File {
        var i = 0
        val conditionResults = conditionList.map { vbsAnalysisCondition ->
            val range = backtestTradeService.fitBacktestRange(
                vbsAnalysisCondition.getStockCodes(),
                vbsAnalysisCondition.basic.range
            )
            log.info("범위 조건 변경: ${vbsAnalysisCondition.basic.range} -> $range")
            vbsAnalysisCondition.basic.range = range

            val tradeItemHistory =
                backtestTradeService.tradeBundle(vbsAnalysisCondition.basic, vbsAnalysisCondition.getPreTradeBundles())
            val analysisResult = backtestTradeService.analysis(
                tradeItemHistory,
                vbsAnalysisCondition.basic,
                vbsAnalysisCondition.getStockCodes()
            )
            log.info("분석 진행 ${++i}/${conditionList.size}")
            Pair(vbsAnalysisCondition, analysisResult)
        }.toList()

        for (idx in conditionResults.indices) {
            val conditionResult = conditionResults[idx]
            makeReportFile(conditionResult.first, conditionResult.second)
            log.info("개별분석파일 생성 ${idx + 1}/${conditionList.size}")
        }

        // 결과 저장
        val reportFile =
            File("./backtest-result", "변동성돌파_전략_백테스트_분석결과_" + Timestamp.valueOf(LocalDateTime.now()).time + ".xlsx")
        XSSFWorkbook().use { workbook ->
            val sheetBacktestSummary = createTotalSummary(workbook, conditionResults)
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
        conditionResults: List<Pair<VbsAnalysisCondition, AnalysisResult>>
    ): XSSFSheet {
        val sheet = workbook.createSheet()

        val header = "분석기간,분석 아이디,종목,투자비율,최초 투자금액,매수 수수료,매도 수수료," +
                "매매주기,변동성비율,이동평균단위,갭 상승 매도 넘김,하루에 한번 거래,호가단위," +
                "조건 설명," +
                "매수 후 보유 수익,매수 후 보유 MDD,매수 후 보유 CAGR,매수 후 보유 샤프지수," +
                "밴치마크 수익,밴치마크 MDD,밴치마크 보유 CAGR,밴치마크 샤프지수," +
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
            val tradeConditionList = multiCondition.tradeConditionList

            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0
            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.range.toString())
            createCell.cellStyle = dateStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString("|") { it.conditionSeq.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.stock.name })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.investRatio)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.cash)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.feeBuy)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.feeSell)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.periodType.name })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.kRate.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.maPeriod.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.gapRisenSkip.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.onlyOneDayTrade.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.unitAskPrice.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.comment)
            createCell.cellStyle = defaultStyle

            val result = conditionResult.second


            val buyHoldTotalYield: TotalYield = result.common.benchmarkTotalYield.buyHoldTotalYield
            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(buyHoldTotalYield.yield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(buyHoldTotalYield.mdd)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(buyHoldTotalYield.getCagr())
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(result.common.getBuyHoldSharpeRatio())
            createCell.cellStyle = decimalStyle

            val benchmarkTotalYield: TotalYield = result.common.benchmarkTotalYield.benchmarkTotalYield
            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(benchmarkTotalYield.yield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(benchmarkTotalYield.mdd)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(benchmarkTotalYield.getCagr())
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(result.common.getBenchmarkSharpeRatio())
            createCell.cellStyle = decimalStyle

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

    /**
     * @return 백테스트 조건 정보를 가지고 있는 시트
     */
    private fun createMultiCondition(
        workbook: XSSFWorkbook,
        conditionList: List<VbsAnalysisCondition>
    ): XSSFSheet {
        val sheet = workbook.createSheet()
        val conditionHeader = "분석 아이디,종목이름,종목코드,매매주기,변동성비율,이동평균단위,갭 상승 매도 넘김,하루에 한번 거래,호가단위,조건설명"
        ReportMakerHelperService.applyHeader(sheet, conditionHeader)

        val vbsConditionList: List<VbsConditionEntity> = conditionList
            .flatMap { it.tradeConditionList }
            .distinct()
            .toList()

        var rowIdx = 1
        val defaultStyle = ReportMakerHelperService.ExcelStyle.createDefault(workbook)
        val percentStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        val decimalStyle = ReportMakerHelperService.ExcelStyle.createDecimal(workbook)

        for (condition in vbsConditionList) {
            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0

            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.conditionSeq.toString())
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
            createCell.setCellValue(condition.kRate)
            createCell.cellStyle = decimalStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.maPeriod.toDouble())
            createCell.cellStyle = decimalStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.gapRisenSkip)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.onlyOneDayTrade)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.unitAskPrice)
            createCell.cellStyle = decimalStyle

            createCell = row.createCell(cellIdx)
            createCell.setCellValue(condition.comment)
            createCell.cellStyle = defaultStyle
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
    private fun getSummary(vbsAnalysisCondition: VbsAnalysisCondition, analysisResult: AnalysisResult): String {
        return ReportMakerHelperService.createSummary(
            analysisResult.common,
            vbsAnalysisCondition.tradeConditionList,
            vbsAnalysisCondition.basic,
            getSpecialInfo(vbsAnalysisCondition)
        )
    }

    private fun getSpecialInfo(vbsAnalysisCondition: VbsAnalysisCondition): String {
        val report = StringBuilder()
        for (i in 1..vbsAnalysisCondition.tradeConditionList.size) {
            val tradeCondition = vbsAnalysisCondition.tradeConditionList[i - 1]
            report.append(String.format("${i}. 조건아이디\t %s", tradeCondition.conditionSeq)).append("\n")
            report.append(String.format("${i}. 분석주기\t %s", tradeCondition.periodType)).append("\n")
            report.append(String.format("${i}. 대상 종목\t %s", tradeCondition.stock.getNameCode())).append("\n")
            report.append(String.format("${i}. 변동성 비율\t %,.2f", tradeCondition.kRate)).append("\n")
            report.append(String.format("${i}. 이동평균 단위\t %d", tradeCondition.maPeriod)).append("\n")
            report.append(String.format("${i}. 갭 상승 시 매도 넘김\t %s", tradeCondition.gapRisenSkip)).append("\n")
            report.append(String.format("${i}. 5분 마다 시세 체크\t %s", tradeCondition.stayGapRise)).append("\n")
            report.append(String.format("${i}. 하루에 한번 거래\t %s", tradeCondition.onlyOneDayTrade)).append("\n")
            report.append(String.format("${i}. 호가단위\t %s", tradeCondition.unitAskPrice)).append("\n")
            report.append(String.format("${i}. 조건 설명\t %s", tradeCondition.comment)).append("\n")
        }
        return report.toString()
    }

    /**
     * 매매 결과 요약 및 조건 시트 만듦
     */
    private fun createReportSummary(
        vbsAnalysisCondition: VbsAnalysisCondition,
        analysisResult: AnalysisResult,
        workbook: XSSFWorkbook
    ): XSSFSheet {
        val sheet = workbook.createSheet()
        val summary = getSummary(vbsAnalysisCondition, analysisResult)
        ReportMakerHelperService.textToSheet(summary, sheet)
        log.info(summary)

        sheet.defaultColumnWidth = 60
        return sheet
    }
}