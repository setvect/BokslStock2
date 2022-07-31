package com.setvect.bokslstock2.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * 클로링 설정보
 */

@ConstructorBinding
@ConfigurationProperties(prefix = "com.setvect.bokslstock.company-crawl")
data class CrawlResourceProperties(
    val url: UrlCollection,
    val config: Config,
    val userAgent: String,
    val savePath: String,
) {
    data class UrlCollection(
        val list: String,
        val info: String,
        val stockPrice: String
    )

    data class Config(
        val connectionTimeoutMs: Int,
        val readTimeoutMs: Int
    )
}
