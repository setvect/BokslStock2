package com.setvect.bokslstock2.koreainvestment.ws

import com.setvect.bokslstock2.slack.SlackMessageService
import lombok.extern.slf4j.Slf4j
import org.springframework.context.ApplicationEventPublisher

/**
 * 실시간으로 시세를 확인 하여 매매 진행
 */
@Slf4j
class TradingWebsocket(
    private val socketListen: KoreainvestmentWebSocketListen,
    private val publisher: ApplicationEventPublisher,
    private val slackMessageService: SlackMessageService
) {
    fun onApplicationEvent() {
        val webSocketListener = StockWebSocketListener(publisher, slackMessageService)
        socketListen.listen(webSocketListener)
    }
}