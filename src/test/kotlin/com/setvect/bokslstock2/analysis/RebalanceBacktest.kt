package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.common.model.StockCode
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

    
    // TODO 전체적으로 맞는지 확인
    
    @Test
    fun 일회성_백테스팅_리포트_만듦() {
        val realRange = DateRange("2002-08-01T00:00:00", "2023-01-01T00:00:00")

        val basic = TradeCondition(
            range = realRange,
            investRatio = 0.999,
            cash = 20_000_000.0,
            feeBuy = 0.001,
            feeSell = 0.001,
            comment = "",
            benchmark = listOf(StockCode.OS_CODE_SPY)
//            benchmark = listOf(StockCode.TIGER_200_102110)
        )

        val timeWeights = listOf(
            RebalanceBacktestCondition.RebalanceFacter(PeriodType.PERIOD_MONTH, 0.1),
        )

        val conditions = timeWeights.map {
            RebalanceBacktestCondition(
                tradeCondition = basic,
                stockCodes = listOf(

//                    RebalanceBacktestCondition.TradeStock(StockCode.TIGER_NASDAQ_133690, 100),
//                    RebalanceBacktestCondition.TradeStock(StockCode.EXCHANGE_DOLLAR, 50),

//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_SSO, 50),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_UBT, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_UGL, 25),

//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_TQQQ, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_TMF, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_UGL, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_SHY, 25),

                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_QQQ, 25),
                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_TLT, 25),
                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_GLD, 25),
                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_SHY, 25),
                ),
                rebalanceFacter = it,
            )
        }
        rebalanceAnalysisService.makeSummaryReport(conditions)
    }
}