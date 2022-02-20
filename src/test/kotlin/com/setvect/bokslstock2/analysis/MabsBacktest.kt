package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.StockCode
import com.setvect.bokslstock2.analysis.common.model.BasicAnalysisCondition
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
import com.setvect.bokslstock2.index.service.MovingAverageService
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
    private lateinit var movingAverageService: MovingAverageService

    @Autowired
    private lateinit var backtestService: MabsBacktestService

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
    fun 한번에_모두_백테스트() {
        // 0. 기존 백테스트 기록 모두 삭제
        deleteBacktestData()

        // 1. 조건 만들기
        조건생성_일봉()
        조건생성_주봉()
        조건생성_월봉()

        // 2. 모든 조건에 대해 백테스트
        backtestService.runTestBatch()

        // 3. 모든 조건에 대한 리포트 만들기
        allConditionReportMake()
    }

    @Test
    @Transactional
    fun 이동평균계산() {
        val movingAverage =
            movingAverageService.getMovingAverage(StockCode.CODE_069500, PERIOD_DAY, listOf(5, 20, 60, 120))
//        movingAverageService.getMovingAverage(StockCode.CODE_069500, PeriodType.PERIOD_WEEK, listOf(5, 20, 60, 120))
//        movingAverageService.getMovingAverage(StockCode.CODE_069500, PeriodType.PERIOD_MONTH, listOf(5, 20, 60, 120))

        movingAverage.forEach {
            val avgInfo = it.average.entries
                .map { entry -> "이동평균(${entry.key}): ${it.average[entry.key]}" }
                .toList()
                .joinToString(", ")
            println("${it.candleDateTimeStart}~${it.candleDateTimeEnd} - O: ${it.openPrice}, H: ${it.highPrice}, L: ${it.lowPrice}, C:${it.closePrice}, ${it.periodType}, $avgInfo")
        }

//        movingAverage.backtest()
    }

    @Test
    @Transactional
    fun 종목과_시세조회() {
        val stockOp = stockRepository.findByCode(StockCode.CODE_069500)
        val stock = stockOp.get()

        log.info("${stock.name}(${stock.code}) ${stock.candleList.size}")
        stock.candleList.forEach {
            log.info("${it.candleDateTime}: close:${String.format("%,d", it.closePrice)}")
        }
        println("끝.")
    }

    @Test
    fun 조건생성() {
        조건생성_일봉()
    }


    @Test
    @Transactional
    @Rollback(false)
    fun 백테스트() {
        backtestService.runTestBatch()
    }

    @Test
    @Transactional
    fun 멀티_리포트생성() {
//        val elementList = listOf(951551, 951255) // 평균 수익률 - TIGER 차이나CSI300, TIGER 미국나스닥100
        val elementList = listOf(950589L, 950064L) // 최고 수익률 - TIGER 차이나CSI300, TIGER 미국나스닥100
//        val elementList = listOf(952722, 950164) // 최악 수익률 - TIGER 차이나CSI300, TIGER 미국나스닥100
//        val elementList = listOf(949078, 951062) // 코스닥150 레버리지, KODEX 코스닥150선물인버스
//        val elementList = listOf(949078, 949331) // 코스닥150 레버리지, KODEX 레버리지
//        val elementList = listOf(949079) // 코스닥150 레버리지
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
                // 시세가 포함된 날짜범위 지정
                val realRangeList =
                    conditionList.map { candleRepository.findByCandleDateTimeBetween(it.stock, range.from, range.to) }
                        .toList()
                val from = realRangeList.minOf { it.from }
                val to = realRangeList.maxOf { it.to }
                val realRange = DateRange(from, to)

                MabsAnalysisCondition(
                    tradeConditionList = conditionList,
                    basic = BasicAnalysisCondition(
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
    fun 단건_리포트생성() {
        val range = DateRange(LocalDateTime.of(2016, 1, 1, 0, 0), LocalDateTime.now())
        val conditionList = mabsConditionRepository.listBySeq(listOf(949092))
        val mabsAnalysisCondition = MabsAnalysisCondition(
            tradeConditionList = conditionList,
            basic = BasicAnalysisCondition(
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
     * 백테스트 건수에 따라 Heap 사이즈 조절
     * -Xms2G -Xmx2G
     */
    @Test
    @Transactional
    fun 모든매매조건_단건기준_리포트생성() {
        allConditionReportMake()
    }

    private fun deleteBacktestData() {
        mabsTradeRepository.deleteAll()
        mabsConditionRepository.deleteAll()
    }

    private fun 조건생성_일봉() {
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

        val rateList = listOf<Double>(0.01, 0.005, 0.001)
        val stockList = stockRepository.findAll()
        stockList.forEach { stock ->
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
                    backtestService.saveCondition(mabsCondition)
                }
            }
        }
    }

    private fun 조건생성_주봉() {
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

        val rateList = listOf<Double>(0.01, 0.005, 0.001)
        val stockList = stockRepository.findAll()
        stockList.forEach { stock ->
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
                    backtestService.saveCondition(mabsCondition)
                }
            }
        }
    }

    private fun 조건생성_월봉() {
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

        val rateList = listOf<Double>(0.01, 0.005, 0.001)
        val stockList = stockRepository.findAll()
        stockList.forEach { stock ->
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
                    backtestService.saveCondition(mabsCondition)
                }
            }
        }
    }

    private fun allConditionReportMake() {
        val conditionList = mabsConditionRepository.findAll().filter {
            // tradeList 정보를 다시 읽어옴. 해당 구분이 없으면 tradeList size가 0인 상태에서 캐싱된 데이터가 불러와짐
            entityManager.refresh(it)
            it.tradeList.size > 1
        }.toList()

        var i = 0
        val mabsConditionList = conditionList.map {
            val range = DateRange(LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.now())
            val priceRange = candleRepository.findByCandleDateTimeBetween(it.stock, range.from, range.to)

            val mabsAnalysisCondition = MabsAnalysisCondition(
                tradeConditionList = listOf(it),
                basic = BasicAnalysisCondition(
                    range = priceRange,
                    investRatio = 0.99,
                    cash = 10_000_000.0,
                    feeBuy = 0.001,
                    feeSell = 0.001,
                    comment = ""
                )
            )
            log.info("거래 내역 조회 진행 ${++i}/${conditionList.size}")
            mabsAnalysisCondition
        }.toList()
        analysisService.makeSummaryReport(mabsConditionList)
    }


}