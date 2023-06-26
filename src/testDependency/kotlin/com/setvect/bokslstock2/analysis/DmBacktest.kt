package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.analysis.common.service.AccountService
import com.setvect.bokslstock2.analysis.common.service.StockCommonFactory
import com.setvect.bokslstock2.analysis.dm.model.DmBacktestCondition
import com.setvect.bokslstock2.analysis.dm.service.DmAnalysisService
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.DateUtil
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
class DmBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var dmAnalysisService: DmAnalysisService

    @Autowired
    private lateinit var stockCommonFactory: StockCommonFactory

    @Test
    fun 일회성_백테스팅_리포트_만듦() {
        val range = DateRange(DateUtil.getLocalDateTime("2018-01-01T00:00:00"), DateUtil.getLocalDateTime("2022-10-01T00:00:00"))

        val timeWeight = hashMapOf(
            1 to 0.33,
            3 to 0.33,
            6 to 0.34
        )

        val condition = DmBacktestCondition(
            range = range,
            investRatio = 0.999,
            cash = 10_000_000.0,
            stockCodes = listOf(StockCode.OS_CODE_SPY, StockCode.OS_CODE_SCZ),
            holdCode = StockCode.OS_CODE_TLT,
            periodType = PeriodType.PERIOD_MONTH,
            timeWeight = timeWeight,
            endSell = true
        )
        val tradeNeoList = dmAnalysisService.runTest(condition)
        val accountCondition = AccountService.AccountCondition(condition.cash, 0.0002, 0.0002)

        val specialInfo = "${String.format("매도 수수료\t %,.2f%%", condition.investRatio * 100)}\n" +
                "모멘텀 대상종목\t${condition.stockCodes.joinToString(", ") { it.name }}\n" +
                "홀드 종목\t${condition.holdCode?.name ?: "현금"}\n" +
                "거래 주기\t${condition.periodType.name}\n" +
                "기간 가중치\t${condition.timeWeight.map { "${it.key}개월: ${it.value}%" }.joinToString(", ")}\n" +
                "종료 시 매도\t${condition.endSell}\n"


        val backtestCondition = AccountService.BacktestCondition(range, StockCode.OS_CODE_SPY, specialInfo)
        val accountService = stockCommonFactory.createStockCommonFactory(accountCondition, backtestCondition)

        // 리포트 만듦
        accountService.addTrade(tradeNeoList)
        accountService.calcTradeResult()
        accountService.calcEvaluationRate()
        val reportFile = File("./backtest-result/dm-trade-report", "dm_trade_${range.fromDate}~${range.toDate}.xlsx")
        accountService.makeReport(reportFile)
        log.info(reportFile.absolutePath)
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