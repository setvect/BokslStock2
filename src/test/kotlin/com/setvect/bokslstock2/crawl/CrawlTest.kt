package com.setvect.bokslstock2.crawl

import com.setvect.bokslstock2.StockCode
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.index.service.CrawlService
import com.setvect.bokslstock2.index.service.CsvStoreService
import java.io.File
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
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var crawlService: CrawlService

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var csvStoreService: CsvStoreService

    @Test
    fun addStock() {
        StockCode.STOCK_CODE_MAP.forEach {
            val stockEntityOptional = stockRepository.findByCode(it.key)
            if (stockEntityOptional.isEmpty) {
                log.info("종목 등록: $it")
                stockRepository.save(StockEntity(code = it.key, name = it.value))
            }
        }

        StockCode.OVERSEAS_STOCK_CODE_MAP.forEach {
            val stockEntityOptional = stockRepository.findByCode(it.key)
            if (stockEntityOptional.isEmpty) {
                log.info("종목 등록: $it")
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

    // ---------
    @Test
    fun storeCsv() {
        var csvStock = File("./data_source/spy_us_d.csv")
        csvStoreService.store(StockCode.OS_CODE_SPY, csvStock)
        csvStock = File("./data_source/vss_us_d.csv")
        csvStoreService.store(StockCode.OS_CODE_VSS, csvStock)
        csvStock = File("./data_source/tlt_us_d.csv")
        csvStoreService.store(StockCode.OS_CODE_TLT, csvStock)
    }
}