package com.setvect.bokslstock2.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * 클로링 설정보
 */

@ConstructorBinding
@ConfigurationProperties(prefix = "com.setvect.bokslstock")
data class BokslStockProperties(
    val crawl: Crawl,
    val slack: Slack,
) {
    data class Crawl(
        val korea: Korea,
        val global: Global,
    ) {
        data class Korea(
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

        data class Global(
            val url: String
        ) {
        }
    }

    data class Slack(
        val enable: Boolean,
        val token: String,
        val channelId: String,
    )
}
