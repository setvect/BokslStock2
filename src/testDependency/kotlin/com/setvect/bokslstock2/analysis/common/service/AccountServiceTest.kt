package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.util.DateUtil
import com.setvect.bokslstock2.util.JsonUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.io.FileOutputStream

@SpringBootTest
@ActiveProfiles("test")
class AccountServiceTest {
    @Autowired
    private lateinit var accountService: AccountService
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun getEvalPrice() {
    }

    @Test
    fun calculateTradeResult() {
        // given
        accountService.accountCondition = AccountService.AccountCondition(1_000_000.0, 0.00015, 0.00015)
        accountService.addTradeHistory(
            AccountService.TradeNeo(
                stockCode = StockCode.KODEX_200_069500,
                tradeType = TradeType.BUY,
                price = 10_000.0,
                qty = 10,
                tradeDate = DateUtil.getLocalDateTime("2023-01-02T10:00:00"),
                memo = "ㅋㅋ"
            )
        )
        accountService.addTradeHistory(
            AccountService.TradeNeo(
                stockCode = StockCode.KODEX_200_069500,
                tradeType = TradeType.SELL,
                price = 11_000.0,
                qty = 7,
                tradeDate = DateUtil.getLocalDateTime("2023-01-03T09:00:00"),
                memo = "ㅋㅋ"
            )
        )
        accountService.addTradeHistory(
            AccountService.TradeNeo(
                stockCode = StockCode.KODEX_200_069500,
                tradeType = TradeType.BUY,
                price = 11_500.0,
                qty = 2,
                tradeDate = DateUtil.getLocalDateTime("2023-01-04T09:00:00"),
                memo = "ㅋㅋ"
            )
        )
        accountService.addTradeHistory(
            AccountService.TradeNeo(
                stockCode = StockCode.KODEX_200_069500,
                tradeType = TradeType.SELL,
                price = 12_000.0,
                qty = 5,
                tradeDate = DateUtil.getLocalDateTime("2023-01-11T09:00:00"),
                memo = "ㅋㅋ"
            )
        )

        // when
        val result = accountService.calculateTradeResult()

        // then
        val json = JsonUtil.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result)
        log.info(json)

        val reportFile = File("./temp", "테스트.xlsx")
        accountService.makeReport(result, reportFile)
        log.info("보고서 생성: ${reportFile.absolutePath}")
    }
}