package com.setvect.bokslstock2.backtest

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.backtest.common.service.AccountService
import com.setvect.bokslstock2.backtest.common.service.StockCommonFactory
import com.setvect.bokslstock2.backtest.stoploss.model.StopLoosBacktestCondition
import com.setvect.bokslstock2.backtest.stoploss.service.StopLossBacktestService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.time.YearMonth

@SpringBootTest
@ActiveProfiles("test")
class StopLossBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var stockCommonFactory: StockCommonFactory

    @Autowired
    private lateinit var stopLossBacktestService: StopLossBacktestService

    @Test
    fun 일회성_백테스팅_리포트_만듦() {
        val condition = StopLoosBacktestCondition(
            from = YearMonth.of(2005, 1),
            to = YearMonth.of(2023, 6),
            investRatio = 0.95,
            cash = 10_000_000.0,
            stockCode = StockCode.KODEX_2X_122630,
            stopLossRate = 0.20,
            averageMonthCount = 6
        )

        val tradeNeoList = stopLossBacktestService.runTest(condition)
        val accountCondition = AccountService.AccountCondition(condition.cash, 0.0005, 0.0005)
        val specialInfo = "대상종목\t${condition.stockCode.name}\n" +
                "투자비율\t${condition.investRatio}\n" +
                "투자금\t${condition.cash}\n" +
                "손절률\t${condition.stopLossRate}\n" +
                "평균월 수익\t${condition.averageMonthCount}개월\n"

        val backtestCondition = AccountService.BacktestCondition(condition.getRange(), StockCode.KODEX_200_069500, specialInfo)
        val accountService = stockCommonFactory.createStockCommonFactory(accountCondition, backtestCondition)
        accountService.addTrade(tradeNeoList)
        accountService.calcTradeResult()
        accountService.calcEvaluationRate()
        val dir = File("./backtest-result/stop_loss")
        dir.mkdirs()
        val reportFile = File(dir, "stop_loss_${condition.getRange().fromDate}~${condition.getRange().toDate}.xlsx")
        accountService.makeReport(reportFile)
        log.info(reportFile.absolutePath)
        log.info("끝.")
    }
}