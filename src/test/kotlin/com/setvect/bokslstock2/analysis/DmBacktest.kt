package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.StockCode
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.dm.model.DmBacktestCondition
import com.setvect.bokslstock2.analysis.dm.serivce.DmAnalysisService
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.util.DateRange
import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class DmBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var dmAnalysisService: DmAnalysisService

    @Test
    fun 일회성_백테스팅_리포트_만듦() {
        val from = LocalDateTime.of(2018, 12, 1, 0, 0)
        val to = LocalDateTime.of(2018, 12, 31, 0, 0)
        val realRange = DateRange(from, to)

        val basic = TradeCondition(
            range = realRange,
            investRatio = 0.999,
            cash = 100_000_000.0,
            feeBuy = 0.0000,
            feeSell = 0.0000,
            comment = ""
        )

        val condition = DmBacktestCondition(
            tradeCondition = basic,
            stockCodes = listOf(StockCode.OS_CODE_SPY),
            holdCode = StockCode.OS_CODE_TLT,
            periodType = PeriodType.PERIOD_MONTH,
            timeWeight = hashMapOf(
                1 to 1.0,
//                3 to 0.33,
//                6 to 0.34
            ),
            endSell = true
        )
        dmAnalysisService.runTest(condition)
        log.info("끝.")
    }
}