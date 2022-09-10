package com.setvect.bokslstock2.koreainvestment.ws.service

import com.setvect.bokslstock2.koreainvestment.ws.model.WsResponse
import com.setvect.bokslstock2.koreainvestment.ws.model.WsTransaction
import com.setvect.bokslstock2.slack.SlackMessageService
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import javax.websocket.*

@ClientEndpoint
class WebsocketClientEndpoint(
    endpointUri: String,
    private val publisher: ApplicationEventPublisher,
    private val slackMessageService: SlackMessageService?

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
        println("onOpen websocket")
        this.userSession = userSession
    }

    @OnClose
    fun onClose(userSession: Session, reason: CloseReason) {
        println("onClose websocket. reason: $reason")
        this.userSession = null
    }

    @OnMessage
    fun onMessage(message: String) {
        log.info("onMessage: $message")
        if (isStockData(message)) {
            val wsResponse = WsResponse.parsing(message)
            publisher.publishEvent(StockWebSocketEvent(wsResponse))
        } else {
            publisher.publishEvent(message)
            log.info(message)
        }

    }

    @OnMessage
    fun onMessage(bytes: ByteBuffer?) {
        // 한국투자증권 웹소켓은 bytes 파라미터를 갖는 onMessage 이벤트는 일어나지 않음
        log.info(bytes.toString())
    }

    @OnError
    fun onError(session: Session, t: Throwable) {
        val message = "Socket Error : " + t.message
        log.error(message, t)
        slack(message)

        // TODO 실패 했을때 다시 웹소켓 연결
//        try {
//            TimeUnit.SECONDS.sleep(5)
//        } catch (e: InterruptedException) {
//            log.error(e.message)
//        }
//        log.info("웹소켓 다시 시작 중")
//        val tradingWebsocket = getBean(TradingWebsocket::class.java)
//        tradingWebsocket.open()

    }

    fun sendMessage(message: String) {
        log.info("send message: $message")
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
        if (slackMessageService == null) {
            return
        }
        slackMessageService.sendMessage(message)
    }

}