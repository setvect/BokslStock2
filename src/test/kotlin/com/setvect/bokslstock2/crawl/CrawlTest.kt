package com.setvect.bokslstock2.crawl

import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.index.service.CrawlService
import com.setvect.bokslstock2.util.DateRange
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("local")
class CrawlTest {
    companion object {
        private val stockCodeList = mapOf(
            "069500" to "KODEX 200",
            "122630" to "KODEX 레버리지",
            "252670" to "KODEX 200선물인버스2X",
            "229200" to "KODEX 코스닥 150",
            "233740" to "KODEX 코스닥150 레버리지",
            "251340" to "KODEX 코스닥150선물인버스",
            "114800" to "KODEX 인버스",
            "005930" to "삼성전자",
        )
    }

    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var crawlService: CrawlService

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Test
    fun testStockCode() {
        stockCodeList.forEach {
            val stockEntityOptional = stockRepository.findByCode(it.key)
            if (stockEntityOptional.isEmpty) {
                println("종목 등록: $it")
                stockRepository.save(StockEntity(code = it.key, name = it.value))
            }
        }
        println("끝.")
    }

    @Test
    fun crawlIncremental() {
        val stockEntities = stockRepository.findAll()
        stockEntities.forEach {
            crawlService.crawlStockIncremental(it.code, LocalDateTime.now())
        }
        println("끝.")
    }

    @Test
    @Disabled
    fun crawlAll() {
        val range = DateRange("2015-12-17T00:00:00", "2022-01-01T00:00:00")
//        crawlService.crawlStock(STOCK_233740, range)
    }
}