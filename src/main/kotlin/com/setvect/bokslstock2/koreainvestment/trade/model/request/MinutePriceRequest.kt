package com.setvect.bokslstock2.koreainvestment.trade.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.setvect.bokslstock2.koreainvestment.ws.model.StockTransaction
import java.time.LocalTime

/**
 * 1분봉 시세 시세
 */
data class MinutePriceRequest(
    // 종목 코드
    val code: String,
    // 분봉 조회 기준 시간(개장후 부터 time(포함)까지 조회)
    val time: LocalTime
) {
    @JsonIgnore
    val stockTransaction: StockTransaction = StockTransaction.DATE_PRICE
}
