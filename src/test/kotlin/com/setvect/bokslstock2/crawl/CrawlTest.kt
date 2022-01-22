package com.setvect.bokslstock2.crawl

import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.index.service.CrawlService
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

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
            "133690" to "TIGER 미국나스닥100",
            "102110" to "TIGER 200",
            "091220" to "TIGER 은행",
            "161510" to "ARIRANG 고배당주",
            "192090" to "TIGER 차이나CSI300"
        )
    }

    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var crawlService: CrawlService

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Test
    fun addStock() {
        stockCodeList.forEach {
            val stockEntityOptional = stockRepository.findByCode(it.key)
            if (stockEntityOptional.isEmpty) {
                println("종목 등록: $it")
                stockRepository.save(StockEntity(code = it.key, name = it.value))
            }
        }
        println("끝.")
    }


    /**
     * 등록된 종목에 대한 시세 데이터를 모두 지우고 다시 수집
     */
    @Test
    @Disabled
    fun crawBatch() {
        crawlService.crawlStock()
        println("끝.")
    }

    /**
     * 등록된 종목에 대한 증분 시세 수집
     */
    @Test
    fun crawlIncremental() {
        crawlService.crawlStockIncremental()
        println("끝.")
    }
}