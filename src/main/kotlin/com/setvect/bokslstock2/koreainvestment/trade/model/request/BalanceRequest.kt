package com.setvect.bokslstock2.koreainvestment.trade.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.setvect.bokslstock2.koreainvestment.ws.model.WsTransaction

/**
 * 잔고 조회
 */
data class BalanceRequest(
    val accountNo: String,
) {
    @JsonIgnore
    val wsTransaction: WsTransaction = WsTransaction.BALANCE
}
