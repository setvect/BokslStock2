package com.setvect.bokslstock2.koreainvestment.ws.service

import com.setvect.bokslstock2.koreainvestment.ws.model.WsResponse
import org.springframework.context.ApplicationEvent

class StockWebSocketEvent(val response: WsResponse) : ApplicationEvent(response) {
}