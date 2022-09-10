package com.setvect.bokslstock2.koreainvestment.ws.service

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.ws.model.WsRequest
import com.setvect.bokslstock2.koreainvestment.ws.model.WsTransaction
import com.setvect.bokslstock2.slack.SlackMessageService
import com.setvect.bokslstock2.util.JsonUtil
import org.apache.juli.logging.LogFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * 실시간으로 시세를 확인 하여 매매 진행
 */
@Component
class TradingWebsocket(
    private val publisher: ApplicationEventPublisher,
    private val slackMessageService: SlackMessageService,
    private val bokslStockProperties: BokslStockProperties
) {
    private var websocketClientEndpoint: WebsocketClientEndpoint? = null
    private val log = LogFactory.getLog(javaClass)

    fun open() {
        val koreainvestment = bokslStockProperties.koreainvestment

        websocketClientEndpoint?.close()

        websocketClientEndpoint = WebsocketClientEndpoint(koreainvestment.ws.url, publisher, slackMessageService)

        bokslStockProperties.koreainvestment.vbs.stockCode.forEach { stockCode ->
            WsTransaction.values().forEach { transaction ->
                val parameter = WsRequest(
                    WsRequest.Header(
                        koreainvestment.appkey,
                        koreainvestment.appsecret,
                        "P", // 고객타입, P : 개인
                        "1", // 거래타입, 1 : 등록
                        "utf-8"
                    ),
                    WsRequest.Body(WsRequest.Input(transaction, stockCode))
                )
                val message = JsonUtil.mapper.writeValueAsString(parameter)
                websocketClientEndpoint!!.sendMessage(message)
            }
        }
    }

    fun close() {
        websocketClientEndpoint?.close()
        websocketClientEndpoint = null
    }
}
