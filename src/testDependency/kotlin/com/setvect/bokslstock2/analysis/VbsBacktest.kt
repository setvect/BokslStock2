package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.analysis.common.service.AccountService
import com.setvect.bokslstock2.analysis.common.service.StockCommonFactory
import com.setvect.bokslstock2.analysis.vbs.model.VbsBacktestCondition
import com.setvect.bokslstock2.analysis.vbs.model.VbsConditionItem
import com.setvect.bokslstock2.analysis.vbs.service.VbsBacktestService
import com.setvect.bokslstock2.koreainvestment.vbs.service.VbsEventHandler
import com.setvect.bokslstock2.koreainvestment.vbs.service.VbsStockSchedule
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.DateUtil
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockBeans
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@ActiveProfiles("test")
// 실제 매매 동작를 하지 않도록 MockBean으로 대체
@MockBeans(value = [MockBean(VbsEventHandler::class, VbsStockSchedule::class)])
class VbsBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var vbsBacktestService: VbsBacktestService

    @Autowired
    private lateinit var stockCommonFactory: StockCommonFactory

    /**
     * DB에 기록 남기지 않고 백테스팅하고 리포트 만듦
     */
    @Test
    @Transactional
    fun 일회성_백테스팅_리포트_만듦() {
        // 거래 조건
//        val range = DateRange(DateUtil.getLocalDateTime("2023-01-01T00:00:00"), DateUtil.getLocalDateTime("2023-05-14T00:00:00"))
//        val range = DateRange(DateUtil.getLocalDateTime("2023-01-01T00:00:00"), DateUtil.getLocalDateTime("2023-02-14T00:00:00"))
        val range = DateRange(DateUtil.getLocalDateTime("2018-01-01T00:00:00"), DateUtil.getLocalDateTime("2023-05-12T00:00:00"))

        val condition = VbsBacktestCondition(
            range = range,
            investRatio = 0.99,
            cash = 20_000_000.0,
            conditionList = arrayListOf(
                VbsConditionItem(
                    stockCode = StockCode.KODEX_KOSDAQ_2X_233740,
                    kRate = 0.5,
                    stayGapRise = true,
                    maPeriod = 0,
                    unitAskPrice = 5.0,
                    comment = null,
                    investmentRatio = 0.5
                ),
                VbsConditionItem(
                    stockCode = StockCode.KODEX_BANK_091170,
                    kRate = 0.5,
                    stayGapRise = false,
                    maPeriod = 0,
                    unitAskPrice = 5.0,
                    comment = null,
                    investmentRatio = 0.25
                )
            )
        )
        val tradeNeo = vbsBacktestService.runTest(condition)
        val accountCondition = AccountService.AccountCondition(condition.cash, 0.0002, 0.0002)
        val count = AtomicInteger(0)
        val specialInfo = condition.conditionList.joinToString("\n") {
            val idx = count.getAndIncrement()
            """
                $count. 대상 종목\t${it.stockCode.name}
                $count. 변동성 비율\t${it.kRate}
                $count. 투자 비율\t${it.investmentRatio}
                $count. 이동평균 단위\t${it.maPeriod}
                $count. 5분 마다 시세 체크\t${it.stayGapRise}
                $count. 호가단위\t${it.unitAskPrice}
                $count. 조건 설명\t${it.comment}
            """.trimIndent().replace("\\t", "\t")
        }

        val backtestCondition = AccountService.BacktestCondition(range, StockCode.KODEX_200_069500, specialInfo)
        val accountService = stockCommonFactory.createStockCommonFactory(accountCondition, backtestCondition)

        // 리포트 만듦
        accountService.addTrade(tradeNeo)
        accountService.calcTradeResult()
        accountService.calcEvaluationRate()
        val reportFile = File("./backtest-result/vbs-trade-report", "vbs_trade_${range.fromDate}~${range.toDate}.xlsx")
        accountService.makeReport(reportFile)
        log.info(reportFile.absolutePath)
        log.info("끝.")
    }
}