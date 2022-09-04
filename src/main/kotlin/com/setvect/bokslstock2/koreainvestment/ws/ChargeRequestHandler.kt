package com.setvect.bokslstock2.koreainvestment.ws

import com.setvect.bokslstock2.koreainvestment.ws.model.Quotation
import com.setvect.bokslstock2.koreainvestment.ws.model.RealtimeExecution
import com.setvect.bokslstock2.koreainvestment.ws.model.WsResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * 호가 이벤트
 */
@Component
class ChargeRequestHandler : ApplicationListener<StockWebSocketEvent> {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: StockWebSocketEvent) {
        val response = event.response
        when (response.getTransaction()) {
            WsTransaction.EXECUTION -> execution(response)
            WsTransaction.QUOTATION -> quotation(response)
        }
    }

    private fun quotation(response: WsResponse) {
        val quotation = Quotation.parsing(response.toString())
        log.info(quotation.toString())
    }

    private fun execution(response: WsResponse) {
        val realtimeExecution = RealtimeExecution.parsing(response.toString())
        log.info(realtimeExecution.toString())
    }

    private fun parsing(response: String) {
    }
}