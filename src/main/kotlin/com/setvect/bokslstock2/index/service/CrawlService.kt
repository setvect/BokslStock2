package com.setvect.bokslstock2.index.service

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.setvect.bokslstock2.config.CrawlResourceProperties
import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_DAY
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.DateUtil
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
        if (stockEntityOptional.isEmpty) {
            throw RuntimeException("종목 코드 정보가 없어요")
        }

        val stockList = crawlResourceProperties.url.marketPrice
        val url = stockList
            .replace("{code}", stockEntityOptional.get().code)
            .replace("{start}", range.getFromDateTimeFormat("yyyyMMdd"))
            .replace("{end}", range.getToDateTimeFormat("yyyyMMdd"))

        val httpEntity = HttpEntity<Map<String, Any>>(Collections.emptyMap())
        val result = crawlRestTemplate.exchange(url, GET, httpEntity, String::class.java)
        val body = result.body ?: throw RuntimeException("JSON 결과 없음")

        body.replace("'", "\"")
        val priceList: ArrayList<ArrayList<String>> =
            Gson().fromJson(body, object : TypeToken<ArrayList<ArrayList<String>>>() {}.type)

        val candleList = priceList.stream().skip(1).map { row ->
            CandleEntity(
                stock = stockEntityOptional.get(),
                periodType = PERIOD_DAY,
                candleDateTime = DateUtil.getLocalDateTime(row[0] + "000000", "yyyyMMddHHmmss"),
                openPrice = Integer.parseInt(row[1]),
                highPrice = Integer.parseInt(row[2]),
                lowPrice = Integer.parseInt(row[3]),
                closePrice = Integer.parseInt(row[4]),
            )
        }

        for (candle in candleList) {
            println(candle)
        }
    }
}