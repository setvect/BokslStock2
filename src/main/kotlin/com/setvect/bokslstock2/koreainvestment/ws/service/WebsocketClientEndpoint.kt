package com.setvect.bokslstock2.koreainvestment.ws.service

import com.setvect.bokslstock2.koreainvestment.ws.model.WsResponse
import com.setvect.bokslstock2.koreainvestment.ws.model.WsTransaction
import com.setvect.bokslstock2.slack.SlackMessageService
import com.setvect.bokslstock2.util.BeanUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import javax.websocket.*

@ClientEndpoint
class WebsocketClientEndpoint(
    endpointUri: String,
    private val publisher: ApplicationEventPublisher,
    private val slackMessageService: SlackMessageService

) {
    private var userSession: Session? = null
    private val log = LoggerFactory.getLogger(javaClass)

    init {
        try {
            val uri = URI(endpointUri)
            val container = ContainerProvider.getWebSocketContainer()
            container.connectToServer(this, uri)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @OnOpen
    fun onOpen(userSession: Session?) {
        println("웹소켓 열림")
        this.userSession = userSession
    }

    @OnClose
    fun onClose(userSession: Session, reason: CloseReason) {
        println("웹소켓 닫임. 이유: $reason")
        this.userSession = null
    }

    @OnMessage
    fun onMessage(message: String) {
        log.info("onMessage: $message")
        if (isStockData(message)) {
            val wsResponse = WsResponse.parsing(message)
            publisher.publishEvent(StockWebSocketEvent(wsResponse))
        } else {
            publisher.publishEvent(StockWebSocketEvent(WsResponse("aaa", "aaa", 100, "aaa")))
        }
    }

    @OnMessage
    fun onMessage(bytes: ByteBuffer?) {
        // 한국투자증권 웹소켓은 bytes 파라미터를 갖는 onMessage 이벤트는 일어나지 않음
        log.info(bytes.toString())
    }

    @OnError
    fun onError(session: Session, t: Throwable) {
        val message = "웹소켓 에러 : " + t.message
        log.error(message, t)
        slack(message)

        // 실패 했을때 다시 웹소켓 연결
        try {
            TimeUnit.SECONDS.sleep(5)
        } catch (e: InterruptedException) {
            log.error(e.message)
        }
        log.info("웹소켓 다시 시작 중")
        val tradingWebsocket = BeanUtils.getBean(TradingWebsocket::class.java)
        tradingWebsocket.open()
    }

    fun sendMessage(message: String) {
        userSession!!.asyncRemote.sendText(message)
    }

    @Throws(IOException::class)
    fun close() {
        userSession!!.close()
    }

    private fun isStockData(response: String): Boolean {
        return WsTransaction.values().any {
            response.contains("|${it.trId}|")
        }
    }

    private fun slack(message: String) {
        slackMessageService.sendMessage(message)
    }
}