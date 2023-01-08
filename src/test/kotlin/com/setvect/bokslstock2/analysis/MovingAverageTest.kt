package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.analysis.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.analysis.common.service.ReportMakerHelperService.ExcelStyle
import com.setvect.bokslstock2.index.model.PeriodType.*
import com.setvect.bokslstock2.index.service.MovingAverageService
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.io.FileOutputStream

@SpringBootTest
@ActiveProfiles("local")
class MovingAverageTest {
    @Autowired
    private lateinit var movingAverageService: MovingAverageService

    @Test
    fun 이동평균계산() {
        val movingAverage =
            movingAverageService.getMovingAverage(StockCode.KODEX_KOSDAQ_2X_233740, PERIOD_DAY, PERIOD_WEEK, listOf(1))

        movingAverage.forEach {
            val avgInfo = it.average.entries
                .map { entry -> "이동평균(${entry.key}): ${it.average[entry.key]}" }
                .toList()
                .joinToString(", ")
            println("${it.candleDateTimeStart}~${it.candleDateTimeEnd} - O: ${it.openPrice}, H: ${it.highPrice}, L: ${it.lowPrice}, C:${it.closePrice}, ${it.periodType}, $avgInfo")
        }
    }

    @Test
    fun 이동평균계산_엑셀_내보내기() {
        val periodType = PERIOD_MONTH
        val stockCode = StockCode.KODEX_KOSDAQ_2X_233740
        val movingAverage =
            movingAverageService.getMovingAverage(stockCode, PERIOD_DAY, periodType, listOf(1))

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet()!!
        ReportMakerHelperService.applyHeader(sheet, "날짜,시가,고가,저가,종가,범위")

        var rowIdx = 1
        val defaultStyle = ExcelStyle.createDate(workbook)
        val commaStyle = ExcelStyle.createComma(workbook)
        val dateStyle = ExcelStyle.createDate(workbook)

        movingAverage.forEach {
            var cellIdx = 0
            val row = sheet.createRow(rowIdx++)!!
            var cell = row.createCell(cellIdx++)!!
            cell.setCellValue(it.candleDateTimeStart)
            cell.cellStyle = dateStyle

            cell = row.createCell(cellIdx++)!!
            cell.setCellValue(it.openPrice)
            cell.cellStyle = commaStyle

            cell = row.createCell(cellIdx++)!!
            cell.setCellValue(it.highPrice)
            cell.cellStyle = commaStyle

            cell = row.createCell(cellIdx++)!!
            cell.setCellValue(it.lowPrice)
            cell.cellStyle = commaStyle

            cell = row.createCell(cellIdx++)!!
            cell.setCellValue(it.closePrice)
            cell.cellStyle = commaStyle

            cell = row.createCell(cellIdx++)!!
            cell.setCellValue("${it.candleDateTimeStart}~${it.candleDateTimeEnd}")
            cell.cellStyle = defaultStyle
        }

        sheet.createFreezePane(0, 1)
        sheet.defaultColumnWidth = 20
        ExcelStyle.applyAllBorder(sheet)
        ExcelStyle.applyDefaultFont(sheet)

        val reportFile =
            File("./temp", "주가정보_${stockCode.desc}(${stockCode})_${periodType}.xlsx")
        FileOutputStream(reportFile).use { ous ->
            workbook.write(ous)
        }
        println("$reportFile 저장 끝.")
    }
}