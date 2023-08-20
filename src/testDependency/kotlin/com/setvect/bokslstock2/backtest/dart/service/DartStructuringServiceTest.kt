package com.setvect.bokslstock2.backtest.dart.service

import com.setvect.bokslstock2.backtest.dart.model.*
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import com.setvect.bokslstock2.util.JsonUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class DartStructuringServiceTest {

    @Autowired
    private lateinit var dartStructuringService: DartStructuringService

    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun load() {
        val runtime = Runtime.getRuntime()

        println("1. Max memory: " + runtime.maxMemory() / (1024 * 1024) + "MB")
        println("1. Used memory: " + (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + "MB")

//        val filter = DartFilter(year = emptySet(), quarter = emptySet(), stockCodes = emptySet())

        val filter = DartFilter(
            year = setOf(2019, 2020, 2021, 2022, 2023),
            quarter = setOf(ReportCode.QUARTER1, ReportCode.ANNUAL),
            stockCodes = setOf()
        )
        dartStructuringService.loadFinancial(filter)
        println("2. Max memory: " + runtime.maxMemory() / (1024 * 1024) + "MB")
        println("2. Used memory: " + (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + "MB")

        dartStructuringService.loadStockQuantity(filter)
        println("3. Max memory: " + runtime.maxMemory() / (1024 * 1024) + "MB")
        println("3. Used memory: " + (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + "MB")

        dartStructuringService.loadDividend(filter)
        println("4. Max memory: " + runtime.maxMemory() / (1024 * 1024) + "MB")
        println("4. Used memory: " + (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + "MB")
    }

    @Test
    fun searchFinancial() {
//        findAndPrint("자본총계")
//        findAndPrint("당기순이익")
//        findAndPrint("영업이익")
        findAndPrint("매출액")
    }

    private fun findAndPrint(name: String) {
        val filter = DartFilter(
            year = setOf(2022, 2023),
            quarter = ReportCode.values().toSet(),
//            stockCodes = setOf("008110"), // 1분기 마감
//            stockCodes = setOf("005390"), // 2분기 마감
//            stockCodes = setOf("003610"), // 3분기 마감
            stockCodes = setOf("005930"), // 4분기 마감
        )
        dartStructuringService.loadFinancial(filter)

        val condition = FinancialSearchCondition(
            accountNm = setOf(name),
            fsDiv = setOf(FinancialFs.CFS),
        )
        val result = dartStructuringService.searchFinancial(condition)

        println("\n------- $name, size: ${result.size} -------")

        result.forEach {
            println("\n----------------------------\n")
            println(JsonUtil.mapper.writeValueAsString(it))
        }
    }

    @Test
    fun getAccountClose() {
        // 008110  1분기 마감
        // 005390  2분기 마감
        // 003610  3분기 마감
        // 005930  4분기 마감

        val filter = DartFilter(
            year = setOf(2022, 2023),
            quarter = ReportCode.values().toSet(),
            stockCodes = setOf("008110", "005390", "003610", "005930")
        )
        dartStructuringService.loadFinancial(filter)

        filter.stockCodes.forEach {
            val result = dartStructuringService.getAccountClose(it)
            println("accountClose: $it, ${result}")
        }
    }

    @Test
    fun getIndustryType() {
        val filter = DartFilter(
            year = setOf(2021, 2022),
            quarter = ReportCode.values().toSet(),
            stockCodes = setOf("008110", "005930", "304100", "377300", "352820")
        )
        dartStructuringService.loadFinancial(filter)

        filter.stockCodes.forEach { stockCode ->
            val industryType = dartStructuringService.getIndustryType(stockCode)
            println("industryType: $stockCode, $industryType")
        }
    }

    /**
     * 분기별 손익 계산서
     */
    @Test
    fun getIncomeStatement() {
        val filter = DartFilter(
            year = linkedSetOf(2021, 2022),
            quarter = ReportCode.values().toSet(),
            stockCodes = linkedSetOf("008110", "005390", "003610", "005930", "304100")
        )

        // 검증은 아래 링크에서
        // https://finance.naver.com/item/coinfo.naver?code=005930

        dartStructuringService.loadFinancial(filter)
        dartStructuringService.loadFinancialDetail(filter)

        FinancialDetailMetric.values().forEach {
            filter.stockCodes.forEach { stockCode ->
                val incomeStatement = dartStructuringService.getFinancialItemValue(stockCode, 2022, it)
                println("2022년 ${it.accountName[0]}: ${incomeStatement}")
            }
            log.info("-------------")
        }
    }

    /**
     * 배당 정보
     * <pre>
     * - 결과 검증: https://finance.naver.com/item/coinfo.naver?code=005930
     * - 투자지표 -> 수익성 클릭
     * </pre>
     */
    @Test
    fun getDividend() {
        val filter = DartFilter(
            year = linkedSetOf(2021, 2022),
            quarter = ReportCode.values().toSet(),
            stockCodes = linkedSetOf("008110", "005390", "003610", "005930", "304100")
        )

        dartStructuringService.loadFinancial(filter)
        dartStructuringService.loadDividend(filter)

        filter.stockCodes.forEach { stockCode ->
            val dividen = dartStructuringService.getDividend(stockCode, 2022)
            println("${dividen.year}년, ${dividen.stockCode}, 배당: ${dividen}")
        }
    }
}