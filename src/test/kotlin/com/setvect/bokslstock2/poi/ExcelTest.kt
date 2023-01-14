package com.setvect.bokslstock2.poi

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Test
import java.io.FileOutputStream

class ExcelTest {
    @Test
    fun makeExcel() {
        val workBook = XSSFWorkbook()
        val sheet = workBook.createSheet()
        val row = sheet.createRow(0)

        val cell = row.createCell(0)
        cell.setCellValue("test")
        val fileOutputStream = FileOutputStream("test.xlsx")

        workBook.write(fileOutputStream)

        fileOutputStream.close()

    }
}