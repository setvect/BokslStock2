package com.setvect.bokslstock2.koreainvestment.ws.service

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("!test") // 테스트 때는 실행 안함
class WebsocketStockSchedule(
    var tradingWebsocket: TradingWebsocket
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 실시간 채결 가격 모니터링
     */
    @Scheduled(cron = "0 59 08 * * MON-FRI") // 월~금 매일 08시 59분에 실행
//    @Scheduled(cron = "0 0/5 09-16 * * MON-FRI") // 웹소켓이 이유 없이 닫히는 경우가 있음. 그래서 5분마다 실행
    fun openWebSocket() {
        tradingWebsocket.open()
        log.info("실시간 채결 가격 모니터링 시작")
    }

    /**
     * 실시간 체결 가격 종료
     */
    @Scheduled(cron = "0 35/10 15 * * MON-FRI")
    fun closeWebSocket() {
        tradingWebsocket.close()
        log.info("실시간 채결 가격 모니터링 종료")
    }
}