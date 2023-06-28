package com.setvect.bokslstock2.migration

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.trade.entity.TradeEntity
import com.setvect.bokslstock2.koreainvestment.trade.repository.TradeRepository
import com.setvect.bokslstock2.util.ApplicationUtil
import org.apache.commons.codec.digest.DigestUtils
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.io.File

@SpringBootTest
@ActiveProfiles("test")
class TradeHistoryMigration {
    private val log = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var tradeRepository: TradeRepository

    @Autowired
    private lateinit var bokslStockProperties: BokslStockProperties

    @Test
    fun migration() {
        val file = File("./temp/거래내역.xls.xlsx")
        WorkbookFactory.create(file).use {
            val sheet = it.getSheetAt(0)

            var rowCount = 3
            while (true) {
                val row = sheet.getRow(rowCount) ?: break
                if (row.getCell(0).localDateTimeCellValue == null) {
                    break
                }
                rowCount++

                // buy

                // 매수 시간 오전 10시로 고정
                val buyDate = row.getCell(0).localDateTimeCellValue.withHour(10)
                val stockName = row.getCell(1).stringCellValue
                val stockCode = findStock(stockName)
                val qty = row.getCell(2).numericCellValue.toInt()
                val buyUnitPrice = row.getCell(3).numericCellValue
                val memo = row.getCell(15).stringCellValue

                log.info("rowCount: $rowCount")
                log.info(
                    """
                        === buy ===
                        buyDate: $buyDate
                        stockName: $stockName
                        stockCode: ${stockCode.code}
                        qty: $qty
                        buyUnitPrice: $buyUnitPrice 
                        memo: $memo
                    """.trimIndent()
                )

                val buyTrade = TradeEntity(
                    account = DigestUtils.md5Hex(bokslStockProperties.koreainvestment.vbs.accountNo),
                    code = stockCode.code,
                    tradeType = TradeType.BUY,
                    qty = qty,
                    unitPrice = buyUnitPrice,
                    yield = 0.0,
                    regDate = buyDate
                )
                tradeRepository.save(buyTrade)

                // ============== sell

                // 매도 시간은 오전 9시로 고정
                val sellDate = row.getCell(8).localDateTimeCellValue?.withHour(9) ?: break
                val sellUnitPrice = row.getCell(11).numericCellValue
                log.info(
                    """
                        === sell ===
                        sellDate: $sellDate
                        sellUnitPrice: $sellUnitPrice
                        memo: $memo
                    """.trimIndent()
                )

                val sellTrade = TradeEntity(
                    account = DigestUtils.md5Hex(bokslStockProperties.koreainvestment.vbs.accountNo),
                    code = stockCode.code,
                    tradeType = TradeType.SELL,
                    qty = qty,
                    unitPrice = sellUnitPrice,
                    yield = ApplicationUtil.getYield(buyUnitPrice, sellUnitPrice),
                    regDate = sellDate
                )
                tradeRepository.save(sellTrade)
            }
        }
    }

    private fun findStock(stockName: String): StockCode {
        return StockCode.values().first { it.desc == stockName }
    }
}