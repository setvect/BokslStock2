package com.setvect.bokslstock2.koreainvestment.ws.service

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class WebsocketStockSchedule(
    var tradingWebsocket: TradingWebsocket
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 실시간 채결 가격 모니터링
     */
    @Scheduled(cron = "0 45 08 * * MON-FRI") // 월~금 매일 08시 45분에 실행
    fun openWebSocket() {
        tradingWebsocket.open()
        log.info("실시간 채결 가격 모니터링 시작")
    }

    /**
     * 실시간 체결 가격 종료
     */
    @Scheduled(cron = "0 35 15 * * MON-FRI")
    fun closeWebSocket() {
        tradingWebsocket.close()
        log.info("실시간 채결 가격 모니터링 종료")
    }
}