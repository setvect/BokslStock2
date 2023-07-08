package com.setvect.bokslstock2.backtest

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.backtest.common.service.AccountService
import com.setvect.bokslstock2.backtest.common.service.StockCommonFactory
import com.setvect.bokslstock2.backtest.laa.model.LaaBacktestCondition
import com.setvect.bokslstock2.backtest.laa.service.LaaBacktestService
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
class LaaBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var stockCommonFactory: StockCommonFactory

    @Autowired
    private lateinit var laaBacktestService: LaaBacktestService

    @Test
    fun 일회성_백테스팅_리포트_만듦() {
//        val range = DateRange("2005-01-01T00:00:00", "2023-07-01T00:00:00")
        val range = DateRange("2007-01-01T00:00:00", "2023-06-01T00:00:00")

        val laaBacktestCondition = LaaBacktestCondition(
            range = range,
            cash = 20_000_000.0,
            stockCodes = listOf(
                LaaBacktestCondition.TradeStock(StockCode.OS_CODE_IEF, 25),
                LaaBacktestCondition.TradeStock(StockCode.OS_CODE_IWD, 25),
                LaaBacktestCondition.TradeStock(StockCode.OS_CODE_GLD, 25),
            ),
            rebalanceFacter = LaaBacktestCondition.RebalanceFacter(PeriodType.PERIOD_QUARTER, 0.05),
            laaWeight = 25,
            testStockCode = StockCode.OS_CODE_SPY,
            testMa = 200,
            offenseCode = StockCode.OS_CODE_QQQ,
            defenseCode = StockCode.OS_CODE_SHY,
        )

        val tradeNeoList = laaBacktestService.runTest(laaBacktestCondition)
        val accountCondition = AccountService.AccountCondition(laaBacktestCondition.cash, 0.001, 0.001)

        val stockInfo = laaBacktestCondition.stockCodes.joinToString("\n") { stock ->
            "\t${stock.stockCode.code}[${stock.stockCode.desc}]\t비율: ${stock.weight}%"
        }
        val specialInfo = "대상종목${stockInfo}\n" +
                "리벨런싱 주기\t${laaBacktestCondition.rebalanceFacter.periodType}\n" +
                "리벨런싱 입계치\t${laaBacktestCondition.rebalanceFacter.threshold}"

        val backtestCondition = AccountService.BacktestCondition(laaBacktestCondition.range, StockCode.OS_CODE_SPY, specialInfo)
        val accountService = stockCommonFactory.createStockCommonFactory(accountCondition, backtestCondition)

        accountService.addTrade(tradeNeoList)
        accountService.calcTradeResult()
        accountService.calcEvaluationRate()
        val dir = File("./backtest-result/laa-trade-report")
        dir.mkdirs()
        val reportFile = File(dir, "laa_trade_${laaBacktestCondition.range.fromDate}~${laaBacktestCondition.range.toDate}.xlsx")
        accountService.makeReport(reportFile)
        log.info(reportFile.absolutePath)
        log.info("끝.")
    }
}