package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.StockCode
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.rb.entity.RbConditionEntity
import com.setvect.bokslstock2.analysis.rb.model.RbAnalysisCondition
import com.setvect.bokslstock2.analysis.rb.repository.RbConditionRepository
import com.setvect.bokslstock2.analysis.rb.repository.RbTradeRepository
import com.setvect.bokslstock2.analysis.rb.setvice.RbAnalysisService
import com.setvect.bokslstock2.analysis.rb.setvice.RbBacktestService
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_MONTH
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateRange
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import javax.persistence.EntityManager

@SpringBootTest
@ActiveProfiles("local")
@Deprecated("삭제할 백테스트")
class RbBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var rbTradeRepository: RbTradeRepository

    @Autowired
    private lateinit var rbConditionRepository: RbConditionRepository

    @Autowired
    private lateinit var rbBacktestService: RbBacktestService

    @Autowired
    private lateinit var analysisService: RbAnalysisService

    @Autowired
    private lateinit var candleRepository: CandleRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    /**
     * 백테스트 건수에 따라 Heap 사이즈 조절
     */
    @Test
    @Transactional
    @Rollback(false)
    fun 한번에_모두_백테스트() {
        // 0. 기존 백테스트 기록 모두 삭제
        deleteBacktestData()

        // 1. 조건 만들기
        조건생성()

        // 2. 모든 조건에 대해 백테스트
        rbBacktestService.runTestBatch()

        // 3. 모든 조건에 대한 리포트 만들기
        allConditionReportMake()
    }

    /**
     * 백테스트 건수에 따라 Heap 사이즈 조절
     */
    @Test
    @Transactional
    fun 모든매매조건_단건기준_리포트생성() {
        allConditionReportMake()
    }

    @Test
    fun 조건생성() {
        val stockList = stockRepository.findAll()

        stockList.forEach { stock ->
            val condition = RbConditionEntity(
                stock = stock,
                periodType = PERIOD_MONTH,
                comment = null
            )
            rbBacktestService.saveCondition(condition)
        }
    }

    /**
     * DB에 기록 남기지 않고 백테스팅하고 리포트 만듦
     */
    @Test
    @Transactional
    fun 일회성_백테스팅_리포트_만듦() {
        // 거래 조건
        val realRange = DateRange(LocalDateTime.of(2016, 1, 1, 0, 0), LocalDateTime.now())
        val rbAnalysisCondition = RbAnalysisCondition(
            tradeConditionList = listOf(
                makeCondition(StockCode.OS_CODE_SPY),
            ),
            basic = TradeCondition(
                range = realRange,
                investRatio = 1.0,
                cash = 10_000_000.0,
                feeBuy = 0.0002,
                feeSell = 0.0002,
                comment = "",
                benchmark = listOf(StockCode.OS_CODE_SPY),
            )
        )
        val rbAnalysisConditionList = listOf(rbAnalysisCondition)

        // 리포트 만듦
        analysisService.makeSummaryReport(rbAnalysisConditionList)

        log.info("끝.")
    }

    private fun makeCondition(codeNam: String): RbConditionEntity {
        val stock = stockRepository.findByCode(codeNam).get()
        val condition = RbConditionEntity(
            stock = stock,
            periodType = PERIOD_MONTH,
            comment = null
        )
        rbBacktestService.saveCondition(condition)
        rbBacktestService.runTest(condition)

        val tradeList = rbTradeRepository.findByCondition(condition)
        condition.tradeList = tradeList

        return condition
    }

    private fun allConditionReportMake() {
        val conditionList = rbConditionRepository.findAll().filter {
            // tradeList 정보를 다시 읽어옴. 해당 구분이 없으면 tradeList size가 0인 상태에서 캐싱된 데이터가 불러와짐
            entityManager.refresh(it)
            it.tradeList.size > 1
        }.toList()

        var i = 0
        val vbsConditionList = conditionList
//            .filter { it.stock.code == "091170" }
            .map {
                val range = DateRange(LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now())
                val priceRange = candleRepository.findByCandleDateTimeBetween(listOf(it.stock), range.from, range.to)

                val vbsAnalysisCondition = RbAnalysisCondition(
                    tradeConditionList = listOf(it),
                    basic = TradeCondition(
                        range = priceRange,
                        investRatio = 0.5,
                        cash = 10_000_000.0,
                        feeBuy = 0.0002,
                        feeSell = 0.0002,
                        comment = ""
                    )
                )
                log.info("거래 내역 조회 진행 ${++i}/${conditionList.size}")
                vbsAnalysisCondition
            }.toList()
        analysisService.makeSummaryReport(vbsConditionList)
    }

    private fun deleteBacktestData() {
        rbTradeRepository.deleteAll()
        rbConditionRepository.deleteAll()
    }


}