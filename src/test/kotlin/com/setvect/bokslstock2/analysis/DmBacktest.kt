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
        val from = LocalDateTime.of(2011, 1, 1, 0, 0)
        val to = LocalDateTime.of(2022, 3, 31, 0, 0)
        val realRange = DateRange(from, to)

        val basic = TradeCondition(
            range = realRange,
            investRatio = 0.999,
            cash = 10_000_000.0,
            feeBuy = 0.001,
            feeSell = 0.001,
            comment = ""
        )

        val timeWeights = listOf(
            hashMapOf(
                1 to 1.0
            ),
            hashMapOf(
                2 to 1.0
            ),
            hashMapOf(
                3 to 1.0
            ),
            hashMapOf(
                5 to 1.0
            ),
            hashMapOf(
                6 to 1.0
            ),
            hashMapOf(
                7 to 1.0
            ),
            hashMapOf(
                8 to 1.0
            ),
            hashMapOf(
                9 to 1.0
            ),
            hashMapOf(
                10 to 1.0
            ),
            hashMapOf(
                11 to 1.0
            ),
            hashMapOf(
                12 to 1.0
            ),
            hashMapOf(
                1 to 0.33,
                3 to 0.33,
                6 to 0.34
            ),
            hashMapOf(
                2 to 0.33,
                4 to 0.33,
                7 to 0.34
            ),
            hashMapOf(
                3 to 0.33,
                5 to 0.33,
                8 to 0.34
            ),
            hashMapOf(
                4 to 0.33,
                6 to 0.33,
                9 to 0.34
            ),
            hashMapOf(
                1 to 0.5,
                3 to 0.5,
            ),
            hashMapOf(
                2 to 0.5,
                4 to 0.5,
            ),
            hashMapOf(
                3 to 0.5,
                5 to 0.5,
            ),
            hashMapOf(
                4 to 0.5,
                6 to 0.5,
            ),
            hashMapOf(
                5 to 0.5,
                7 to 0.5,
            ),
        )

        val conditions = timeWeights.map {
            DmBacktestCondition(
                tradeCondition = basic,
                stockCodes = listOf(StockCode.OS_CODE_SPY, StockCode.OS_CODE_VSS),
                holdCode = null,
                periodType = PeriodType.PERIOD_MONTH,
                timeWeight = it,
                endSell = true
            )
        }
//        dmAnalysisService.runTest(condition)
        dmAnalysisService.makeSummaryReport(conditions)


        log.info("끝.")
    }
}