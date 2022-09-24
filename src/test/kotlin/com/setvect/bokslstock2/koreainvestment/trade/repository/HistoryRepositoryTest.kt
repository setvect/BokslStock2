package com.setvect.bokslstock2.koreainvestment.trade.repository

import com.setvect.bokslstock2.koreainvestment.trade.model.web.AssetHistorySearchForm
import com.setvect.bokslstock2.koreainvestment.trade.model.web.AssetPeriodHistorySearchForm
import com.setvect.bokslstock2.koreainvestment.trade.model.web.TradeSearchForm
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
internal class HistoryRepositoryTest {
    @Autowired
    private lateinit var historyRepository: HistoryRepository

    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun pageTrade() {
        val page = historyRepository.pageTrade(TradeSearchForm(), PageRequest.of(0, 2))
        log.info("##################################### ${page.totalElements}")
        page.forEach {
            println(it)
        }
    }

    @Test
    fun pageAssetHistory() {
        val page = historyRepository.pageAssetHistory(AssetHistorySearchForm(), PageRequest.of(0, 2))
        log.info("##################################### ${page.totalElements}")
        page.forEach {
            println(it)
        }
    }

    @Test
    fun pageAssetPeriodHistory() {
        val page = historyRepository.pageAssetPeriodHistory(AssetPeriodHistorySearchForm(), PageRequest.of(0, 3))
        log.info("##################################### ${page.totalElements}")
        page.forEach {
            println(it)
        }
    }
}