package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.entity.ConditionEntity
import com.setvect.bokslstock2.analysis.common.model.AnalysisResult
import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.analysis.common.model.EvaluationRateItem
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.common.model.YieldRateItem
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import java.time.LocalDateTime
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.ss.usermodel.DataFormat
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.streams.toList

/**
 * 리포트 생성에 필요한 공통 메소드 제공
 */
@Service
class ReportMakerHelperService(
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(javaClass)

        /**
         * 매매 내역을 시트로 만듦
         */
        fun createTradeReport(result: AnalysisResult, workbook: XSSFWorkbook): XSSFSheet {
            val sheet = workbook.createSheet()
            val header =
                "날짜,종목,매매 구분,매수 수량,매매 금액,체결 가격,실현 수익률,수수료,투자 수익(수수료포함),보유 주식 평가금,매매후 보유 현금,평가금(주식+현금),수익비"
            applyHeader(sheet, header)
            var rowIdx = 1

            val defaultStyle = ExcelStyle.createDefault(workbook)
            val dateStyle = ExcelStyle.createDate(workbook)
            val commaStyle = ExcelStyle.createComma(workbook)
            val percentStyle = ExcelStyle.createPercent(workbook)
            val decimalStyle = ExcelStyle.createDecimal(workbook)

            result.tradeHistory.forEach { tradeItem ->
                val preTrade = tradeItem.preTrade
//            val vbsConditionEntity: VbsConditionEntity = vbsTradeEntity.vbsConditionEntity
                val tradeDate: LocalDateTime = preTrade.tradeDate

                val row = sheet.createRow(rowIdx++)
                var cellIdx = 0
                var createCell = row.createCell(cellIdx++)
                createCell.setCellValue(tradeDate)
                createCell.cellStyle = dateStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(preTrade.stock.toString())
                createCell.cellStyle = defaultStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(preTrade.tradeType.name)
                createCell.cellStyle = defaultStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(tradeItem.qty.toDouble())
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(tradeItem.getBuyAmount())
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(preTrade.unitPrice)
                if (preTrade.unitPrice > 1000) {
                    createCell.cellStyle = commaStyle
                } else {
                    createCell.cellStyle = decimalStyle
                }

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(preTrade.yield)
                createCell.cellStyle = percentStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(tradeItem.feePrice)
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(tradeItem.gains)
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(tradeItem.stockEvalPrice)
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(tradeItem.cash)
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(tradeItem.getEvalPrice())
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx)
                createCell.setCellValue(tradeItem.getEvalPrice() / result.analysisCondition.cash)
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

            ExcelStyle.applyAllBorder(sheet)
            ExcelStyle.applyDefaultFont(sheet)
            return sheet
        }

        /**
         * 날짜에 따른 평가금액(Buy&Hold, 벡테스트) 변화 시트 만듦
         */
        fun createReportEvalAmount(
            evaluationAmountHistory: List<EvaluationRateItem>,
            workbook: XSSFWorkbook
        ): XSSFSheet {
            val sheet = workbook.createSheet()
            val header = "날짜,Buy&Hold 평가금,백테스트 평가금,Buy&Hold 일일 수익률,백테스트 일일 수익률,Buy&Hold Maxdrawdown,백테스트 Maxdrawdown"
            applyHeader(sheet, header)
            var rowIdx = 1

            val dateStyle = ExcelStyle.createDate(workbook)
            val commaStyle = ExcelStyle.createDecimal(workbook)
            val percentStyle = ExcelStyle.createPercent(workbook)

            var buyAndHoldMax = 0.0
            var backtestMax = 0.0

            evaluationAmountHistory.forEach { evalItem: EvaluationRateItem ->
                val row = sheet.createRow(rowIdx++)
                var cellIdx = 0
                var createCell = row.createCell(cellIdx++)
                buyAndHoldMax = buyAndHoldMax.coerceAtLeast(evalItem.buyHoldRate)
                backtestMax = backtestMax.coerceAtLeast(evalItem.backtestRate)

                createCell.setCellValue(evalItem.baseDate)
                createCell.cellStyle = dateStyle

                // 변화량
                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(evalItem.buyHoldRate)
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(evalItem.backtestRate)
                createCell.cellStyle = commaStyle

                // 일일 수익률
                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(evalItem.buyHoldYield)
                createCell.cellStyle = percentStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(evalItem.backtestYield)
                createCell.cellStyle = percentStyle

                // Maxdrawdown
                createCell = row.createCell(cellIdx++)
                createCell.setCellValue((evalItem.buyHoldRate - buyAndHoldMax) / buyAndHoldMax)
                createCell.cellStyle = percentStyle

                createCell = row.createCell(cellIdx)
                createCell.setCellValue((evalItem.backtestRate - backtestMax) / backtestMax)
                createCell.cellStyle = percentStyle
            }
            sheet.createFreezePane(0, 1)
            sheet.defaultColumnWidth = 20
            ExcelStyle.applyAllBorder(sheet)
            ExcelStyle.applyDefaultFont(sheet)
            return sheet
        }

        /**
         * 기간별(년,월) 수익률
         */
        fun createReportRangeReturn(
            yieldHistory: List<YieldRateItem>,
            workbook: XSSFWorkbook
        ): XSSFSheet {
            val sheet = workbook.createSheet()
            val header = "날짜,Buy&Hold 수익률,백테스트 수익률"
            applyHeader(sheet, header)
            var rowIdx = 1

            val dateStyle = ExcelStyle.createYearMonth(workbook)
            val percentStyle = ExcelStyle.createPercent(workbook)

            yieldHistory.forEach { monthYield ->
                val row = sheet.createRow(rowIdx++)
                var cellIdx = 0
                var createCell = row.createCell(cellIdx++)
                createCell.setCellValue(monthYield.baseDate)
                createCell.cellStyle = dateStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(monthYield.buyHoldYield)
                createCell.cellStyle = percentStyle

                createCell = row.createCell(cellIdx)
                createCell.setCellValue(monthYield.backtestYield)
                createCell.cellStyle = percentStyle
            }
            sheet.createFreezePane(0, 1)
            sheet.defaultColumnWidth = 20
            ExcelStyle.applyAllBorder(sheet)
            ExcelStyle.applyDefaultFont(sheet)
            return sheet
        }


        /**
         * [specialInfo]: 분석 조건별 특별한 정보
         */
        fun createSummary(
            commonAnalysisReportResult: CommonAnalysisReportResult,
            tradeConditionList: List<ConditionEntity>,
            tradeCondition: TradeCondition,
            specialInfo: String
        ): String {
            val report = StringBuilder()
            report.append("----------- Buy&Hold 결과 -----------\n")
            report.append(String.format("합산 동일비중 수익\t %,.2f%%", commonAnalysisReportResult.buyHoldYieldTotal.yield * 100))
                .append("\n")
            report.append(String.format("합산 동일비중 MDD\t %,.2f%%", commonAnalysisReportResult.buyHoldYieldTotal.mdd * 100)).append("\n")
            report.append(String.format("합산 동일비중 CAGR\t %,.2f%%", commonAnalysisReportResult.buyHoldYieldTotal.getCagr() * 100))
                .append("\n")
            report.append(String.format("샤프지수\t %,.2f", commonAnalysisReportResult.getBuyHoldSharpeRatio())).append("\n")

            for (i in 1..tradeConditionList.size) {
                val condition = tradeConditionList[i - 1]
                report.append(
                    "${i}. 조건번호\t${condition.conditionSeq}\n"
                )
                val sumYield = commonAnalysisReportResult.buyHoldYieldCondition[condition.stock.code]
                if (sumYield == null) {
                    log.warn("조건에 해당하는 결과가 없습니다. vbsConditionSeq: ${condition.conditionSeq}")
                    break
                }
                report.append(String.format("${i}. 동일비중 수익\t %,.2f%%", sumYield.yield * 100)).append("\n")
                report.append(String.format("${i}. 동일비중 MDD\t %,.2f%%", sumYield.mdd * 100)).append("\n")
            }

            val totalYield: CommonAnalysisReportResult.TotalYield = commonAnalysisReportResult.yieldTotal
            report.append("----------- 전략 결과 -----------\n")
            report.append(String.format("합산 실현 수익\t %,.2f%%", totalYield.yield * 100)).append("\n")
            report.append(String.format("합산 실현 MDD\t %,.2f%%", totalYield.mdd * 100)).append("\n")
            report.append(String.format("합산 매매회수\t %d", commonAnalysisReportResult.getWinningRateTotal().getTradeCount())).append("\n")
            report.append(String.format("합산 승률\t %,.2f%%", commonAnalysisReportResult.getWinningRateTotal().getWinRate() * 100))
                .append("\n")
            report.append(String.format("합산 CAGR\t %,.2f%%", totalYield.getCagr() * 100)).append("\n")
            report.append(String.format("샤프지수\t %,.2f", commonAnalysisReportResult.getBacktestSharpeRatio())).append("\n")

            for (i in 1..tradeConditionList.size) {
                val condition = tradeConditionList[i - 1]
                report.append(
                    "${i}. 조건번호\t${condition.conditionSeq}\n"
                )

                val winningRate = commonAnalysisReportResult.winningRateCondition[condition.stock.code]
                if (winningRate == null) {
                    log.warn("조건에 해당하는 결과가 없습니다. vbsConditionSeq: ${condition.conditionSeq}")
                    break
                }
                report.append(String.format("${i}. 실현 수익\t %,f", winningRate.invest)).append("\n")
                report.append(String.format("${i}. 매매회수\t %d", winningRate.getTradeCount())).append("\n")
                report.append(String.format("${i}. 승률\t %,.2f%%", winningRate.getWinRate() * 100)).append("\n")
            }

            val range: DateRange = tradeCondition.range

            report.append("----------- 백테스트 조건 -----------\n")
            report.append(String.format("분석기간\t %s", range)).append("\n")
            report.append(String.format("투자비율\t %,.2f%%", tradeCondition.investRatio * 100)).append("\n")
            report.append(String.format("최초 투자금액\t %,f", tradeCondition.cash)).append("\n")
            report.append(String.format("매수 수수료\t %,.2f%%", tradeCondition.feeBuy * 100)).append("\n")
            report.append(String.format("매도 수수료\t %,.2f%%", tradeCondition.feeSell * 100)).append("\n")
            report.append(specialInfo)
            return report.toString()
        }

        /**
         * @return 전체 투자 종목에 대한 Buy & Hold시 수익 정보
         */
        fun calculateTotalBuyAndHoldYield(
            evaluationRateList: List<EvaluationRateItem>,
            range: DateRange
        ): CommonAnalysisReportResult.TotalYield {
            val rateList = evaluationRateList.map { it.buyHoldRate }.toList()
            return CommonAnalysisReportResult.TotalYield(
                ApplicationUtil.getYield(rateList),
                ApplicationUtil.getMdd(rateList),
                range.diffDays.toInt()
            )
        }

        /**
         * @return 수익률 정보
         */
        fun calculateTotalYield(
            evaluationAmountList: List<EvaluationRateItem>, range: DateRange
        ): CommonAnalysisReportResult.TotalYield {
            if (evaluationAmountList.isEmpty()) {
                return CommonAnalysisReportResult.TotalYield(
                    yield = 0.0, mdd = 0.0, dayCount = range.diffDays.toInt()
                )
            }

            val lastCash = evaluationAmountList.last().backtestRate
            val startCash = evaluationAmountList.first().backtestRate
            val realYield = ApplicationUtil.getYield(startCash, lastCash)

            val finalResultList = evaluationAmountList.stream().map(EvaluationRateItem::backtestRate).toList()
            val realMdd = ApplicationUtil.getMdd(finalResultList)
            return CommonAnalysisReportResult.TotalYield(realYield, realMdd, range.diffDays.toInt())
        }

        /**
         * [currentBuyStockCount] 현재 매수중인 종목 수
         * [cash] 현재 보유 현금
         * [stockBuyTotalCount] 매매 대상 종목수
         * [investRatio] 전체 현금 대비 투자 비율. 1: 모든 현금을 투자, 0.5 현금의 50%만 매수에 사용
         *
         * @return 매수에 사용될 금액 반환
         */
        fun getBuyCash(
            currentBuyStockCount: Int,
            cash: Double,
            stockBuyTotalCount: Int,
            investRatio: Double
        ): Double {
            // 매수에 사용할 현금
            // 현재현금 * 직전 매수 종목 수 / 매매 대상 종목수 * 사용비율 * 매매 대상 종목수  / 사용비율 / (매매 대상 종목수 / 사용비율 - 직전 매수 종목 수) + 현재현금
            val startCash =
                cash * currentBuyStockCount / stockBuyTotalCount * investRatio * stockBuyTotalCount / investRatio / (stockBuyTotalCount / investRatio - currentBuyStockCount) + cash
            // 매수에 사용할 현금 = 시작현금 역산 * 사용비율 * (1/매매종목수)
            return startCash * investRatio * (1 / stockBuyTotalCount.toDouble())
        }

        fun textToSheet(summary: String, sheet: XSSFSheet) {
            val lines = summary.split("\n")
            sheet.createRow(sheet.physicalNumberOfRows)

            for (rowIdx in lines.indices) {
                val row = sheet.createRow(sheet.physicalNumberOfRows)
                val columns = lines[rowIdx].split("\t")

                for (colIdx in columns.indices) {
                    val colVal = columns[colIdx]
                    val cell = row.createCell(colIdx)
                    cell.setCellValue(colVal)
                    cell.cellStyle = ExcelStyle.createDefault(sheet.workbook)
                }
            }
        }


        fun applyHeader(
            sheet: XSSFSheet,
            header: List<String>,
        ) {
            val rowHeader = sheet.createRow(0)
            for (cellIdx in header.indices) {
                val cell = rowHeader.createCell(cellIdx)
                cell.setCellValue(header[cellIdx])
                cell.cellStyle = ExcelStyle.createHeaderRow(sheet.workbook)
            }
        }

        fun applyHeader(
            sheet: XSSFSheet,
            header: String,
        ) {
            val headerTxt = header.split(",")
            applyHeader(sheet, headerTxt)
        }

        /**
         * @return 조건 정보가 담긴 리포트 파일명 suffix
         */
        fun getReportFileSuffix(tradeCondition: TradeCondition, stockCodes: List<String>, append: String = ""): String {
            return String.format(
                "%s~%s%s.xlsx",
                tradeCondition.range.fromDateFormat,
                tradeCondition.range.toDateFormat,
                append,
                stockCodes.joinToString(",") { it },
            )
        }
    }

    /**
     * 엑셀 리포트에 사용될 셀 스타일 모음
     */
    object ExcelStyle {
        fun createDefault(workbook: XSSFWorkbook): XSSFCellStyle {
            return workbook.createCellStyle()
        }

        fun createDate(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            val createHelper: CreationHelper = workbook.creationHelper
            cellStyle.dataFormat = createHelper.createDataFormat().getFormat("yyyy/MM/dd")
            return cellStyle
        }

        fun createYearMonth(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            val createHelper: CreationHelper = workbook.creationHelper
            cellStyle.dataFormat = createHelper.createDataFormat().getFormat("yyyy/MM")
            return cellStyle
        }

        fun createComma(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            val format: DataFormat = workbook.createDataFormat()
            cellStyle.dataFormat = format.getFormat("###,###")
            return cellStyle
        }

        /**
         * 소수점 표시
         */
        fun createDecimal(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            val format: DataFormat = workbook.createDataFormat()
            cellStyle.dataFormat = format.getFormat("0.00")
            return cellStyle
        }

        fun createCommaDecimal(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            val format: DataFormat = workbook.createDataFormat()
            cellStyle.dataFormat = format.getFormat("###,###.00")
            return cellStyle
        }

        fun createPercent(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            val format: DataFormat = workbook.createDataFormat()
            cellStyle.dataFormat = format.getFormat("###,##0.00%")
            return cellStyle
        }

        fun createHeaderRow(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            cellStyle.fillForegroundColor = IndexedColors.YELLOW.index

            val font: XSSFFont = workbook.createFont()
            font.bold = true
            cellStyle.setFont(font)
            cellStyle.alignment = HorizontalAlignment.CENTER
            cellStyle.verticalAlignment = VerticalAlignment.CENTER
            return cellStyle
        }

        /**
         * 모든 셀 border 적용
         */
        fun applyAllBorder(sheet: XSSFSheet) {
            val rowCount = sheet.physicalNumberOfRows
            for (rowIdx in 0 until rowCount) {
                val row = sheet.getRow(rowIdx)
                val cellCount = row.physicalNumberOfCells
                for (cellIdx in 0 until cellCount) {
                    val cell = row.getCell(cellIdx)
                    val cellStyle = cell.cellStyle
                    cellStyle.borderBottom = BorderStyle.THIN
                    cellStyle.borderTop = BorderStyle.THIN
                    cellStyle.borderRight = BorderStyle.THIN
                    cellStyle.borderLeft = BorderStyle.THIN
                }
            }
        }

        fun applyDefaultFont(sheet: XSSFSheet) {
            val rowCount = sheet.physicalNumberOfRows
            for (rowIdx in 0 until rowCount) {
                val row = sheet.getRow(rowIdx)
                val cellCount = row.physicalNumberOfCells
                for (cellIdx in 0 until cellCount) {
                    val cellStyle = row.getCell(cellIdx).cellStyle
                    cellStyle.font.fontName = "맑은 고딕"
                }
            }
        }
    }
}