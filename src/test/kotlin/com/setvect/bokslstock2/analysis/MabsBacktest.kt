package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.mabs.model.MabsAnalysisCondition
import com.setvect.bokslstock2.analysis.mabs.model.MabsCondition
import com.setvect.bokslstock2.analysis.mabs.service.MabsAnalysisService
import com.setvect.bokslstock2.analysis.mabs.service.MabsBacktestService
import com.setvect.bokslstock2.index.model.PeriodType.*
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateRange
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
class MabsBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var mabsBacktestService: MabsBacktestService

    @Autowired
    private lateinit var analysisService: MabsAnalysisService

    /**
     * DB에 기록 남기지 않고 백테스팅하고 리포트 만듦
     */
    @Test
    @Transactional
    fun 일회성_백테스팅_리포트_만듦() {
        // 거래 조건
        val realRange = DateRange(LocalDateTime.of(2001, 1, 1, 0, 0), LocalDateTime.now())
        val mabsCondition = makeCondition(StockCode.KODEX_2X_122630.code)
        mabsCondition.tradeList = mabsBacktestService.runTest(mabsCondition)
        val mabsAnalysisCondition = MabsAnalysisCondition(
            tradeConditionList = listOf(mabsCondition),
            basic = TradeCondition(
                range = realRange,
                investRatio = 0.99,
                cash = 10_000_000.0,
                feeBuy = 0.00015,
                feeSell = 0.00015,
                comment = ""
            )
        )
        val mabsAnalysisConditionList = listOf(mabsAnalysisCondition)

        // 리포트 만듦
        analysisService.makeSummaryReport(mabsAnalysisConditionList)

        log.info("끝.")
    }

    private fun makeCondition(codeNam: String): MabsCondition {
        val stock = stockRepository.findByCode(codeNam).get()
        return MabsCondition(
            name = stock.name,
            stock = stock,
            periodType = PERIOD_DAY,
            upBuyRate = 0.00,
            downSellRate = 0.00,
            shortPeriod = 1,
            longPeriod = 200,
            comment = ""
        )
    }
}