package com.setvect.bokslstock2.koreainvestment.trade.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.setvect.bokslstock2.koreainvestment.ws.model.StockTransaction

data class QuoteRequest(
    val code: String,
) {
    @JsonIgnore
    val wsTransaction: StockTransaction = StockTransaction.QUOTE
}