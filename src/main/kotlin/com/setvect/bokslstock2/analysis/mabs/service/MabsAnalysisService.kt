package com.setvect.bokslstock2.analysis.mabs.service

import com.setvect.bokslstock2.analysis.common.model.AnalysisResult
import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult.TotalYield
import com.setvect.bokslstock2.analysis.common.service.BacktestTradeService
import com.setvect.bokslstock2.analysis.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.analysis.mabs.entity.MabsConditionEntity
import com.setvect.bokslstock2.analysis.mabs.model.MabsAnalysisCondition
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
 * 이동평균 돌파 매매 분석
 */
@Service
class MabsAnalysisService(
    val backtestTradeService: BacktestTradeService
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     *  분석 리포트
     */
    fun makeReport(mabsAnalysisCondition: MabsAnalysisCondition) {
        val trades =
            backtestTradeService.tradeBundle(mabsAnalysisCondition.basic, mabsAnalysisCondition.getPreTradeBundles())
        val analysisResult =
            backtestTradeService.analysis(trades, mabsAnalysisCondition.basic, mabsAnalysisCondition.getStockCodes())
        val summary = getSummary(mabsAnalysisCondition, analysisResult)
        println(summary)
        makeReportFile(mabsAnalysisCondition, analysisResult)
    }

    /**
     * 분석건에 대한 리포트 파일 만듦
     * @return 엑셀 파일
     */
    private fun makeReportFile(mabsAnalysisCondition: MabsAnalysisCondition, analysisResult: AnalysisResult): File {
        val reportFileSubPrefix =
            ReportMakerHelperService.getReportFileSuffix(
                mabsAnalysisCondition.basic,
                mabsAnalysisCondition.getStockCodes()
            )
        val reportFile = File("./backtest-result/mabs-trade-report", "mabs_trade_$reportFileSubPrefix")

        XSSFWorkbook().use { workbook ->
            var sheet = ReportMakerHelperService.createTradeReport(analysisResult, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "1. 매매이력")

            sheet =
                ReportMakerHelperService.createReportEvalAmount(analysisResult.common.evaluationAmountHistory, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "2. 일자별 자산변화")

            sheet = ReportMakerHelperService.createReportRangeReturn(analysisResult.common.getMonthlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "3. 월별 수익률")

            sheet = ReportMakerHelperService.createReportRangeReturn(analysisResult.common.getYearlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "4. 년별 수익률")

            sheet = createReportSummary(mabsAnalysisCondition, analysisResult, workbook)
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
    fun makeSummaryReport(conditionList: List<MabsAnalysisCondition>): File {
        var i = 0
        val conditionResults = conditionList.map { mabsAnalysisCondition ->
            val tradeItemHistory = backtestTradeService.tradeBundle(
                mabsAnalysisCondition.basic,
                mabsAnalysisCondition.getPreTradeBundles()
            )
            val analysisResult = backtestTradeService.analysis(
                tradeItemHistory,
                mabsAnalysisCondition.basic,
                mabsAnalysisCondition.getStockCodes()
            )
            log.info("분석 진행 ${++i}/${conditionList.size}")
            Pair(mabsAnalysisCondition, analysisResult)
        }.toList()

        for (idx in conditionResults.indices) {
            val conditionResult = conditionResults[idx]
            makeReportFile(conditionResult.first, conditionResult.second)
            log.info("개별분석파일 생성 ${idx + 1}/${conditionList.size}")
        }

        // 결과 저장
        val reportFile =
            File("./backtest-result", "이평선돌파_전략_백테스트_분석결과_" + Timestamp.valueOf(LocalDateTime.now()).time + ".xlsx")
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
        conditionResults: List<Pair<MabsAnalysisCondition, AnalysisResult>>
    ): XSSFSheet {
        val sheet = workbook.createSheet()

        val header = "분석기간,분석 아이디," +
                "종목,종목코드,매매주기,단기 이동평균 기간,장기 이동평균 기간," +
                "투자비율,최초 투자금액,하락 매도률,상승 매도률,매수 수수료,매도 수수료," +
                "조건 설명," +
                "매수 후 보유 수익,매수 후 보유 MDD,매수 후 보유 CAGR,매수 후 보유 샤프지수," +
                "매치 마크 수익,매치 마크 MDD,매치 마크 CAGR,매치 마크 샤프지수," +
                "실현 수익,실현 MDD,실현 CAGR,샤프지수,매매 횟수,승률"
        ReportMakerHelperService.applyHeader(sheet, header)
        var rowIdx = 1

        val defaultStyle = ReportMakerHelperService.ExcelStyle.createDate(workbook)
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
    private fun getSummary(mabsAnalysisCondition: MabsAnalysisCondition, analysisResult: AnalysisResult): String {
        return ReportMakerHelperService.createSummary(
            analysisResult.common,
            mabsAnalysisCondition.tradeConditionList,
            mabsAnalysisCondition.basic,
            getSpecialInfo(mabsAnalysisCondition)
        )
    }

    private fun getSpecialInfo(mabsAnalysisCondition: MabsAnalysisCondition): String {
        val report = StringBuilder()
        for (i in 1..mabsAnalysisCondition.tradeConditionList.size) {
            val tradeCondition = mabsAnalysisCondition.tradeConditionList[i - 1]
            report.append(String.format("${i}. 조건아이디\t %s", tradeCondition.conditionSeq)).append("\n")
            report.append(String.format("${i}. 분석주기\t %s", tradeCondition.periodType)).append("\n")
            report.append(String.format("${i}. 대상 종목\t %s", tradeCondition.stock.getNameCode())).append("\n")
            report.append(String.format("${i}. 상승 매수률\t %,.2f%%", tradeCondition.upBuyRate * 100)).append("\n")
            report.append(String.format("${i}. 하락 매도률\t %,.2f%%", tradeCondition.downSellRate * 100)).append("\n")
            report.append(String.format("${i}. 단기 이동평균 기간\t %d", tradeCondition.shortPeriod)).append("\n")
            report.append(String.format("${i}. 장기 이동평균 기간\t %d", tradeCondition.longPeriod)).append("\n")
        }
        return report.toString()
    }

    /**
     * 매매 결과 요약 및 조건 시트 만듦
     */
    private fun createReportSummary(
        mabsAnalysisCondition: MabsAnalysisCondition,
        analysisResult: AnalysisResult,
        workbook: XSSFWorkbook
    ): XSSFSheet {
        val sheet = workbook.createSheet()
        val summary = getSummary(mabsAnalysisCondition, analysisResult)
        ReportMakerHelperService.textToSheet(summary, sheet)

        sheet.defaultColumnWidth = 60
        return sheet
    }


}