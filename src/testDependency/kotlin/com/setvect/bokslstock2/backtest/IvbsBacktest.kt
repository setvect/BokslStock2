package com.setvect.bokslstock2.backtest

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.backtest.common.service.AccountService
import com.setvect.bokslstock2.backtest.common.service.StockCommonFactory
import com.setvect.bokslstock2.backtest.ivbs.model.IvbsBacktestCondition
import com.setvect.bokslstock2.backtest.ivbs.model.IvbsConditionItem
import com.setvect.bokslstock2.backtest.ivbs.service.IvbsBacktestService
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.DateUtil
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * 결론: 의미 없음
 */
@SpringBootTest
@ActiveProfiles("test")
class IvbsBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var ivbsBacktestService: IvbsBacktestService

    @Autowired
    private lateinit var stockCommonFactory: StockCommonFactory


    @Test
    fun runTest() {
        // 거래 조건
//        val range = DateRange(DateUtil.getLocalDateTime("2023-01-01T00:00:00"), DateUtil.getLocalDateTime("2023-05-14T00:00:00"))
//        val range = DateRange(DateUtil.getLocalDateTime("2023-01-01T00:00:00"), DateUtil.getLocalDateTime("2023-02-14T00:00:00"))
        val range = DateRange(DateUtil.getLocalDateTime("2018-01-01T00:00:00"), DateUtil.getLocalDateTime("2023-05-12T00:00:00"))

        val condition = IvbsBacktestCondition(
            range = range,
            investRatio = 0.99,
            cash = 20_000_000.0,
            conditionList = arrayListOf(
                IvbsConditionItem(
                    stockCode = StockCode.KODEX_BANK_091170,
                    kRate = 0.5,
                    stayGapRise = false, // KODEX 은행과 같은 배당을 주는 종목은 수정 주가 계산으로 5분봉 시세 체크를 하면 안됨.
                    unitAskPrice = 5.0,
                    comment = null,
                    investmentRatio = 0.5
                )
            )
        )
        val tradeNeoList = ivbsBacktestService.runTest(condition)
        val accountCondition = AccountService.AccountCondition(condition.cash, 0.0002, 0.0002)
        val count = AtomicInteger(0)
        var specialInfo = String.format("투자 비율\t %,.2f%%", condition.investRatio * 100) + "\n"
        specialInfo += condition.conditionList.joinToString("\n") {
            """
                ${count.incrementAndGet()}. 대상 종목\t${it.stockCode.name}
                $count. 변동성 비율\t${it.kRate}
                $count. 투자 비율\t${it.investmentRatio}
                $count. 5분 마다 시세 체크\t${it.stayGapRise}
                $count. 호가단위\t${it.unitAskPrice}
                $count. 조건 설명\t${it.comment}
            """.trimIndent().replace("\\t", "\t")
        }

        val backtestCondition = AccountService.BacktestCondition(range, StockCode.KODEX_200_069500, specialInfo)
        val accountService = stockCommonFactory.createStockCommonFactory(accountCondition, backtestCondition)

        // 리포트 만듦
        accountService.addTrade(tradeNeoList)
        accountService.calcTradeResult()
        accountService.calcEvaluationRate()
        val dir = File( "./backtest-result/ivbs-trade-report")
        dir.mkdirs()
        val reportFile = File(dir, "ivbs_trade_${range.fromDate}~${range.toDate}.xlsx")
        accountService.makeReport(reportFile)
        log.info(reportFile.absolutePath)
        log.info("끝.")
    }
}