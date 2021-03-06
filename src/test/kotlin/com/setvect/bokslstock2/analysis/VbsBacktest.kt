package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.StockCode
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
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
    fun ?????????_??????_????????????() {
        // 0. ?????? ???????????? ?????? ?????? ??????
        deleteBacktestData()

        // 1. ?????? ?????????
        ????????????()

        // 2. ?????? ????????? ?????? ????????????
        vbsBacktestService.runTestBatch()

        // 3. ?????? ????????? ?????? ????????? ?????????
        allConditionReportMake()
    }


    /**
     * ???????????? ????????? ?????? Heap ????????? ??????
     */
    @Test
    @Transactional
    fun ??????????????????_????????????_???????????????() {
        allConditionReportMake()
    }

    @Test
    @Transactional
    @Rollback(false)
    fun ????????????() {
        vbsBacktestService.runTestBatch()
    }

    @Test
    fun ????????????() {
//        val stockList = stockRepository.findAll()
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
                        maPeriod = 0,
                        unitAskPrice = 0.01,
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
    fun ??????_???????????????() {
        val range = DateRange(LocalDateTime.of(2000, 12, 1, 0, 0), LocalDateTime.now())
        val conditionList = vbsConditionRepository.listBySeq(listOf(4986346L))

        val stocks = conditionList.map { it.stock }.distinct().toList()
        val realRange = candleRepository.findByCandleDateTimeBetween(stocks, range.from, range.to)

        val vbsAnalysisCondition = VbsAnalysisCondition(
            tradeConditionList = conditionList,
            basic = TradeCondition(
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
    fun ??????_???????????????() {
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
                // ????????? ????????? ???????????? ??????
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
                    basic = TradeCondition(
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

    /**
     * DB??? ?????? ????????? ?????? ?????????????????? ????????? ??????
     */
    @Test
    @Transactional
    fun ?????????_????????????_?????????_??????() {
        // ?????? ??????
        val range = DateRange(LocalDateTime.of(2016, 1, 1, 0, 0), LocalDateTime.now())
        val vbsAnalysisCondition = listOf(
            VbsAnalysisCondition(
                tradeConditionList = listOf(
                    makeCondition(StockCode.CODE_KODEX_KOSDAQ_2X_233740), // KODEX ?????????150 ????????????
                ),
                basic = TradeCondition(
                    range = range,
                    investRatio = 0.99,
                    cash = 10_000_000.0,
                    feeBuy = 0.0002,
                    feeSell = 0.0002,
                    comment = ""
                )
            ),
            VbsAnalysisCondition(
                tradeConditionList = listOf(
                    makeCondition(StockCode.CODE_KODEX_KOSDAQ_2X_233740), // KODEX ?????????150 ????????????
                ),
                basic = TradeCondition(
                    range = range,
                    investRatio = 0.7,
                    cash = 10_000_000.0,
                    feeBuy = 0.0002,
                    feeSell = 0.0002,
                    comment = ""
                )
            ),
            VbsAnalysisCondition(
                tradeConditionList = listOf(
                    makeCondition(StockCode.CODE_KODEX_KOSDAQ_2X_233740), // KODEX ?????????150 ????????????
                ),
                basic = TradeCondition(
                    range = range,
                    investRatio = 0.5,
                    cash = 10_000_000.0,
                    feeBuy = 0.0002,
                    feeSell = 0.0002,
                    comment = ""
                )
            )
        )

        // ????????? ??????
        analysisService.makeSummaryReport(vbsAnalysisCondition)

        log.info("???.")
    }

    private fun makeCondition(codeNam: String): VbsConditionEntity {
        val stock = stockRepository.findByCode(codeNam).get()
        val condition = VbsConditionEntity(
            stock = stock,
            periodType = PERIOD_DAY,
            kRate = 0.5,
            maPeriod = 0,
            unitAskPrice = 5.0,
            gapRisenSkip = false,
            onlyOneDayTrade = false,
            comment = null
        )
        vbsBacktestService.saveCondition(condition)
        vbsBacktestService.runTest(condition)

        val tradeList = vbsTradeRepository.findByCondition(condition)
        condition.tradeList = tradeList

        return condition
    }

    private fun allConditionReportMake() {
        val conditionList = vbsConditionRepository.findAll().filter {
            // tradeList ????????? ?????? ?????????. ?????? ????????? ????????? tradeList size??? 0??? ???????????? ????????? ???????????? ????????????
            entityManager.refresh(it)
            it.tradeList.size > 1
        }.toList()
        var i = 0
        val vbsConditionList = conditionList
            .map {
                val range = DateRange(LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now())
                val priceRange = candleRepository.findByCandleDateTimeBetween(listOf(it.stock), range.from, range.to)

                val vbsAnalysisCondition = VbsAnalysisCondition(
                    tradeConditionList = listOf(it),
                    basic = TradeCondition(
                        range = priceRange,
                        investRatio = 0.99,
                        cash = 10_000_000.0,
                        feeBuy = 0.0002,
                        feeSell = 0.0002,
                        comment = ""
                    )
                )
                log.info("?????? ?????? ?????? ?????? ${++i}/${conditionList.size}")
                vbsAnalysisCondition
            }.toList()
        analysisService.makeSummaryReport(vbsConditionList)
    }

    private fun deleteBacktestData() {
        vbsTradeRepository.deleteAll()
        vbsConditionRepository.deleteAll()
    }


}