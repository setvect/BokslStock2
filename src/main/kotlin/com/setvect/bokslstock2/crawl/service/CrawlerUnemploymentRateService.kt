package com.setvect.bokslstock2.crawl.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.setvect.bokslstock2.config.BokslStockProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod.GET
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.File
import java.time.LocalDate


/**
 * 미국 실업률 통계 수집
 */
@Service
class CrawlerUnemploymentRateService(
    private val bokslStockProperties: BokslStockProperties,
    private val crawlRestTemplate: RestTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 미국 실업률 통계 수집
     */
    fun crawl(): UnemploymentData {
        val parameter: MutableMap<String, Any> = HashMap()
        val httpEntity = HttpEntity<Map<String, Any>>(parameter)

        val key = bokslStockProperties.crawl.unemploymentRate.key
        val result = crawlRestTemplate.exchange(
            "https://api.stlouisfed.org/fred/series/observations?series_id=UNRATE&api_key=$key&file_type=json",
            GET,
            httpEntity,
            UnemploymentData::class.java
        )
        return result.body ?: throw RuntimeException("결과 없음")
    }
}


data class UnemploymentData(
    @JsonProperty("realtime_start")
    val realtimeStart: LocalDate,

    @JsonProperty("realtime_end")
    val realtimeEnd: LocalDate,

    @JsonProperty("observation_start")
    val observationStart: LocalDate,

    @JsonProperty("observation_end")
    val observationEnd: LocalDate,

    val units: String,

    @JsonProperty("output_type")
    val outputType: Int,

    @JsonProperty("file_type")
    val fileType: String,

    @JsonProperty("order_by")
    val orderBy: String,

    @JsonProperty("sort_order")
    val sortOrder: String,

    val count: Int,
    val offset: Int,
    val limit: Int,

    val observations: List<Observation>
)

data class Observation(
    @JsonProperty("realtime_start")
    val realtimeStart: LocalDate,

    @JsonProperty("realtime_end")
    val realtimeEnd: LocalDate,

    val date: LocalDate,
    val value: String
)
