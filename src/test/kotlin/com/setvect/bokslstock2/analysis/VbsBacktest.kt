package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.entity.vbs.VbsConditionEntity
import com.setvect.bokslstock2.analysis.model.vbs.VbsAnalysisCondition
import com.setvect.bokslstock2.analysis.repository.vbs.VbsConditionRepository
import com.setvect.bokslstock2.analysis.repository.vbs.VbsTradeRepository
import com.setvect.bokslstock2.analysis.service.MovingAverageService
import com.setvect.bokslstock2.analysis.service.vbs.VbsAnalysisService
import com.setvect.bokslstock2.analysis.service.vbs.VbsBacktestService
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_DAY
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateRange
import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@SpringBootTest
@ActiveProfiles("local")
class VbsBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var vbsTradeRepository: VbsTradeRepository

    @Autowired
    private lateinit var vbsAverageService: MovingAverageService

    @Autowired
    private lateinit var vbsConditionRepository: VbsConditionRepository

    @Autowired
    private lateinit var vbsBacktestService: VbsBacktestService

    @Autowired
    private lateinit var analysisService: VbsAnalysisService

    @Autowired
    private lateinit var candleRepository: CandleRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    @Transactional
    @Rollback(false)
    fun 한번에_모두_백테스트() {
        // 0. 기존 백테스트 기록 모두 삭제
        deleteBacktestData()

        // 1. 조건 만들기
        조건생성()

        // 2. 모든 조건에 대해 백테스트
        vbsBacktestService.runTestBatch()

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

        val optionList = listOf(
            Pair(false, false),
            Pair(false, true),
            Pair(true, false),
            Pair(true, true),
        )

        val kRateList = listOf(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0)

        stockList.forEach { stock ->
            kRateList.forEach { kRate ->
                optionList.forEach {
                    val condition = VbsConditionEntity(
                        stock = stock,
                        periodType = PERIOD_DAY,
                        kRate = kRate,
                        maPeriod = 1,
                        unitAskPrice = 5,
                        gapRisenSkip = it.first,
                        onlyOneDayTrade = it.second,
                        comment = null
                    )
                    vbsBacktestService.saveCondition(condition)
                }
            }
        }
    }

    @Test
    @Transactional
    // TODO
    fun 단건_리포트생성() {
//        VBS_CONDITION_SEQ	GAP_RISEN_SKIP	ONLY_ONE_DAY_TRADE	K_RATE
//        1313246	N	N	0.5
//        1313247	N	Y	0.5
//        1313248	Y	N	0.5
//        1313249	Y	Y	0.5

        val range = DateRange(LocalDateTime.of(2015, 12, 1, 0, 0), LocalDateTime.now())
        val conditionList = vbsConditionRepository.listBySeq(listOf(1313246))
        val vbsAnalysisCondition = VbsAnalysisCondition(
            tradeConditionList = conditionList,
            range = range,
            investRatio = 0.99,
            cash = 10_000_000,
            feeBuy = 0.0002,
            feeSell = 0.0002,
            comment = ""
        )

        analysisService.makeReport(vbsAnalysisCondition)
    }

    private fun allConditionReportMake() {
        val conditionList = vbsConditionRepository.findAll().filter {
            // tradeList 정보를 다시 읽어옴. 해당 구분이 없으면 tradeList size가 0인 상태에서 캐싱된 데이터가 불러와짐
            entityManager.refresh(it)
            it.tradeList.size > 1
        }.toList()

        var i = 0
        val vbsConditionList = conditionList.map {
            val range = DateRange(LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now())
            val priceRange = candleRepository.findByCandleDateTimeBetween(it.stock, range.from, range.to)

            val vbsAnalysisCondition = VbsAnalysisCondition(
                tradeConditionList = listOf(it),
                range = priceRange,
                investRatio = 0.99,
                cash = 10_000_000,
                feeBuy = 0.0002,
                feeSell = 0.0002,
                comment = ""
            )
            log.info("거래 내역 조회 진행 ${++i}/${conditionList.size}")
            vbsAnalysisCondition
        }.toList()
        analysisService.makeSummaryReport(vbsConditionList)
    }

    private fun deleteBacktestData() {
        vbsTradeRepository.deleteAll()
        vbsConditionRepository.deleteAll()
    }


}