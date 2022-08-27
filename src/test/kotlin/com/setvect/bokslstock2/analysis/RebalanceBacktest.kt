package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.StockCode
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.rebalance.model.RebalanceBacktestCondition
import com.setvect.bokslstock2.analysis.rebalance.service.RebalanceAnalysisService
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.util.DateRange
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("local")
class RebalanceBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var rebalanceAnalysisService: RebalanceAnalysisService

    @Test
    fun 일회성_백테스팅_리포트_만듦() {
        val from = LocalDateTime.of(2020, 1, 1, 0, 0)
        val to = LocalDateTime.of(2021, 1, 1, 0, 0)
        val realRange = DateRange(from, to)

        val basic = TradeCondition(
            range = realRange,
            investRatio = 0.999,
            cash = 20_000.0,
            feeBuy = 0.0001,
            feeSell = 0.0001,
            comment = "",
            benchmark = listOf(StockCode.OS_CODE_SSO)
        )

        val timeWeights = listOf(
            RebalanceBacktestCondition.RebalanceFacter(PeriodType.PERIOD_MONTH, 0.0),
        )

        val conditions = timeWeights.map {
            RebalanceBacktestCondition(
                tradeCondition = basic,
                stockCodes = listOf(
                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_SSO, 50),
                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_UBT, 25),
                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_UGL, 25),

//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_SSO, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_UBT, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_UGL, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_SHY, 25),
                ),
                rebalanceFacter = it,
            )
        }
        rebalanceAnalysisService.makeSummaryReport(conditions)
    }
}