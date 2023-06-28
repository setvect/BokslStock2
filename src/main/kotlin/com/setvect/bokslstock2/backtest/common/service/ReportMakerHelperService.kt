package com.setvect.bokslstock2.backtest.common.service

import com.setvect.bokslstock2.backtest.common.model.*
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * 리포트 생성에 필요한 공통 메소드 제공
 */
object ReportMakerHelperService {
    private val log: Logger = LoggerFactory.getLogger(ReportMakerHelperService::class.java)

    /**
     * 매매 내역을 시트로 만듦
     */
    fun createTradeReport(result: List<TradeResult>, workbook: XSSFWorkbook): XSSFSheet {
        val sheet = workbook.createSheet()
        val header =
            "날짜,종목,백테스트 조건,매매 구분,매매 수량,매매 금액,체결 가격,실현 수익률,수수료,투자 수익(수수료 제외)," +
                    "보유 주식 평가금,매매후 보유 현금,평가금(주식+현금),수익비,메모"
        applyHeader(sheet, header)
        var rowIdx = 1

        val defaultStyle = ExcelStyle.createDefault(workbook)
        val dateStyle = ExcelStyle.createDate(workbook)
        val commaStyle = ExcelStyle.createComma(workbook)
        val percentStyle = ExcelStyle.createPercent(workbook)
        val decimalStyle = ExcelStyle.createDecimal(workbook)

        result.forEach { tradeItem ->
            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0
            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.tradeDate.toLocalDate())
            createCell.cellStyle = dateStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue("${tradeItem.stockCode.name}(${tradeItem.stockCode.code})")

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.backtestConditionName)

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.tradeType.name)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.qty.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.price * tradeItem.qty)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.price)
            if (tradeItem.price > 1000) {
                createCell.cellStyle = commaStyle
            } else {
                createCell.cellStyle = decimalStyle
            }

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.yieldRate)
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

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.profitRate)
            createCell.cellStyle = decimalStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.memo)
            createCell.cellStyle = defaultStyle
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
     * 날짜에 따른 평가금액(Buy&Hold, 밴치마크, 벡테스트) 변화 시트 만듦
     */
    fun createReportEvalAmount(
        evaluationAmountHistory: List<EvaluationRateItem>,
        workbook: XSSFWorkbook
    ): XSSFSheet {
        val sheet = workbook.createSheet()
        val header = "날짜," +
                "백테스트 평가금,Buy&Hold 평가금,밴치마크 평가금," +
                "백테스트 일일 수익률,Buy&Hold 일일 수익률,밴치마크 일일 수익률," +
                "백테스트 Maxdrawdown,Buy&Hold Maxdrawdown,밴치마크 Maxdrawdown"
        applyHeader(sheet, header)
        var rowIdx = 1

        val dateStyle = ExcelStyle.createDate(workbook)
        val commaStyle = ExcelStyle.createDecimal(workbook)
        val percentStyle = ExcelStyle.createPercent(workbook)

        var buyAndHoldMax = 0.0
        var benchmarkMax = 0.0
        var backtestMax = 0.0

        evaluationAmountHistory.forEach { evalItem: EvaluationRateItem ->
            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0
            var createCell = row.createCell(cellIdx++)
            buyAndHoldMax = buyAndHoldMax.coerceAtLeast(evalItem.buyHoldRate)
            benchmarkMax = benchmarkMax.coerceAtLeast(evalItem.benchmarkRate)
            backtestMax = backtestMax.coerceAtLeast(evalItem.backtestRate)
            createCell.setCellValue(evalItem.baseDate)
            createCell.cellStyle = dateStyle

            // 변화량
            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(evalItem.backtestRate)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(evalItem.buyHoldRate)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(evalItem.benchmarkRate)
            createCell.cellStyle = commaStyle

            // 일일 수익률
            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(evalItem.backtestYield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(evalItem.buyHoldYield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(evalItem.benchmarkYield)
            createCell.cellStyle = percentStyle

            // Maxdrawdown
            createCell = row.createCell(cellIdx++)
            createCell.setCellValue((evalItem.backtestRate - backtestMax) / backtestMax)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue((evalItem.buyHoldRate - buyAndHoldMax) / buyAndHoldMax)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx)
            createCell.setCellValue((evalItem.benchmarkRate - benchmarkMax) / benchmarkMax)
            createCell.cellStyle = percentStyle
        }
        sheet.createFreezePane(0, 1)
        sheet.defaultColumnWidth = 20
        ExcelStyle.applyAllBorder(sheet)
        ExcelStyle.applyDefaultFont(sheet)
        applyEmphasisHeader(sheet, 1, 4, 7)
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
        val header = "날짜,백테스트 수익률,Buy&Hold 수익률,밴치마크 수익률"
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
            createCell.setCellValue(monthYield.backtestYield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(monthYield.buyHoldYield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx)
            createCell.setCellValue(monthYield.benchmarkYield)
            createCell.cellStyle = percentStyle

        }
        sheet.createFreezePane(0, 1)
        sheet.defaultColumnWidth = 20
        ExcelStyle.applyAllBorder(sheet)
        ExcelStyle.applyDefaultFont(sheet)
        applyEmphasisHeader(sheet, 1)

        return sheet
    }

    fun makeSummaryCompareStock(
        totalYield: CommonAnalysisReportResult.TotalYield,
        sharpeRatio: Double,
        yieldByCode: Map<StockCode, CommonAnalysisReportResult.YieldMdd>
    ): StringBuilder {
        val report = StringBuilder()
        val buyHoldText = ApplicationUtil.makeSummaryCompareStock(totalYield, sharpeRatio)
        report.append(buyHoldText)

        var i = 0
        yieldByCode.entries.forEach {
            i++
            report.append(
                "$i. 종목 코드: ${it.key}\n"
            )
            val sumYield = it.value
            report.append(String.format("$i. 동일비중 수익\t %,.2f%%", sumYield.yield * 100)).append("\n")
            report.append(String.format("$i. 동일비중 MDD\t %,.2f%%", sumYield.mdd * 100)).append("\n")
        }
        return report
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

    private fun applyEmphasisHeader(sheet: XSSFSheet, vararg colIdx: Int) {
        colIdx.forEach { col ->
            val cell = sheet.getRow(0).getCell(col)
            val style = cell.cellStyle
            style.fillForegroundColor = IndexedColors.LIGHT_ORANGE.index
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

        fun createHyperlink(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            val font = workbook.createFont()
            font.underline = XSSFFont.U_SINGLE
            font.color = IndexedColors.BLUE.index
            cellStyle.setFont(font)
            return cellStyle
        }

        /**
         * 모든 셀 border 적용
         */
        fun applyAllBorder(sheet: XSSFSheet) {
            val rowCount = sheet.physicalNumberOfRows

            // 셀 스타일 캐시
            val styleCache = mutableMapOf<Int, XSSFCellStyle>()

            for (rowIdx in 0 until rowCount) {
                val row = sheet.getRow(rowIdx)
                val cellCount = row.physicalNumberOfCells
                for (cellIdx in 0 until cellCount) {
                    val cell = row.getCell(cellIdx)
                    val originCellStyle = cell.cellStyle
                    val originStyleHashCode = originCellStyle.hashCode()

                    // 기존 캐시에서 스타일 검색
                    var newCellStyle = styleCache[originStyleHashCode]

                    // 새 스타일이 캐시에 없을 경우 생성 및 저장
                    if (newCellStyle == null) {
                        newCellStyle = sheet.workbook.createCellStyle()
                        newCellStyle.cloneStyleFrom(originCellStyle)
                        newCellStyle.borderBottom = BorderStyle.THIN
                        newCellStyle.borderTop = BorderStyle.THIN
                        newCellStyle.borderRight = BorderStyle.THIN
                        newCellStyle.borderLeft = BorderStyle.THIN
                        styleCache[originStyleHashCode] = newCellStyle
                    }

                    cell.cellStyle = newCellStyle
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

