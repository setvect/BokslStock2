package com.setvect.bokslstock2.crawl.service

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.crawl.service.CrawlerStockPriceService
import com.setvect.bokslstock2.index.service.CsvStoreService
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.io.File

@SpringBootTest
@ActiveProfiles("test")
class CrawlerStockPriceServiceTest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var crawlerStockPriceService: CrawlerStockPriceService

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var csvStoreService: CsvStoreService

    @Test
    @Disabled
    fun addStock() {
        StockCode.values().forEach {
            val stockEntityOptional = stockRepository.findByCode(it.code)
            if (stockEntityOptional.isEmpty) {
                log.info("종목 등록: $it")
                stockRepository.save(StockEntity(code = it.code, name = it.desc))
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
        crawlerStockPriceService.crawlStockPriceAll()
        println("끝.")
    }

    /**
     *  시세 모두 지우고 다시 수집
     */
    @Test
    fun crawBatchStock() {
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.KOSEF_TREASURY_BOND_10_148070)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.TIGER_SNP_360750)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.KOSEF_200TR_294400)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.KODEX_GLD_H_132030)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.TIGER_USA_TREASURY_BOND_305080)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.ACE_GLD_411060)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.KODEX_200_USD_BOND_284430)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.KODEX_KOSDAQ_2X_233740, PeriodType.PERIOD_DAY)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.KODEX_200_069500, PeriodType.PERIOD_DAY)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.KODEX_BANK_091170, PeriodType.PERIOD_DAY)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.KODEX_KOSDAQ_229200, PeriodType.PERIOD_DAY)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.KODEX_KOSDAQ_IV_251340, PeriodType.PERIOD_DAY)
        println("끝.")
    }

    /**
     *  현금 시세 모두 지우고 다시 입력
     */
    @Test
    fun crawBatchCash() {
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.CASH_1)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.CASH_2)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.CASH_3)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.CASH_4)
        crawlerStockPriceService.crawlStockPriceWithDelete(StockCode.CASH_5)
        println("끝.")
    }

    /**
     * 등록된 종목에 대한 증분 시세 수집
     *
     * ※ 주의사항: 계속 증분 수집만 하다보면 수정주가가 반영되지 않아 다른(네이버 주식, HTS 등)과 맞지 않아 외곡이 발생함.
     * 그냥 매번 배치 수집하자.
     */
    @Test
    @Disabled
    @Deprecated("그냥 매번 배치 수집하자")
    fun crawlIncremental() {
        crawlerStockPriceService.crawlStockPriceIncremental()
//        crawlStockPriceService.crawlStockPriceIncremental(setOf(StockCode.TIGER_USD_SHORT_BONDS_329750))
        println("끝.")
    }

    /**
     * 크래온으로 수집한 시세 정보 저장
     *
     * 크래온 수집은 본 프로젝트에서 하지 않음. 여기선 수집 결과 CSV만 사용해서 DB 입력 작업함
     */
    @Test
//    @Disabled
    fun crawCron() {
        val stockCodes = listOf(
            StockCode.KODEX_200_069500,
            StockCode.KODEX_BANK_091170,
            StockCode.KODEX_IV_114800,
            StockCode.KODEX_2X_122630,
            StockCode.KODEX_KOSDAQ_2X_233740,
            StockCode.KODEX_IV_2X_252670
        )
        stockCodes.forEach {
            val csvFile = File("data_source/cron/5_minute_20230513", "A${it.code}.csv")
            if (!csvFile.exists()) {
                log.warn("파일 없음: ${csvFile.absolutePath}")
                return@forEach
            }
            csvStoreService.storeFromCron(it.code, csvFile, true)
        }
    }
}