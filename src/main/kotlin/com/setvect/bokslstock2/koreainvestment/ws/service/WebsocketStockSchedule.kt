package com.setvect.bokslstock2.koreainvestment.ws.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class WebsocketStockSchedule(
    var tradingWebsocket: TradingWebsocket
) {
    private val log = LoggerFactory.getLogger(javaClass)

    //    @Scheduled(cron = "0 45 08 * * MON-FRI") // 월~금 매일 08시 45분에 실행
//    @Scheduled(cron = "0/30 * * * * ?")
    fun openWebSocket() {
        tradingWebsocket.open()
    }

    //    @Scheduled(cron = "15/35 * * * * ?") // 월~금 매일 오전 8시 30분에 실행
    fun closeWebSocket() {
        tradingWebsocket.close()
    }
}