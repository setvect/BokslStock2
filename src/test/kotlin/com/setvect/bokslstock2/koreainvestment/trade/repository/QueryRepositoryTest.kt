package com.setvect.bokslstock2.koreainvestment.trade.repository

import com.setvect.bokslstock2.koreainvestment.trade.model.web.AssetHistorySearchForm
import com.setvect.bokslstock2.koreainvestment.trade.model.web.AssetPeriodHistorySearchForm
import com.setvect.bokslstock2.koreainvestment.trade.model.web.TradeSearchForm
import com.setvect.bokslstock2.koreainvestment.trade.repository.query.AssetHistorySelectRepository
import com.setvect.bokslstock2.koreainvestment.trade.repository.query.TradSelectRepository
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
internal class QueryRepositoryTest {

    @Autowired
    private lateinit var tradSelectRepository: TradSelectRepository

    @Autowired
    private lateinit var assetHistorySelectRepository: AssetHistorySelectRepository


    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun pageTrade() {
        val page = tradSelectRepository.pageTrade(TradeSearchForm(), PageRequest.of(0, 2))
        log.info("##################################### ${page.totalElements}")
        page.forEach {
            println(it)
        }
    }

    @Test
    fun pageAssetHistory() {
        val page = assetHistorySelectRepository.pageAssetHistory(AssetHistorySearchForm(), PageRequest.of(0, 2))
        log.info("##################################### ${page.totalElements}")
        page.forEach {
            println(it)
        }
    }

    @Test
    fun pageAssetPeriodHistory() {
        val page = assetHistorySelectRepository.pageAssetPeriodHistory(AssetPeriodHistorySearchForm(), PageRequest.of(0, 3))
        log.info("##################################### ${page.totalElements}")
        page.forEach {
            println(it)
        }
    }
}