package com.setvect.bokslstock2.backtest.common.service

import org.apache.poi.xssf.usermodel.XSSFWorkbook

@FunctionalInterface
interface SheetAppendMaker {
    /**
     * 결과 리포트에 시트를 추가할 때 사용
     */
    fun appendSheet(workbook: XSSFWorkbook)

    companion object {
        fun nothing(): SheetAppendMaker {
            return object : SheetAppendMaker {
                override fun appendSheet(workbook: XSSFWorkbook) {
                }
            }
        }
    }
}