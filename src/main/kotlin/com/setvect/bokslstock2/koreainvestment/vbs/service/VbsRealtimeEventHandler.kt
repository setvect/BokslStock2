package com.setvect.bokslstock2.koreainvestment.vbs.service

import com.setvect.bokslstock2.koreainvestment.ws.model.WsTransaction
import com.setvect.bokslstock2.koreainvestment.ws.service.StockWebSocketEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener
import org.springframework.stereotype.Component

/**
 * 호가, 매수 채결가 이벤트
 */
@Component
class VbsRealtimeEventHandler(
    private val vbsService: VbsService
) : ApplicationListener<StockWebSocketEvent> {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    override fun onApplicationEvent(event: StockWebSocketEvent) {
        val response = event.response
        when (response.getTransaction()) {
            WsTransaction.EXECUTION -> vbsService.execution(response)
            WsTransaction.QUOTATION -> vbsService.quotation(response)
        }
    }

}