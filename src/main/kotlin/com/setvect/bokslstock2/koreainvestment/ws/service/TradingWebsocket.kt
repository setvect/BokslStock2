package com.setvect.bokslstock2.koreainvestment.ws.service

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.ws.model.WsRequest
import com.setvect.bokslstock2.koreainvestment.ws.model.WsTransaction
import com.setvect.bokslstock2.slack.SlackMessageService
import com.setvect.bokslstock2.util.JsonUtil
import org.slf4j.LoggerFactory
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
    private val log = LoggerFactory.getLogger(javaClass)

    fun open() {
        log.info("웹소켓 시작")
        val koreainvestment = bokslStockProperties.koreainvestment

        websocketClientEndpoint?.close()

        websocketClientEndpoint = WebsocketClientEndpoint(koreainvestment.ws.url, publisher)

        bokslStockProperties.koreainvestment.vbs.stock.forEach { stock ->
            val parameter = WsRequest(
                WsRequest.Header(
                    koreainvestment.appkey,
                    koreainvestment.appsecret,
                    "P", // 고객타입, P : 개인
                    "1", // 거래타입, 1 : 등록
                    "utf-8"
                ),
                WsRequest.Body(WsRequest.Input(WsTransaction.EXECUTION, stock.code))
            )
            val message = JsonUtil.mapper.writeValueAsString(parameter)
            websocketClientEndpoint!!.sendMessage(message)
        }
        slackMessageService.sendMessage("웹소켓 오픈")
    }

    fun close() {
        log.info("웹소켓 종료")
        websocketClientEndpoint?.close()
        websocketClientEndpoint = null
    }
}
