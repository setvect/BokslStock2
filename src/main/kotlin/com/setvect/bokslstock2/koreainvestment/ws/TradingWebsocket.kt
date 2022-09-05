package com.setvect.bokslstock2.koreainvestment.ws

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.ws.model.WsRequest
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
                        koreainvestment.accessKey,
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


// INFO  [22-09-05 09:04:18] [main] StockWebSocketListener.addParameter(StockWebSocketListener.kt:27) {"header":{"appkey":"PSlmLW134xAK4APGritDHO0R154Ol2kv5NCg","appsecret":"TfKYNfGYN/lIQo8SuYm3XWMDmm8TKMiyOz1+Vao4nSCf6zrJAa+DJbZKDoGERL2CYI4wKsnru8sYEQY/Xg+9oJn79l74h7lX1GohkUDihRTUYAPmzSOlRWQbDTd+/SeGM0m276npPWf1z9W39NGP+/TgtCtvLsauJRTufdsApKzqTq90Os0=","custtype":"P","tr_type":"1","content-type":"utf-8"},"body":{"input":{"tr_id":"H0STCNT0","tr_key":"005930"}}}
// INFO     22-09-05 09:04:52[main] [c.s.b.k.w.StockWebSocketListener:27] -                           {"header":{"appkey":"PSlmLW134xAK4APGritDHO0R154Ol2kv5NCg","appsecret":"TfKYNfGYN/lIQo8SuYm3XWMDmm8TKMiyOz1+Vao4nSCf6zrJAa+DJbZKDoGERL2CYI4wKsnru8sYEQY/Xg+9oJn79l74h7lX1GohkUDihRTUYAPmzSOlRWQbDTd+/SeGM0m276npPWf1z9W39NGP+/TgtCtvLsauJRTufdsApKzqTq90Os0=","custtype":"P","tr_type":"1","content-type":"utf-8"},"body":{"input":{"tr_id":"H0STCNT0","tr_key":"69500.0"}}}

//
//        val parameter = WsRequest(
//            WsRequest.Header(
//                koreainvestment.accessKey,
//                koreainvestment.appsecret,
//                "P", // 고객타입, P : 개인
//                "1", // 거래타입, 1 : 등록
//                "utf-8"
//            ),
//            WsRequest.Body(WsRequest.Input(WsTransaction.QUOTATION, StockCode.SAMSUNG_005930.code))
//        )
//        webSocketListener.addParameter(parameter)
//        socketListen.listen(webSocketListener)

    }
}
