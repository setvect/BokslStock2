package com.setvect.bokslstock2.backtest.dart.service

import com.setvect.bokslstock2.backtest.dart.model.FilterCondition
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class DartStructuringServiceTest {

    @Autowired
    lateinit var dartStructuringService: DartStructuringService

    @Test
    fun load() {
        val runtime = Runtime.getRuntime()

        println("1. Max memory: " + runtime.maxMemory() / (1024 * 1024) + "MB")
        println("1. Used memory: " + (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + "MB")

        val filter = FilterCondition(
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
}