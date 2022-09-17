package com.setvect.bokslstock2.koreainvestment.trade.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.setvect.bokslstock2.koreainvestment.ws.model.StockTransaction

data class CurrentPriceRequest(
    val code: String,
) {
    @JsonIgnore
    val stockTransaction: StockTransaction = StockTransaction.CURRENT_PRICE
}
