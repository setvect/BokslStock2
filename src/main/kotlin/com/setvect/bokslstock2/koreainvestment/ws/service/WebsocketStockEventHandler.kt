package com.setvect.bokslstock2.koreainvestment.ws.service

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * 실시간 체결 웹소캣 오픈
 */
@Component
class WebsocketStockEventHandler(
    var tradingWebsocket: TradingWebsocket
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 장 중간에 프로그램이 시작될경우 스케줄로 인한 웹소켓 요청이 가지 않는다.
     * 그래서 프로그램 시작 시 체결 웹 소켓을 오픈함
     * @see WebsocketStockSchedule
     */
    @EventListener
    fun onApplicationEvent(event: StockWebSocketEvent) {
        if (!TradeTimeHelper.isTimeToTrade()) {
            log.info("매매 가능 시간이 아닙니다.")
            return
        }
        tradingWebsocket.open()
    }
}