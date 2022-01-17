package com.setvect.bokslstock2.index.service

import com.setvect.bokslstock2.config.CrawlResourceProperties
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateRange
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod.GET
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class CrawlService(
    private val crawlResourceProperties: CrawlResourceProperties,
    private val stockRepository: StockRepository,
    private val candleRepository: CandleRepository,
    private val crawlRestTemplate: RestTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun crawlStock(code: String, range: DateRange) {
        val stockEntityOptional = stockRepository.findByCode(code)
        if(stockEntityOptional.isEmpty){
            throw RuntimeException("종목 코드 정보가 없어요")
        }
        
        val stockList = crawlResourceProperties.url.marketPrice
        val url = stockList
            .replace("{code}", stockEntityOptional.get().code)
            .replace("{start}", range.getFromDateTimeFormat("yyyyMMdd"))
            .replace("{end}", range.getToDateTimeFormat("yyyyMMdd"))

        val httpEntity = HttpEntity<Map<String, Any>>(Collections.emptyMap())
        val result = crawlRestTemplate.exchange(url, GET, httpEntity, String::class.java)
        println(result)
    }
}