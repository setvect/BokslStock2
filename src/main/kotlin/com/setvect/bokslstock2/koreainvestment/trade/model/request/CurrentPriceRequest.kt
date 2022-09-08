package com.setvect.bokslstock2.koreainvestment.trade.model.request

import com.setvect.bokslstock2.koreainvestment.ws.model.WsTransaction

data class CurrentPriceRequest(
    val code: String,
) {
    val wsTransaction: WsTransaction = WsTransaction.CURRENT_PRICE
}
