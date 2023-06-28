package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.analysis.common.service.AccountService
import com.setvect.bokslstock2.analysis.common.service.StockCommonFactory
import com.setvect.bokslstock2.analysis.mabs.model.MabsBacktestCondition
import com.setvect.bokslstock2.analysis.mabs.service.MabsBacktestService
import com.setvect.bokslstock2.index.model.PeriodType.*
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.DateUtil
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.io.File

@SpringBootTest
@ActiveProfiles("test")
class MabsBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var stockCommonFactory: StockCommonFactory

    @Autowired
    private lateinit var mabsBacktestService: MabsBacktestService

    /**
     * DB에 기록 남기지 않고 백테스팅하고 리포트 만듦
     */
    @Test
    @Transactional
    fun 일회성_백테스팅_리포트_만듦() {
        // 거래 조건
        val range = DateRange(DateUtil.getLocalDateTime("2001-01-01T00:00:00"), DateUtil.getLocalDateTime("2023-01-01T00:00:00"))
        val mabsBacktestCondition = MabsBacktestCondition(
            range = range,
            investRatio = 0.99,
            cash = 10_000_000.0,
            stockCode = StockCode.KODEX_2X_122630,
            periodType = PERIOD_DAY,
            upBuyRate = 0.00,
            downSellRate = 0.00,
            shortPeriod = 1,
            longPeriod = 200,
            comment = ""
        )
        val tradeNeoList = mabsBacktestService.runTest(mabsBacktestCondition)

        val specialInfo = "${String.format("투자 비율\t %,.2f%%", mabsBacktestCondition.investRatio * 100)}\n" +
                "${String.format("상승 매수률\t %,.2f%%", mabsBacktestCondition.upBuyRate * 100)}\n" +
                "${String.format("하락 매도률\t %,.2f%%", mabsBacktestCondition.downSellRate * 100)}\n" +
                "단기 이동평균 기간\t${mabsBacktestCondition.shortPeriod}\n" +
                "장기 이동평균 기간\t${mabsBacktestCondition.longPeriod}"

        val accountCondition = AccountService.AccountCondition(mabsBacktestCondition.cash, 0.00015, 0.00015)
        val backtestCondition = AccountService.BacktestCondition(range, StockCode.KODEX_200_069500, specialInfo)
        val accountService = stockCommonFactory.createStockCommonFactory(accountCondition, backtestCondition)

        // 리포트 만듦
        accountService.addTrade(tradeNeoList)
        accountService.calcTradeResult()
        accountService.calcEvaluationRate()
        val reportFile = File("./backtest-result/mabs-trade-report", "mbas_trade_${range.fromDate}~${range.toDate}.xlsx")
        accountService.makeReport(reportFile)
        log.info(reportFile.absolutePath)
        log.info("끝.")
    }
}