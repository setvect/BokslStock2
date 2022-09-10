package com.setvect.bokslstock2.koreainvestment.trade.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.setvect.bokslstock2.koreainvestment.ws.model.StockTransaction
import com.setvect.bokslstock2.koreainvestment.ws.model.WsTransaction

/**
 * 일짜별 시세
 */
data class DatePriceRequest(
    val code: String,
    val dateType: DateType,
) {
    @JsonIgnore
    val wsTransaction: StockTransaction = StockTransaction.DATE_PRICE

    enum class DateType(val value: String) {
        DAY("D"), WEEK("W"), MONTH("M");
    }
}
