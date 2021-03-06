package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.StockCode
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.mabs.entity.MabsConditionEntity
import com.setvect.bokslstock2.analysis.mabs.model.MabsAnalysisCondition
import com.setvect.bokslstock2.analysis.mabs.repository.MabsConditionRepository
import com.setvect.bokslstock2.analysis.mabs.repository.MabsTradeRepository
import com.setvect.bokslstock2.analysis.mabs.service.MabsAnalysisService
import com.setvect.bokslstock2.analysis.mabs.service.MabsBacktestService
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_DAY
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_MONTH
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_WEEK
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
class MabsBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var mabsBacktestService: MabsBacktestService

    @Autowired
    private lateinit var analysisService: MabsAnalysisService

    @Autowired
    private lateinit var mabsConditionRepository: MabsConditionRepository

    @Autowired
    private lateinit var candleRepository: CandleRepository

    @Autowired
    private lateinit var mabsTradeRepository: MabsTradeRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    @Transactional
    @Rollback(false)
    fun ?????????_??????_????????????() {
        // 0. ?????? ???????????? ?????? ?????? ??????
        deleteBacktestData()

        // 1. ?????? ?????????
        ????????????_??????()
        ????????????_??????()
        ????????????_??????()

        // 2. ?????? ????????? ?????? ????????????
        mabsBacktestService.runTestBatch()

        // 3. ?????? ????????? ?????? ????????? ?????????
        allConditionReportMake()
    }

    @Test
    @Transactional
    fun ?????????_????????????() {
        val stockOp = stockRepository.findByCode(StockCode.CODE_KODEX_200_069500)
        val stock = stockOp.get()

        log.info("${stock.name}(${stock.code}) ${stock.candleList.size}")
        stock.candleList.forEach {
            log.info("${it.candleDateTime}: close:${String.format("%,d", it.closePrice)}")
        }
        println("???.")
    }

    @Test
    fun ????????????() {
        ????????????_??????()
    }


    @Test
    @Transactional
    @Rollback(false)
    fun ????????????() {
        mabsBacktestService.runTestBatch()
    }

    @Test
    @Transactional
    fun ??????_???????????????() {
//        val elementList = listOf(951551, 951255) // ?????? ????????? - TIGER ?????????CSI300, TIGER ???????????????100
        val elementList = listOf(950589L, 950064L) // ?????? ????????? - TIGER ?????????CSI300, TIGER ???????????????100
//        val elementList = listOf(952722, 950164) // ?????? ????????? - TIGER ?????????CSI300, TIGER ???????????????100
//        val elementList = listOf(949078, 951062) // ?????????150 ????????????, KODEX ?????????150???????????????
//        val elementList = listOf(949078, 949331) // ?????????150 ????????????, KODEX ????????????
//        val elementList = listOf(949079) // ?????????150 ????????????
        val conditionSetList = ApplicationUtil.getSubSet(elementList)

        val rangeList = listOf(
//            DateRange(LocalDateTime.of(2016, 1, 1, 0, 0), LocalDateTime.of(2017, 1, 1, 0, 0)),
//            DateRange(LocalDateTime.of(2017, 1, 1, 0, 0), LocalDateTime.of(2018, 1, 1, 0, 0)),
//            DateRange(LocalDateTime.of(2018, 1, 1, 0, 0), LocalDateTime.of(2019, 1, 1, 0, 0)),
//            DateRange(LocalDateTime.of(2019, 1, 1, 0, 0), LocalDateTime.of(2020, 1, 1, 0, 0)),
//            DateRange(LocalDateTime.of(2020, 1, 1, 0, 0), LocalDateTime.of(2021, 1, 1, 0, 0)),
//            DateRange(LocalDateTime.of(2021, 1, 1, 0, 0), LocalDateTime.now()),
            DateRange(LocalDateTime.of(2010, 1, 1, 0, 0), LocalDateTime.now()),
        )

        val mabsAnalysisConditionList = conditionSetList.flatMap { conditionSet ->
            val conditionList = mabsConditionRepository.listBySeq(conditionSet)
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

                MabsAnalysisCondition(
                    tradeConditionList = conditionList,
                    basic = TradeCondition(
                        range = realRange,
                        investRatio = 0.99,
                        cash = 10_000_000.0,
                        feeBuy = 0.001,
                        feeSell = 0.001,
                        comment = ""
                    )
                )
            }.toList()
        }.toList()

        analysisService.makeSummaryReport(mabsAnalysisConditionList)
    }

    @Test
    @Transactional
    fun ??????_???????????????() {
        val range = DateRange(LocalDateTime.of(2016, 1, 1, 0, 0), LocalDateTime.now())
        val conditionList = mabsConditionRepository.listBySeq(listOf(949092))
        val mabsAnalysisCondition = MabsAnalysisCondition(
            tradeConditionList = conditionList,
            basic = TradeCondition(
                range = range,
                investRatio = 0.99,
                cash = 10_000_000.0,
                feeBuy = 0.001,
                feeSell = 0.001,
                comment = ""
            )
        )

        analysisService.makeReport(mabsAnalysisCondition)
    }

    /**
     * ???????????? ????????? ?????? Heap ????????? ??????
     * -Xms2G -Xmx2G
     */
    @Test
    @Transactional
    fun ??????????????????_????????????_???????????????() {
        allConditionReportMake()
    }

    /**
     * DB??? ?????? ????????? ?????? ?????????????????? ????????? ??????
     */
    @Test
    @Transactional
    fun ?????????_????????????_?????????_??????() {
        // ?????? ??????
        val realRange = DateRange(LocalDateTime.of(2015, 1, 1, 0, 0), LocalDateTime.now())
        val mabsAnalysisCondition = MabsAnalysisCondition(
            tradeConditionList = listOf(
                makeCondition(StockCode.CODE_KODEX_2X_122630),
            ),
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

        // ????????? ??????
        analysisService.makeSummaryReport(mabsAnalysisConditionList)

        log.info("???.")
    }


    private fun makeCondition(codeNam: String): MabsConditionEntity {
        val stock = stockRepository.findByCode(codeNam).get()
        val condition = MabsConditionEntity(
            stock = stock,
            periodType = PERIOD_DAY,
            upBuyRate = 0.00,
            downSellRate = 0.00,
            shortPeriod = 1,
            longPeriod = 200,
            comment = ""
        )
        mabsBacktestService.saveCondition(condition)
        mabsBacktestService.runTest(condition)

        val tradeList = mabsTradeRepository.findByCondition(condition)
        condition.tradeList = tradeList

        return condition
    }

    private fun deleteBacktestData() {
        mabsTradeRepository.deleteAll()
        mabsConditionRepository.deleteAll()
    }

    private fun ????????????_??????() {
        val pairList = listOf(
            Pair(3, 20),
            Pair(5, 20),
            Pair(10, 20),
            Pair(3, 25),
            Pair(5, 25),
            Pair(10, 25),
            Pair(5, 30),
            Pair(10, 30),
            Pair(20, 30),
            Pair(5, 40),
            Pair(10, 40),
            Pair(20, 40),
            Pair(30, 40),
            Pair(5, 50),
            Pair(10, 50),
            Pair(20, 50),
            Pair(30, 50),
            Pair(5, 60),
            Pair(10, 60),
            Pair(20, 60),
            Pair(30, 60),
            Pair(5, 80),
            Pair(10, 80),
            Pair(20, 80),
            Pair(30, 80),
            Pair(5, 90),
            Pair(10, 90),
            Pair(20, 90),
            Pair(30, 90),
            Pair(5, 100),
            Pair(10, 100),
            Pair(20, 100),
            Pair(30, 100),
            Pair(5, 110),
            Pair(10, 110),
            Pair(20, 110),
            Pair(30, 110),
            Pair(5, 120),
            Pair(10, 120),
            Pair(20, 120),
            Pair(30, 120),
        )

        val rateList = listOf(0.01, 0.005, 0.001)
        val stockList = stockRepository.findAll()
        stockList
            .forEach { stock ->
                pairList.forEach { periodPair ->
                    rateList.forEach { rate ->
                        val mabsCondition = MabsConditionEntity(
                            stock = stock,
                            periodType = PERIOD_DAY,
                            upBuyRate = rate,
                            downSellRate = rate,
                            periodPair.first,
                            periodPair.second,
                            ""
                        )
                        mabsBacktestService.saveCondition(mabsCondition)
                    }
                }
            }
    }

    private fun ????????????_??????() {
        val pairList = listOf(
            Pair(1, 5),
            Pair(2, 5),
            Pair(3, 5),
            Pair(1, 8),
            Pair(2, 8),
            Pair(3, 8),
            Pair(1, 10),
            Pair(2, 10),
            Pair(3, 10),
            Pair(5, 10),
            Pair(2, 20),
            Pair(3, 20),
            Pair(4, 20),
            Pair(5, 20),
            Pair(3, 25),
            Pair(5, 25),
            Pair(7, 25),
            Pair(10, 25),
            Pair(12, 25),
            Pair(3, 30),
            Pair(5, 30),
            Pair(7, 30),
            Pair(10, 30),
            Pair(13, 30),
        )

        val rateList = listOf(0.01, 0.005, 0.001)
        val stockList = stockRepository.findAll()
        stockList
            .forEach { stock ->
                pairList.forEach { periodPair ->
                    rateList.forEach { rate ->
                        val mabsCondition = MabsConditionEntity(
                            stock = stock,
                            periodType = PERIOD_WEEK,
                            upBuyRate = rate,
                            downSellRate = rate,
                            periodPair.first,
                            periodPair.second,
                            ""
                        )
                        mabsBacktestService.saveCondition(mabsCondition)
                    }
                }
            }
    }

    private fun ????????????_??????() {
        val pairList = listOf(
            Pair(1, 2),
            Pair(1, 3),
            Pair(1, 4),
            Pair(1, 5),
            Pair(1, 6),
            Pair(1, 7),
            Pair(1, 8),
            Pair(1, 9),
            Pair(1, 10),
            Pair(1, 11),
            Pair(1, 12),

            Pair(2, 3),
            Pair(2, 4),
            Pair(2, 5),
            Pair(2, 6),
            Pair(2, 7),
            Pair(2, 8),
            Pair(2, 9),
            Pair(2, 10),
            Pair(2, 11),
            Pair(2, 12),

            Pair(3, 4),
            Pair(3, 5),
            Pair(3, 6),
            Pair(3, 7),
            Pair(3, 8),
            Pair(3, 9),
            Pair(3, 10),
            Pair(3, 11),
            Pair(3, 12),
        )

        val rateList = listOf(0.01, 0.005, 0.001)
        val stockList = stockRepository.findAll()
        stockList
            .forEach { stock ->
                pairList.forEach { periodPair ->
                    rateList.forEach { rate ->
                        val mabsCondition = MabsConditionEntity(
                            stock = stock,
                            periodType = PERIOD_MONTH,
                            upBuyRate = rate,
                            downSellRate = rate,
                            periodPair.first,
                            periodPair.second,
                            ""
                        )
                        mabsBacktestService.saveCondition(mabsCondition)
                    }
                }
            }
    }

    private fun allConditionReportMake() {
        val conditionList = mabsConditionRepository.findAll()
            .filter {
                // tradeList ????????? ?????? ?????????. ?????? ????????? ????????? tradeList size??? 0??? ???????????? ????????? ???????????? ????????????
                entityManager.refresh(it)
                it.tradeList.size > 1
            }.toList()

        var i = 0
        val mabsConditionList = conditionList.map {
            val range = DateRange(LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now())
            val priceRange = candleRepository.findByCandleDateTimeBetween(listOf(it.stock), range.from, range.to)

            val mabsAnalysisCondition = MabsAnalysisCondition(
                tradeConditionList = listOf(it),
                basic = TradeCondition(
                    range = priceRange,
                    investRatio = 0.99,
                    cash = 10_000_000.0,
                    feeBuy = 0.001,
                    feeSell = 0.001,
                    comment = ""
                )
            )
            log.info("?????? ?????? ?????? ?????? ${++i}/${conditionList.size}")
            mabsAnalysisCondition
        }.toList()
        analysisService.makeSummaryReport(mabsConditionList)
    }


}