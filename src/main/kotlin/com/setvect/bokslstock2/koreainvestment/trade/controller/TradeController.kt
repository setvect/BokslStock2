package com.setvect.bokslstock2.koreainvestment.trade.controller

import com.setvect.bokslstock2.koreainvestment.ws.service.TradingWebsocket
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/trade")
class TradeController(
    val tradingWebsocket: TradingWebsocket
) {

    @GetMapping("/abc")
    fun hello(): String {
        return "hello"
    }

    @PostMapping("/open")
    fun openWebSocket(): Boolean {
        tradingWebsocket.open()
        return true
    }

    @PostMapping("close")
    fun closeWebSocket(): Boolean {
        tradingWebsocket.close()
        return true
    }
}