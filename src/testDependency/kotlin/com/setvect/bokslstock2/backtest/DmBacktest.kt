package com.setvect.bokslstock2.backtest

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.backtest.common.service.AccountService
import com.setvect.bokslstock2.backtest.common.service.StockCommonFactory
import com.setvect.bokslstock2.backtest.dm.model.DmBacktestCondition
import com.setvect.bokslstock2.backtest.dm.service.DmBacktestService
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
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
class DmBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var dmBacktestService: DmBacktestService

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

        val dmBacktestCondition = DmBacktestCondition(
            range = range,
            investRatio = 0.999,
            cash = 10_000_000.0,
            stockCodes = listOf(StockCode.OS_CODE_SPY, StockCode.OS_CODE_SCZ),
            holdCode = StockCode.OS_CODE_TLT,
            periodType = PeriodType.PERIOD_MONTH,
            timeWeight = timeWeight,
            endSell = true
        )
        val dualMomentumResult = dmBacktestService.runTest(dmBacktestCondition)
        val tradeNeoList = dualMomentumResult.tradeList
        val accountCondition = AccountService.AccountCondition(dmBacktestCondition.cash, 0.0002, 0.0002)

        val specialInfo = "${String.format("매도 수수료\t %,.2f%%", dmBacktestCondition.investRatio * 100)}\n" +
                "모멘텀 대상종목\t${dmBacktestCondition.stockCodes.joinToString(", ") { it.name }}\n" +
                "홀드 종목\t${dmBacktestCondition.holdCode?.name ?: "현금"}\n" +
                "거래 주기\t${dmBacktestCondition.periodType.name}\n" +
                "기간 가중치\t${dmBacktestCondition.timeWeight.map { "${it.key}개월: ${it.value}%" }.joinToString(", ")}\n" +
                "종료 시 매도\t${dmBacktestCondition.endSell}\n"


        val backtestCondition = AccountService.BacktestCondition(range, StockCode.OS_CODE_SPY, specialInfo)
        val accountService = stockCommonFactory.createStockCommonFactory(accountCondition, backtestCondition)

        // 리포트 만듦
        accountService.addTrade(tradeNeoList)
        accountService.calcTradeResult()
        accountService.calcEvaluationRate()
        val reportFile = File("./backtest-result/dm-trade-report", "dm_trade_${range.fromDate}~${range.toDate}.xlsx")

        accountService.makeReport(reportFile, DmBacktestService.MomentumScoreSheetMaker(dualMomentumResult.momentumScoreList, dmBacktestCondition))
        log.info(reportFile.absolutePath)
        log.info("끝.")
    }

    @Test
    fun momentumScore() {
        val date = LocalDate.of(2022, 5, 1)
        val momentumScore = dmBacktestService.getMomentumScore(
            date, listOf(StockCode.OS_CODE_SPY, StockCode.OS_CODE_SCZ), StockCode.OS_CODE_TLT, hashMapOf(
                1 to 0.33,
                3 to 0.33,
                6 to 0.34
            )
        )
        println(momentumScore)
    }
}