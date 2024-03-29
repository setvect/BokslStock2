package com.setvect.bokslstock2.backtest

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.backtest.common.service.AccountService
import com.setvect.bokslstock2.backtest.common.service.StockCommonFactory
import com.setvect.bokslstock2.backtest.rebalance.model.RebalanceBacktestCondition
import com.setvect.bokslstock2.backtest.rebalance.service.RebalanceBacktestService
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.util.DateRange
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.io.File

@SpringBootTest
@ActiveProfiles("test")
class RebalanceBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var stockCommonFactory: StockCommonFactory

    @Autowired
    private lateinit var rebalanceBacktestService: RebalanceBacktestService

    @Test
    fun 일회성_백테스팅_리포트_만듦() {
        val range = DateRange("2002-08-01T00:00:00", "2023-03-01T00:00:00")

        val rebalanceBacktestCondition = RebalanceBacktestCondition(
            range = range,
            cash = 20_000_000.0,
            stockByWeight = listOf(

//                    RebalanceBacktestCondition.TradeStock(StockCode.TIGER_NASDAQ_133690, 100),
//                    RebalanceBacktestCondition.TradeStock(StockCode.EXCHANGE_DOLLAR, 50),

//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_SSO, 50),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_UBT, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_UGL, 25),

//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_TQQQ, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_TMF, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_UGL, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_SHY, 25),

//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_QQQ, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_TLT, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_GLD, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.OS_CODE_SHY, 25),

//                    RebalanceBacktestCondition.TradeStock(StockCode.TIGER_SNP_360750, 17),
//                    RebalanceBacktestCondition.TradeStock(StockCode.KOSEF_200TR_294400, 18),
//                    RebalanceBacktestCondition.TradeStock(StockCode.KODEX_GLD_H_132030, 15),
//                    RebalanceBacktestCondition.TradeStock(StockCode.KOSEF_TREASURY_BOND_10_148070, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.TIGER_USA_TREASURY_BOND_305080, 25),

//                    RebalanceBacktestCondition.TradeStock(StockCode.TIGER_SNP_360750, 18),
//                    RebalanceBacktestCondition.TradeStock(StockCode.ACE_GLD_411060, 15),
//                    RebalanceBacktestCondition.TradeStock(StockCode.KOSEF_TREASURY_BOND_10_148070, 25),
//                    RebalanceBacktestCondition.TradeStock(StockCode.KODEX_200_USD_BOND_284430, 42),

                RebalanceBacktestCondition.StockByWeight(StockCode.TIGER_NASDAQ_133690, 17),
                RebalanceBacktestCondition.StockByWeight(StockCode.KODEX_200_069500, 18),
                RebalanceBacktestCondition.StockByWeight(StockCode.KODEX_GLD_H_132030, 15),
                RebalanceBacktestCondition.StockByWeight(StockCode.KOSEF_TREASURY_BOND_10_148070, 25),
                RebalanceBacktestCondition.StockByWeight(StockCode.EXCHANGE_DOLLAR, 25),
            ),
            rebalanceFacter = RebalanceBacktestCondition.RebalanceFacter(PeriodType.PERIOD_HALF, 0.05),
        )

        val tradeNeoList = rebalanceBacktestService.runTest(rebalanceBacktestCondition)
        val accountCondition = AccountService.AccountCondition(rebalanceBacktestCondition.cash, 0.001, 0.001)

        val stockInfo = rebalanceBacktestCondition.stockByWeight.joinToString("\n") { stock ->
            "\t${stock.stockCode.code}[${stock.stockCode.desc}]\t비율: ${stock.weight}%"
        }
        val specialInfo = "대상종목${stockInfo}\n" +
                "리벨런싱 주기\t${rebalanceBacktestCondition.rebalanceFacter.periodType}\n" +
                "리벨런싱 입계치\t${rebalanceBacktestCondition.rebalanceFacter.threshold}"

        val backtestCondition = AccountService.BacktestCondition(rebalanceBacktestCondition.range, StockCode.KODEX_200_069500, specialInfo)
        val accountService = stockCommonFactory.createStockCommonFactory(accountCondition, backtestCondition)

        accountService.addTrade(tradeNeoList)
        accountService.calcTradeResult()
        accountService.calcEvaluationRate()
        val dir = File( "./backtest-result/rebalance-trade-report")
        val reportFile = File(dir, "rebalance_trade_${rebalanceBacktestCondition.range.fromDate}~${rebalanceBacktestCondition.range.toDate}.xlsx")
        accountService.makeReport(reportFile)
        log.info(reportFile.absolutePath)
        log.info("끝.")
    }
}