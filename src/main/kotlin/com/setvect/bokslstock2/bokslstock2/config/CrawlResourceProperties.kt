package com.setvect.bokslstock2.bokslstock2.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * 클로링 설정보
 */

@ConstructorBinding
@ConfigurationProperties(prefix = "com.setvect.bokslstock.crawl")
data class CrawlResourceProperties(
    val url: UrlCollection,
    val userAgent: String
) {
    data class UrlCollection(
        val stockList: String,
        val companyInfo: String,
        val marketPrice: String
    )
}
