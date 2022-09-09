package com.setvect.bokslstock2.koreainvestment.ws.service

import com.setvect.bokslstock2.koreainvestment.ws.model.Quotation
import com.setvect.bokslstock2.koreainvestment.ws.model.RealtimeExecution
import com.setvect.bokslstock2.koreainvestment.ws.model.WsResponse
import com.setvect.bokslstock2.koreainvestment.ws.model.WsTransaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * 호가 이벤트
 */
@Component
class WebsocketRequestHandler : ApplicationListener<StockWebSocketEvent> {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: StockWebSocketEvent) {
        val response = event.response
        when (response.getTransaction()) {
            WsTransaction.EXECUTION -> execution(response)
            WsTransaction.QUOTATION -> quotation(response)
        }
    }

    private fun quotation(response: WsResponse) {
        val quotation = Quotation.parsing(response.responseData)
        log.info("${response.trId} = $quotation")
    }

    private fun execution(response: WsResponse) {
        val realtimeExecution = RealtimeExecution.parsing(response.responseData)
        log.info("${response.trId} = $realtimeExecution")
    }
}