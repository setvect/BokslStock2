package com.setvect.bokslstock2.koreainvestment.trade.service

import com.setvect.bokslstock2.koreainvestment.ws.service.TradingWebsocket
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class StockSchedule(
    var tradingWebsocket: TradingWebsocket
) {
    private val log = LoggerFactory.getLogger(javaClass)

    //    @Scheduled(cron = "0 30 08 * * MON-FRI") // 월~금 매일 오전 8시 30분에 실행
    @Scheduled(cron = "0/10 * * * * ?") // 월~금 매일 오전 8시 30분에 실행
    fun scheduleVbs() {
        log.info("AAAAAAAAAAAAAAAAAAAA")
        tradingWebsocket.onApplicationEvent()
    }
}