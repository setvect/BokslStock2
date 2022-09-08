package com.setvect.bokslstock2.koreainvestment.trade.model.request

import com.setvect.bokslstock2.koreainvestment.ws.model.WsTransaction

/**
 * 일짜별 시세
 */
data class DatePriceRequest(
    val code: String,
    val dateType: DateType,
) {
    val wsTransaction: WsTransaction = WsTransaction.DATE_PRICE

    enum class DateType(val value: String) {
        DAY("D"), WEEK("W"), MONTH("M");
    }
}
