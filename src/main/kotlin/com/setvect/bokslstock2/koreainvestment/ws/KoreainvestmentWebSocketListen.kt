package com.setvect.bokslstock2.koreainvestment.ws

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import okhttp3.WebSocketListener
import org.springframework.stereotype.Component

@Component
class KoreainvestmentWebSocketListen {
    // TODO °ª ³Ö±â
    private val url: String = ""
    
    fun listen(listener: WebSocketListener?) {
        val client = OkHttpClient()
        val request: Request = Builder()
            .url(url)
            .build()
        client.newWebSocket(request, listener!!)
        client.dispatcher.executorService.shutdown()
    }
}