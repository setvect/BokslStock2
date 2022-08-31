package com.setvect.bokslstock2.koreainvestment.ws

import com.setvect.bokslstock2.koreainvestment.ws.model.WsRequest
import com.setvect.bokslstock2.slack.SlackMessageService
import com.setvect.bokslstock2.util.BeanUtils.getBean
import com.setvect.bokslstock2.util.GsonUtil
import lombok.extern.slf4j.Slf4j
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

@Slf4j
class StockWebSocketListener(private val publisher: ApplicationEventPublisher,
                             private val slackMessageService: SlackMessageService?) : WebSocketListener() {

    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun setParameter(parameter: WsRequest) {
        request = GsonUtil.GSON.toJson(parameter)
    }

    private var request: String? = null
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        val message = String.format("Socket Closed : %s / %s", code, reason)
        log.info(message)
        slack(message)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        val message = String.format("Socket Closing : %s / %s\n", code, reason)
        log.info(message)
        slack(message)
        webSocket.close(NORMAL_CLOSURE_STATUS, null)
        webSocket.cancel()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable,
                           response: Response?) {
        val message = "Socket Error : " + t.message
        log.error(message, t)
        slack(message)
        try {
            TimeUnit.SECONDS.sleep(5)
        } catch (e: InterruptedException) {
            log.error(e.message)
        }
        log.info("restarting")
        val tradingWebsocket = getBean(TradingWebsocket::class.java)
        tradingWebsocket.onApplicationEvent()
        log.info("restart completed")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        log.info(text)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        val tradeResult = GsonUtil.GSON.fromJson(bytes.string(StandardCharsets.UTF_8), TradeResult::class.java)
        publisher.publishEvent(ChangeTrade(tradeResult))
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocket.send(request!!)
    }

    private fun slack(message: String) {
        if (slackMessageService == null) {
            return
        }
        slackMessageService.sendMessage(message)
    }

    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }
}