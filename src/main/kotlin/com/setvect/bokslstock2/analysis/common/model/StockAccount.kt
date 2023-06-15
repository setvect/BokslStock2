package com.setvect.bokslstock2.analysis.common.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class StockAccount(
    var qty: Int,
    var totalBuyPrice: Double,
) {
    /** 매수 평단가 */
    fun getAveragePrice(): Double {
        return totalBuyPrice / qty
    }

    /** 매수 */
    fun buy(price: Double, qty: Int) {
        this.qty += qty
        this.totalBuyPrice += price * qty
    }

    /** 매도 */
    fun sell(qty: Int) {
        // 매도 후 수량이 마이너스면 예외 발생
        if (this.qty - qty < 0) {
            throw IllegalArgumentException("매도 수량이 현재 수량보다 많음. 현재 수량:${this.qty}, 매도 수량:$qty")
        }
        this.totalBuyPrice -= getAveragePrice() * qty
        this.qty -= qty
    }
}