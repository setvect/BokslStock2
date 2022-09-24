package com.setvect.bokslstock2.koreainvestment.trade.controller

import com.setvect.bokslstock2.koreainvestment.trade.repository.TradeRepository
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/trade")
class TradeHistoryController(
    val tradeRepository: TradeRepository
) {

}