package com.setvect.bokslstock2.migration

import com.setvect.bokslstock2.analysis.common.model.StockCode
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.io.File

@SpringBootTest
@ActiveProfiles("local")
class TradeHistoryMigration {
    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun migration() {
        val file = File("./temp/거래내역.xls.xlsx")
        WorkbookFactory.create(file).use {
            val sheet = it.getSheetAt(0)

            var rowCount = 3
            while (true) {
                val row = sheet.getRow(rowCount) ?: break
                rowCount++

                // 매수 시간 오전 10시로 고정
                val buyDate = row.getCell(0).localDateTimeCellValue.withHour(10)
                val stockName = row.getCell(1).stringCellValue
                val stockCode = findStock(stockName)
                val qty = row.getCell(2).numericCellValue.toLong()
                val buyUnitPrice = row.getCell(3).numericCellValue.toLong()

                log.info("rowCount: $rowCount")
                log.info(
                    """
                        === buy ===
                        buyDate: $buyDate
                        stockName: $stockName
                        stockCode: ${stockCode.code}
                        qty: $qty
                        buyUnitPrice: $buyUnitPrice 
                    """.trimIndent()
                )

                // 매도 시간은 오전 9시로 고정
                val sellDate = row.getCell(8).localDateTimeCellValue?.withHour(9) ?: break
                val sellUnitPrice = row.getCell(11).numericCellValue.toLong()
                log.info(
                    """
                        === sell ===
                        sellDate: $sellDate
                        sellUnitPrice: $sellUnitPrice
                    """.trimIndent()
                )
            }
        }
    }

    private fun findStock(stockName: String): StockCode {
        return StockCode.values().first { it.desc == stockName }
    }
}