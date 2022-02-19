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
import java.time.LocalDateTime
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod.GET
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import kotlin.streams.toList


@Service
class CrawlService(

    private val crawlResourceProperties: CrawlResourceProperties,
    private val stockRepository: StockRepository,
    private val candleRepository: CandleRepository,
    private val crawlRestTemplate: RestTemplate,
) {
    companion object {
        private val START_DATE = LocalDateTime.of(1991, 1, 1, 0, 0)
    }

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 등록된 모든 종목에 대한 시세 데이터를 삭제하고 다시 수집
     */
    @Transactional
    fun crawlStock() {
        val stockEntities = stockRepository.findAll()
        stockEntities.forEach {
            val deleteCount = candleRepository.deleteByStock(it)
            log.info("시세 데이터 삭제: ${it.name}(${it.code}) - ${String.format("%,d", deleteCount)}건")
            crawlStock(it.code)
        }
    }

    /**
     * 기존 수집된 데이터를 모두 지우고 전체 기간 수집
     */
    fun crawlStock(code: String) {
        val stockEntityOptional = stockRepository.findByCode(code)
        checkExistStock(stockEntityOptional)
        saveCandleEntityList(stockEntityOptional, DateRange(START_DATE, LocalDateTime.now()))
    }


    /**
     * 등록된 모든 종목에 대한 증분 시세 수집
     */
    fun crawlStockIncremental() {
        val stockEntities = stockRepository.findAll()
        stockEntities.forEach {
            crawlStockIncremental(it.code)
        }
    }

    /**
     *  마지막 수집일 부터 [end]까지 [code]종목을 시세 수집
     */
    fun crawlStockIncremental(code: String, end: LocalDateTime = LocalDateTime.now()) {
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
        val stockList = crawlResourceProperties.url.marketPrice
        val url = stockList.replace("{code}", stockEntityOptional.get().code)
            .replace("{start}", range.getFromDateTimeFormat("yyyyMMdd"))
            .replace("{end}", range.getToDateTimeFormat("yyyyMMdd"))

        log.info("crawl: $url")

        val httpEntity = HttpEntity<Map<String, Any>>(Collections.emptyMap())
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