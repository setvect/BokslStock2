package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.common.model.BasicAnalysisCondition
import com.setvect.bokslstock2.analysis.vbs.entity.VbsConditionEntity
import com.setvect.bokslstock2.analysis.vbs.model.VbsAnalysisCondition
import com.setvect.bokslstock2.analysis.vbs.repository.VbsConditionRepository
import com.setvect.bokslstock2.analysis.vbs.repository.VbsTradeRepository
import com.setvect.bokslstock2.analysis.vbs.service.VbsAnalysisService
import com.setvect.bokslstock2.analysis.vbs.service.VbsBacktestService
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_DAY
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.ApplicationUtil
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
                        unitAskPrice = 5.0,
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
    fun 단건_리포트생성() {
        val range = DateRange(LocalDateTime.of(2000, 12, 1, 0, 0), LocalDateTime.now())
        val conditionList = vbsConditionRepository.listBySeq(listOf(3615774L))

        val stocks = conditionList.map { it.stock }.distinct().toList()
        val realRange = candleRepository.findByCandleDateTimeBetween(stocks, range.from, range.to)

        val vbsAnalysisCondition = VbsAnalysisCondition(
            tradeConditionList = conditionList,
            basic = BasicAnalysisCondition(
                range = realRange,
                investRatio = 0.99,
                cash = 10_000_000.0,
                feeBuy = 0.0002,
                feeSell = 0.0002,
                comment = ""
            )
        )

        analysisService.makeReport(vbsAnalysisCondition)
    }

    @Test
    @Transactional
    fun 멀티_리포트생성() {
        val elementList = listOf(2419416L, 2418908L)
        val conditionSetList = ApplicationUtil.getSubSet(elementList)

        val rangeList = listOf(
            DateRange(LocalDateTime.of(2016, 1, 1, 0, 0), LocalDateTime.of(2017, 1, 1, 0, 0)),
            DateRange(LocalDateTime.of(2017, 1, 1, 0, 0), LocalDateTime.of(2018, 1, 1, 0, 0)),
            DateRange(LocalDateTime.of(2018, 1, 1, 0, 0), LocalDateTime.of(2019, 1, 1, 0, 0)),
            DateRange(LocalDateTime.of(2019, 1, 1, 0, 0), LocalDateTime.of(2020, 1, 1, 0, 0)),
            DateRange(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2021, 1, 1, 0, 0)),
            DateRange(LocalDateTime.of(2021, 1, 1, 0, 0), LocalDateTime.now()),
            DateRange(LocalDateTime.of(2016, 1, 1, 0, 0), LocalDateTime.now()),
//            DateRange(LocalDateTime.of(2010, 1, 1, 0, 0), LocalDateTime.now()),
        )

        val mabsAnalysisConditionList = conditionSetList.flatMap { conditionSet ->
            val conditionList = vbsConditionRepository.listBySeq(conditionSet)
            rangeList.map { range ->
                // 시세가 포함된 날짜범위 지정
                val realRangeList =
                    conditionList.map {
                        candleRepository.findByCandleDateTimeBetween(
                            listOf(it.stock),
                            range.from,
                            range.to
                        )
                    }
                        .toList()
                val from = realRangeList.minOf { it.from }
                val to = realRangeList.maxOf { it.to }
                val realRange = DateRange(from, to)

                VbsAnalysisCondition(
                    tradeConditionList = conditionList,
                    basic = BasicAnalysisCondition(
                        range = realRange,
                        investRatio = 0.99,
                        cash = 10_000_000.0,
                        feeBuy = 0.0002,
                        feeSell = 0.0002,
                        comment = ""
                    )
                )
            }.toList()
        }.toList()

        analysisService.makeSummaryReport(mabsAnalysisConditionList)
    }

    private fun allConditionReportMake() {
        val conditionList = vbsConditionRepository.findAll().filter {
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

                val vbsAnalysisCondition = VbsAnalysisCondition(
                    tradeConditionList = listOf(it),
                    basic = BasicAnalysisCondition(
                        range = priceRange,
                        investRatio = 0.99,
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
        vbsTradeRepository.deleteAll()
        vbsConditionRepository.deleteAll()
    }


}