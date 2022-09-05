package com.setvect.bokslstock2.koreainvestment.ws.service

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.ws.model.WsRequest
import com.setvect.bokslstock2.koreainvestment.ws.model.WsTransaction
import com.setvect.bokslstock2.slack.SlackMessageService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * 실시간으로 시세를 확인 하여 매매 진행
 */
@Component
class TradingWebsocket(
    private val socketListen: KoreainvestmentWebSocketListen,
    private val publisher: ApplicationEventPublisher,
    private val slackMessageService: SlackMessageService,
    private val bokslStockProperties: BokslStockProperties
) {
    fun onApplicationEvent() {
        val webSocketListener = StockWebSocketListener(publisher, slackMessageService)
        val koreainvestment = bokslStockProperties.koreainvestment

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
                webSocketListener.addParameter(parameter)
            }
        }
        socketListen.listen(webSocketListener)
    }
}
