package com.setvect.bokslstock2.config

import com.setvect.bokslstock2.backtest.common.model.StockCode
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
        val fred: Fred,
        val dart: Dart,
    ) {
        data class Fred(
            val key: String
        )
        data class Dart(
            val key: String
        )
    }

    data class Koreainvestment(
        val appkey: String,
        val appsecret: String,
        val ws: Ws,
        val trade: Trade,
        val vbs: Vbs
    ) {
        data class Ws(val url: String)

        data class Trade(val url: String)
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
            /**
             * false: 동시호가에 매도 되도록 예상 채결가 보다 낮게 매도
             * true: 5분마다 직전 5분봉을 체크해 시가 >= 종가 이면 매도, 아니면 유지
             */
            val stayGapRise: Boolean,
            val k: Double,
            /** 종목별 거래 비율(0 ~ 1) */
            val investmentRatio: Double
        ) {
            fun getName(): String? {
                return StockCode.findByCodeOrNull(code)?.desc
            }
        }
    }
}
