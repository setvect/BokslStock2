package com.setvect.bokslstock2.koreainvestment.ws

import com.setvect.bokslstock2.koreainvestment.ws.model.WsRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder

fun main() {
    val client = OkHttpClient()
    val request: Request = Builder().url("ws://ops.koreainvestment.com:21000/tryitout/H0STASP0").build()

    val webSocketListener = StockWebSocketListener({ println(it) }, null)
    val parameter = WsRequest(
        WsRequest.Header(
            "PShCnGGVZ40Cn4GAH6vXWQfMBxs7SRfsqD7y",
            "brriSfXncMy928UAYae4surPgskI3veP+ZsdPlAMBQCohKWF8H+N5ZmEJB44bdMPNhzIfrgV7UnVTOcuxRA42aXhvqSDpEyFv1TTg+9vAmq9h/nLgIk0v5OfcyogB9In4mR0OVDrMU1/9+wpXNfMchJ/UcVl9CbzCRkdroe/+w65gciCxE8=",
            "P",
            "1",
            "utf-8"
        ),
        WsRequest.Body(
            WsRequest.Input(WsTransaction.QUOTATION, "005930")
        )
    )

    webSocketListener.setParameter(parameter)
    client.newWebSocket(request, webSocketListener)
    client.dispatcher.executorService.shutdown()
}

