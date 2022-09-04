package com.setvect.bokslstock2.koreainvestment.ws

import com.setvect.bokslstock2.config.BokslStockProperties
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.WebSocketListener
import org.springframework.stereotype.Component

@Component
class KoreainvestmentWebSocketListen(
    private val bokslStockProperties: BokslStockProperties
) {

    fun listen(listener: WebSocketListener?) {
        val koreainvestment = bokslStockProperties.koreainvestment
        val client = OkHttpClient()
        val request: Request = Builder()
            .url(koreainvestment.ws.url)
            .build()

        client.dispatcher.executorService.shutdown()


        client.newWebSocket(request, listener!!)
        client.dispatcher.executorService.shutdown()
    }
}