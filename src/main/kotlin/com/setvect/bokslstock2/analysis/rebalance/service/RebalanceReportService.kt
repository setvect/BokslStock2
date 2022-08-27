package com.setvect.bokslstock2.analysis.rebalance.service

import com.setvect.bokslstock2.analysis.common.model.AnalysisResult
import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.analysis.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.analysis.rebalance.model.RebalanceBacktestCondition
import com.setvect.bokslstock2.util.ApplicationUtil
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream

@Service
// TODO spring bean에 의존하지 않음. static한 helper class로 변경 가능
class RebalanceReportService() {
    /**
     * 분석건에 대한 리포트 파일 만듦
     * @return 엑셀 파일
     */
    internal fun makeReportFile(
        backtestCondition: RebalanceBacktestCondition,
        analysisResult: AnalysisResult
    ): File {
        val rebalanceFacter = backtestCondition.rebalanceFacter
        val stockCodes = backtestCondition.stockCodes
        val append =
            "_${stockCodes.joinToString { it.stockCode.code }}_${rebalanceFacter.periodType},${rebalanceFacter.threshold}"

        val reportFileSubPrefix = ReportMakerHelperService.getReportFileSuffix(backtestCondition.tradeCondition, backtestCondition.listStock(), append)
        val reportFile = File(
            "./backtest-result/rebalance-trade-report",
            "rebalance_trade_${reportFileSubPrefix}"
        )

        XSSFWorkbook().use { workbook ->
            var sheet = ReportMakerHelperService.createTradeReport(analysisResult, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "1. 매매이력")

            sheet = ReportMakerHelperService.createReportEvalAmount(
                analysisResult.common.evaluationAmountHistory,
                workbook
            )
            workbook.setSheetName(workbook.getSheetIndex(sheet), "2. 일자별 자산비율 변화")

            sheet = ReportMakerHelperService.createReportRangeReturn(analysisResult.common.getMonthlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "3. 월별 수익률")

            sheet = ReportMakerHelperService.createReportRangeReturn(analysisResult.common.getYearlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "4. 년별 수익률")

            sheet = createReportSummary(backtestCondition, analysisResult, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "5. 매매 요약결과 및 조건")

            sheet.createFreezePane(0, 1)

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
        backtestCondition: RebalanceBacktestCondition,
        analysisResult: AnalysisResult,
        workbook: XSSFWorkbook
    ): XSSFSheet {
        val sheet = workbook.createSheet()
        val summary = getSummary(backtestCondition, analysisResult.common)
        ReportMakerHelperService.textToSheet(summary, sheet)
        sheet.defaultColumnWidth = 60
        return sheet
    }

    internal fun getSummary(
        rebalanceBacktestCondition: RebalanceBacktestCondition,
        commonAnalysisReportResult: CommonAnalysisReportResult
    ): String {
        val report = StringBuilder()
        report.append("----------- Buy&Hold 결과 -----------\n")
        val buyHoldText = ApplicationUtil.makeSummaryCompareStock(
            commonAnalysisReportResult.benchmarkTotalYield.buyHoldTotalYield,
            commonAnalysisReportResult.getBuyHoldSharpeRatio()
        )
        report.append(buyHoldText)

        report.append("----------- Benchmark 결과 -----------\n")
        val benchmarkText = ApplicationUtil.makeSummaryCompareStock(
            commonAnalysisReportResult.benchmarkTotalYield.benchmarkTotalYield,
            commonAnalysisReportResult.getBenchmarkSharpeRatio()
        )
        report.append(benchmarkText)

        val totalYield: CommonAnalysisReportResult.TotalYield = commonAnalysisReportResult.yieldTotal
        report.append("----------- 전략 결과 -----------\n")
        report.append(String.format("합산 실현 수익\t %,.2f%%", totalYield.yield * 100)).append("\n")
        report.append(String.format("합산 실현 MDD\t %,.2f%%", totalYield.mdd * 100)).append("\n")
        report.append(String.format("합산 매매회수\t %d", commonAnalysisReportResult.getWinningRateTotal().getTradeCount()))
            .append("\n")
        report.append(
            String.format(
                "합산 승률\t %,.2f%%",
                commonAnalysisReportResult.getWinningRateTotal().getWinRate() * 100
            )
        )
            .append("\n")
        report.append(String.format("합산 CAGR\t %,.2f%%", totalYield.getCagr() * 100)).append("\n")
        report.append(String.format("샤프지수\t %,.2f", commonAnalysisReportResult.getBacktestSharpeRatio())).append("\n")

        report.append("----------- 테스트 조건 -----------\n")
        val stockName = rebalanceBacktestCondition.stockCodes.joinToString("\n") { stock ->
            "\t${stock.stockCode.code}[${stock.stockCode.desc}]\t비율: ${stock.weight}%"
        }

        val tradeCondition = rebalanceBacktestCondition.tradeCondition
        report.append("대상종목\n${stockName}").append("\n")
        report.append("리벨런싱 주기\t${rebalanceBacktestCondition.rebalanceFacter.periodType}").append("\n")
        report.append("리벨런싱 입계치\t${rebalanceBacktestCondition.rebalanceFacter.threshold}").append("\n")
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
    internal fun createTotalSummary(
        workbook: XSSFWorkbook,
        conditionResults: List<Pair<RebalanceBacktestCondition, AnalysisResult>>
    ): XSSFSheet {
        val sheet = workbook.createSheet()

        val header = "분석기간,거래종목,리벨런싱주기,리벨런싱입계치,종복별비율,투자비율,최초 투자금액,매수 수수료,매도 수수료," +
            "조건 설명," +
            "매수 후 보유 수익,매수 후 보유 MDD,매수 후 보유 CAGR,매수 후 샤프지수," +
            "밴치마크 보유 수익,밴치마크 보유 MDD,밴치마크 보유 CAGR,밴치마크 샤프지수," +
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
            createCell.setCellValue(multiCondition.rebalanceFacter.periodType.name)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.rebalanceFacter.threshold)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            val stockWeight = multiCondition.stockCodes
                .joinToString(",") { stock ->
                    "${stock.stockCode}:${stock.weight}"
                }
            createCell.setCellValue(stockWeight)
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

            val buyHoldTotalYield: CommonAnalysisReportResult.TotalYield =
                result.common.benchmarkTotalYield.buyHoldTotalYield
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

            val benchmarkTotalYield: CommonAnalysisReportResult.TotalYield =
                result.common.benchmarkTotalYield.benchmarkTotalYield
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