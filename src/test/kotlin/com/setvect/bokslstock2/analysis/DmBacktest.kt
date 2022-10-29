package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.dm.model.DmBacktestCondition
import com.setvect.bokslstock2.analysis.dm.service.DmAnalysisService
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.util.DateRange
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("local")
class DmBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var dmAnalysisService: DmAnalysisService

    @Test
    fun 일회성_백테스팅_리포트_만듦() {
//        val from = LocalDateTime.of(2022, 7, 1, 0, 0)
        val from = LocalDateTime.of(2018, 1, 1, 0, 0)
//        val from = LocalDateTime.of(2022, 4, 1, 0, 0)
        val to = LocalDateTime.of(2022, 10, 1, 0, 0)
//        val to = LocalDateTime.now()
        val realRange = DateRange(from, to)

        val basic = TradeCondition(
            range = realRange,
            investRatio = 0.999,
            cash = 10_000_000.0,
            feeBuy = 0.001,
            feeSell = 0.001,
            comment = "",
            benchmark = listOf(StockCode.OS_CODE_SPY)
        )

        val timeWeights = listOf(
//            hashMapOf(
//                1 to 1.0
//            ),
//            hashMapOf(
//                2 to 1.0
//            ),
//            hashMapOf(
//                3 to 1.0
//            ),
//            hashMapOf(
//                5 to 1.0
//            ),
//            hashMapOf(
//                6 to 1.0
//            ),
//            hashMapOf(
//                7 to 1.0
//            ),
//            hashMapOf(
//                8 to 1.0
//            ),
//            hashMapOf(
//                9 to 1.0
//            ),
//            hashMapOf(
//                10 to 1.0
//            ),
//            hashMapOf(
//                11 to 1.0
//            ),
//            hashMapOf(
//                12 to 1.0
//            ),
            hashMapOf(
                1 to 0.33,
                3 to 0.33,
                6 to 0.34
            ),
//            hashMapOf(
//                2 to 0.33,
//                4 to 0.33,
//                7 to 0.34
//            ),
//            hashMapOf(
//                3 to 0.33,
//                5 to 0.33,
//                8 to 0.34
//            ),
//            hashMapOf(
//                4 to 0.33,
//                6 to 0.33,
//                9 to 0.34
//            ),
//            hashMapOf(
//                1 to 0.5,
//                3 to 0.5,
//            ),
//            hashMapOf(
//                2 to 0.5,
//                4 to 0.5,
//            ),
//            hashMapOf(
//                3 to 0.5,
//                5 to 0.5,
//            ),
//            hashMapOf(
//                4 to 0.5,
//                6 to 0.5,
//            ),
//            hashMapOf(
//                5 to 0.5,
//                7 to 0.5,
//            ),
        )

        val conditions = timeWeights.map {
            DmBacktestCondition(
                tradeCondition = basic,
                stockCodes = listOf(StockCode.OS_CODE_SPY, StockCode.OS_CODE_SCZ),
                holdCode = StockCode.OS_CODE_TLT,
                periodType = PeriodType.PERIOD_MONTH,
                timeWeight = it,
                endSell = true
            )
        }
        conditions.forEach {
            dmAnalysisService.runTest(it)
        }
//        dmAnalysisService.makeSummaryReport(conditions)


        log.info("끝.")
    }

    @Test
    fun momentumScore() {
        val date = LocalDate.of(2022, 5, 1)
        val momentumScore = dmAnalysisService.getMomentumScore(
            date, listOf(StockCode.OS_CODE_SPY, StockCode.OS_CODE_SCZ), StockCode.OS_CODE_TLT, hashMapOf(
                1 to 0.33,
                3 to 0.33,
                6 to 0.34
            )
        )
        println(momentumScore)
    }
}