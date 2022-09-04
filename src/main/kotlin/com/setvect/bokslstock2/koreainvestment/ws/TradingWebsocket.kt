package com.setvect.bokslstock2.koreainvestment.ws

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.ws.model.WsRequest
import com.setvect.bokslstock2.slack.SlackMessageService
import lombok.extern.slf4j.Slf4j
import org.springframework.context.ApplicationEventPublisher

/**
 * 실시간으로 시세를 확인 하여 매매 진행
 */
@Slf4j
class TradingWebsocket(
    private val socketListen: KoreainvestmentWebSocketListen,
    private val publisher: ApplicationEventPublisher,
    private val slackMessageService: SlackMessageService,
    private val bokslStockProperties: BokslStockProperties
) {
    fun onApplicationEvent() {
        val webSocketListener = StockWebSocketListener(publisher, slackMessageService)
        val koreainvestment = bokslStockProperties.koreainvestment
        val parameter = WsRequest(
            WsRequest.Header(
                koreainvestment.accessKey,
                koreainvestment.appsecret,
                "P", // 고객타입, P : 개인
                "1", // 거래타입, 1 : 등록
                "utf-8"
            ),
            WsRequest.Body(
                // TODO 고정값을 모니터링 값으로
                WsRequest.Input(WsTransaction.QUOTATION, "005930")
            )
        )
        webSocketListener.setParameter(parameter)

        socketListen.listen(webSocketListener)
    }
}