package com.setvect.bokslstock2.koreainvestment.trade.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.setvect.bokslstock2.koreainvestment.ws.model.WsTransaction

data class CurrentPriceRequest(
    val code: String,
) {
    @JsonIgnore
    val wsTransaction: WsTransaction = WsTransaction.CURRENT_PRICE
}