package com.setvect.bokslstock2.index.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.setvect.bokslstock2.config.CrawlResourceProperties
import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_DAY
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.DateUtil
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod.GET
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.io.File
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.streams.toList


@Service
class CrawlStockPriceService(

    private val crawlResourceProperties: CrawlResourceProperties,
    private val stockRepository: StockRepository,
    private val candleRepository: CandleRepository,
    private val crawlRestTemplate: RestTemplate,
) {
    companion object {
        private val START_DATE = LocalDateTime.of(1991, 1, 1, 0, 0)
    }

    @Autowired
    private lateinit var csvStoreService: CsvStoreService

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 등록된 모든 종목에 대한 시세 데이터를 삭제하고 다시 수집
     */
    @Transactional
    fun crawlStockPriceAll() {
        val stockEntities = stockRepository.findAll()
        stockEntities.forEach {
            val deleteCount = candleRepository.deleteByStock(it)
            log.info("시세 데이터 삭제: ${it.name}(${it.code}) - ${String.format("%,d", deleteCount)}건")
            if (NumberUtils.isDigits(it.code)) {
                crawlStockPrice(it.code)
            } else {
                crawlStockPriceYahoo(it.code)
            }
        }
    }

    /**
     * 전체 기간 수집
     */
    fun crawlStockPrice(code: String) {
        val stockEntityOptional = stockRepository.findByCode(code)
        checkExistStock(stockEntityOptional)
        saveCandleEntityList(stockEntityOptional, DateRange(START_DATE, LocalDateTime.now()))
    }


    /**
     * 등록된 모든 종목에 대한 증분 시세 수집
     */
    fun crawlStockPriceIncremental() {
        val stockEntities = stockRepository.findAll()
        stockEntities.forEach {
            if (NumberUtils.isDigits(it.code)) {
                crawlStockPriceIncremental(it.code)
            } else {
                crawlStockPriceYahoo(it.code)
            }
        }
    }

    private fun crawlStockPriceYahoo(code: String) {
        val from = DateUtil.getUnixTime(LocalDate.of(1994, 1, 1))
        val to = DateUtil.getUnixTimeCurrent()
        val url = URL("https://query1.finance.yahoo.com/v7/finance/download/${code}?period1=${from}&period2=${to}")
        log.info("call: $url")

        url.openStream().use { outputStream ->
            val csvFile = File("./data_source/", "${code}.csv")

            log.info("$code downloading from $url")
            FileUtils.copyInputStreamToFile(outputStream, csvFile)
            log.info("$code complete")
            csvStoreService.store(code, csvFile)
            log.info("$code store complete")
            TimeUnit.SECONDS.sleep(3)
        }
    }

    /**
     *  마지막 수집일 부터 [end]까지 [code]종목을 시세 수집
     */
    fun crawlStockPriceIncremental(code: String, end: LocalDateTime = LocalDateTime.now()) {
        val stockEntityOptional = stockRepository.findByCode(code)
        checkExistStock(stockEntityOptional)

        val priceList = candleRepository.list(
            stockEntityOptional.get(), PageRequest.of(0, 1, Sort.by("candleDateTime").descending())
        )
        val start: LocalDateTime = if (priceList.isNotEmpty()) {
            // 완전한 시세 정보를 얻기 위해 마지막 시세 정보를 삭제함
            candleRepository.delete(priceList[0])
            priceList[0].candleDateTime
        } else {
            START_DATE
        }
        saveCandleEntityList(stockEntityOptional, DateRange(start, end))
    }

    private fun saveCandleEntityList(stockEntityOptional: Optional<StockEntity>, range: DateRange) {
        val stockList = crawlResourceProperties.url.stockPrice
        val url = stockList.replace("{code}", stockEntityOptional.get().code)
            .replace("{start}", range.getFromDateTimeFormat("yyyyMMdd"))
            .replace("{end}", range.getToDateTimeFormat("yyyyMMdd"))

        log.info("crawl: $url")

        val parameter: MutableMap<String, Any> = HashMap()
        parameter["User-Agent"] = crawlResourceProperties.userAgent
        val httpEntity = HttpEntity<Map<String, Any>>(parameter)

        val result = crawlRestTemplate.exchange(url, GET, httpEntity, String::class.java)
        val body = result.body ?: throw RuntimeException("JSON 결과 없음")

        // json format 맞추기
        body.replace("'", "\"")
        val priceList: ArrayList<ArrayList<String>> =
            Gson().fromJson(body, object : TypeToken<ArrayList<ArrayList<String>>>() {}.type)
        val candleList = priceList.stream().skip(1).map { row ->
            CandleEntity(
                stock = stockEntityOptional.get(),
                periodType = PERIOD_DAY,
                candleDateTime = DateUtil.getLocalDateTime(row[0] + "000000", "yyyyMMddHHmmss"),
                openPrice = row[1].toDouble(),
                highPrice = row[2].toDouble(),
                lowPrice = row[3].toDouble(),
                closePrice = row[4].toDouble(),
            )
        }.toList()

        if (candleList.isEmpty()) {
            log.info("empty candle.")
            return
        }

        candleRepository.saveAll(candleList)
        log.info("save count: ${candleList.size}, from=${candleList[0].candleDateTime}, to=${candleList[candleList.size - 1].candleDateTime}")
    }

    /**
     * 종목정보가 있는지 체크
     */
    private fun checkExistStock(stockEntityOptional: Optional<StockEntity>) {
        if (stockEntityOptional.isEmpty) {
            throw RuntimeException("종목 코드 정보가 없어요")
        }
    }
}