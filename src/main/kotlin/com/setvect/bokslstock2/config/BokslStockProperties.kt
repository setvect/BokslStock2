package com.setvect.bokslstock2.config

import com.setvect.bokslstock2.analysis.common.model.StockCode
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
        /** 계좌 번호 */
        val accountNo: String,
        /** 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자*/
        val investRatio: Double,
        val stock: List<VbsStock>,
    ) {
        data class VbsStock(
            val code: String,
            val openSell: Boolean, // TODO 해당 속성 없애기. 갭상승 유지로 판단
            val k: Double
        ) {
            fun getName(): String? {
                return StockCode.findByCodeOrNull(code)?.desc
            }
        }
    }
}
