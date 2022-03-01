package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.StockCode
import com.setvect.bokslstock2.analysis.common.model.BasicAnalysisCondition
import com.setvect.bokslstock2.analysis.dm.model.DmAnalysisCondition
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
        val realRange = DateRange(LocalDateTime.of(2016, 5, 1, 0, 0), LocalDateTime.now())

        val basic = BasicAnalysisCondition(
            range = realRange,
            investRatio = 0.99,
            cash = 10_000_000.0,
            feeBuy = 0.00015,
            feeSell = 0.00015,
            comment = ""
        )

        val condition = DmAnalysisCondition(
            basic = basic,
            stockCodes = listOf(StockCode.CODE_KODEX_KOSDAQ_2X_233740),
            holdCode = null,
            periodType = PeriodType.PERIOD_QUARTER,
            timeWeight = hashMapOf(
                1 to 100
            )
        )
        dmAnalysisService.runTest(condition)
    }


}