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
    val koreainvestment: Koreainvestment,
    val restTemplate: RestTemplate,
    val slack: Slack,
) {
    data class Crawl(
        val korea: Korea,
        val global: Global,
        val exchangeRate: ExchangeRate,
    ) {
        data class Korea(
            val url: UrlCollection,
            val userAgent: String,
            val savePath: String,
        ) {
            data class UrlCollection(
                val list: String,
                val info: String,
                val stockPrice: String
            )

        }

        data class Global(
            val url: String
        ) {
        }

        data class ExchangeRate(
            val url: String
        ) {
        }
    }

    data class Koreainvestment(
        val appkey: String,
        val appsecret: String,
        val ws: Ws,
        val trade: Trade,
        val vbs: Vbs
    ) {
        data class Ws(val url: String) {
        }

        data class Trade(val url: String) {
        }
    }

    data class RestTemplate(
        val connectionTimeoutMs: Int,
        val readTimeoutMs: Int
    )

    data class Slack(
        val enable: Boolean,
        val token: String,
        val channelId: String,
    )

    data class Vbs(
        val stockCode: List<String>,
    )
}
