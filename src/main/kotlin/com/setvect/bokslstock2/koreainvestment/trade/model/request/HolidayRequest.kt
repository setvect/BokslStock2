package com.setvect.bokslstock2.koreainvestment.trade.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.setvect.bokslstock2.koreainvestment.ws.model.StockTransaction
import java.time.LocalDate

data class HolidayRequest(
    val baseDate: LocalDate,
) {
    @JsonIgnore
    val stockTransaction: StockTransaction = StockTransaction.HOLIDAY
}
