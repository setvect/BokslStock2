package com.setvect.bokslstock2.koreainvestment.vbs.service

import com.setvect.bokslstock2.koreainvestment.ws.model.WsTransaction
import com.setvect.bokslstock2.koreainvestment.ws.service.StockWebSocketEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * 호가, 매수 채결가 이벤트
 */
@Component
class VbsEventHandler(
    val vbsService: VbsService
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun onApplicationEvent(event: ApplicationStartedEvent) {
        log.info("복슬매매2 실행")
        // TODO 테스트 실행시 아래 로직 실행 안되게 하기
        vbsService.start()
    }

    @EventListener
    fun onApplicationEvent(event: StockWebSocketEvent) {
        val response = event.response
        when (response.getTransaction()) {
            WsTransaction.EXECUTION -> vbsService.execution(response)
            else -> {
                // nothing
            }
        }
    }
}