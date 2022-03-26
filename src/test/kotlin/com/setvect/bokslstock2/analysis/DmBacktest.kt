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
        val from = LocalDateTime.of(2016, 6, 1, 0, 0)
        val to = LocalDateTime.now()
        val realRange = DateRange(from, to)

        val basic = TradeCondition(
            range = realRange,
            investRatio = 0.99,
            cash = 10_000_000.0,
            feeBuy = 0.00015,
            feeSell = 0.00015,
            comment = ""
        )

        val condition = DmBacktestCondition(
            tradeCondition = basic,
            stockCodes = listOf(StockCode.CODE_KODEX_200_069500),
            holdCode = StockCode.CODE_KODEX_SHORT_BONDS_153130,
            periodType = PeriodType.PERIOD_MONTH,
            timeWeight = hashMapOf(
                1 to 0.33,
                3 to 0.33,
                6 to 0.34
            )
        )
        dmAnalysisService.runTest(condition)
        log.info("끝.")
    }


}