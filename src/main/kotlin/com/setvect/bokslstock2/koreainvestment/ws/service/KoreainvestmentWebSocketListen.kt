package com.setvect.bokslstock2.koreainvestment.ws.service

import com.setvect.bokslstock2.config.BokslStockProperties
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KoreainvestmentWebSocketListen(
    private val bokslStockProperties: BokslStockProperties
) {
    private var webSocket: WebSocket? = null
    private val log = LoggerFactory.getLogger(javaClass)

    fun connect(listener: WebSocketListener) {
        val koreainvestment = bokslStockProperties.koreainvestment
        val client = OkHttpClient()
        val request: Request = Builder()
            .url(koreainvestment.ws.url)
            .build()

        if (isOpen()) {
            log.info("이미 실행된 웹소켓")
        } else {
            webSocket = client.newWebSocket(request, listener)
        }

        // 안써도 될 것 겉음
        //  client.dispatcher.executorService.shutdown()
    }

    fun disconnect() {
        if (isOpen()) {
            val close = webSocket!!.close(1000, null)
            log.info("웹소켓 닫음. 결과: $close")
        } else {
            log.info("오픈되어 있지 않음")
        }

        // 안써도 될 것 겉음
        //  client.dispatcher.executorService.shutdown()
    }

    fun isOpen(): Boolean {
        // TODO 오픈 여부를 체크하는 더 좋은 방법은 없을까?
        return webSocket != null
    }
}