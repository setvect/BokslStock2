package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.vbs.model.VbsAnalysisCondition
import com.setvect.bokslstock2.analysis.vbs.model.VbsCondition
import com.setvect.bokslstock2.analysis.vbs.service.VbsAnalysisService
import com.setvect.bokslstock2.analysis.vbs.service.VbsBacktestService
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_DAY
import com.setvect.bokslstock2.index.repository.StockRepository
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

@SpringBootTest
@ActiveProfiles("test")
// 실제 매매 동작를 하지 않도록 MockBean으로 대체
@MockBeans(value = [MockBean(VbsEventHandler::class, VbsStockSchedule::class)])
class VbsBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var vbsBacktestService: VbsBacktestService

    @Autowired
    private lateinit var analysisService: VbsAnalysisService

    /**
     * DB에 기록 남기지 않고 백테스팅하고 리포트 만듦
     */
    @Test
    @Transactional
    fun 일회성_백테스팅_리포트_만듦() {
        // 거래 조건
//        val range = DateRange(LocalDateTime.of(2021, 8, 31, 0, 0), LocalDateTime.now())
//        val range = DateRange(LocalDateTime.of(2021, 9, 20, 0, 0), LocalDateTime.of(2021, 10, 1, 0, 0))
//        val range = DateRange(LocalDateTime.of(2018, 1, 1, 0, 0), LocalDateTime.of(2023, 1, 6, 0, 0))
//        val range = DateRange(LocalDateTime.of(2022, 8, 24, 0, 0), LocalDateTime.of(2022, 8, 31, 0, 0))

//        val range = DateRange(DateUtil.getLocalDateTime("2018-01-01T00:00:00"), DateUtil.getLocalDateTime("2023-03-18T00:00:00"))
        val range = DateRange(DateUtil.getLocalDateTime("2018-01-01T00:00:00"), DateUtil.getLocalDateTime("2023-05-14T00:00:00"))
//        val range = DateRange(DateUtil.getLocalDateTime("2018-01-01T00:00:00"), DateUtil.getLocalDateTime("2018-01-14T00:00:00"))

        val condition1 = makeCondition(StockCode.KODEX_KOSDAQ_2X_233740, range, 0.5, true, 0.5)
        condition1.tradeList = vbsBacktestService.runTest(condition1)

        val condition2 = makeCondition(StockCode.KODEX_BANK_091170, range, 0.5, false, 0.25)
        condition2.tradeList = vbsBacktestService.runTest(condition2)

        val tradeConditionList = listOf(condition1, condition2)

        val vbsAnalysisCondition = listOf(
            VbsAnalysisCondition(
                tradeConditionList = tradeConditionList,
                basic = TradeCondition(
                    range = range,
                    investRatio = 0.99,
                    cash = 20_000_000.0,
                    feeBuy = 0.0002,
                    feeSell = 0.0002,
                    comment = "",
                    benchmark = listOf(StockCode.KODEX_200_069500)
                )
            ),
        )

        // 리포트 만듦
        val vbsAnalysisConditionAndResults = analysisService.runAnalysis(vbsAnalysisCondition)
        analysisService.makeTradeReport(vbsAnalysisConditionAndResults)
        analysisService.makeSummaryReport(vbsAnalysisConditionAndResults)

        log.info("끝.")
    }

    private fun makeCondition(
        stockCode: StockCode,
        range: DateRange,
        kRate: Double,
        stayGapRise: Boolean,
        investmentRatio: Double
    ): VbsCondition {
        val stock = stockRepository.findByCode(stockCode.code).get()
        return VbsCondition(
            name = stock.name + " " + kRate,
            stock = stock,
            range = range,
            periodType = PERIOD_DAY,
            kRate = kRate,
            maPeriod = 0,
            unitAskPrice = 5.0,
            gapRisenSkip = false,
            onlyOneDayTrade = false,
            comment = null,
            stayGapRise = stayGapRise,
            investmentRatio = investmentRatio
        )
    }
}