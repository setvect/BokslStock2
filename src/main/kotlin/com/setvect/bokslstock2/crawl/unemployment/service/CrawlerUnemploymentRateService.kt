package com.setvect.bokslstock2.crawl.unemployment.service

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.crawl.unemployment.model.UnemploymentData
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

/**
 * 미국 실업률 통계 수집
 */
@Service
class CrawlerUnemploymentRateService(
    private val bokslStockProperties: BokslStockProperties,
    private val crawlRestTemplate: RestTemplate,
) {
    /**
     * 미국 실업률 통계 수집
     */
    fun crawl(): UnemploymentData {
        val parameter: MutableMap<String, Any> = HashMap()
        val httpEntity = HttpEntity<Map<String, Any>>(parameter)

        val key = bokslStockProperties.crawl.fred.key
        val result = crawlRestTemplate.exchange(
            "https://api.stlouisfed.org/fred/series/observations?series_id=UNRATE&api_key=$key&file_type=json",
            HttpMethod.GET,
            httpEntity,
            UnemploymentData::class.java
        )
        return result.body ?: throw RuntimeException("결과 없음")
    }
}