package com.setvect.bokslstock2.analysis.common.model

/**
 * 단위 거래 건별 내역
 */
data class Trade(
    val preTrade: PreTrade,
    /**
     * 매수 수량
     */
    val qty: Int,
    /**
     * 해당 거래 후 현금
     */
    val cash: Double,
    /**
     * 매매 수수료
     */
    val feePrice: Double,
    /**
     * 투자 수익 금액
     */
    val gains: Double,
    /**
     * 현재시점 주식평가금
     */
    val stockEvalPrice: Double
) {
    /**
     * @return 평가금
     */
    fun getEvalPrice(): Double {
        return stockEvalPrice + cash
    }

    /**
     * @return 매수 금액
     */
    fun getBuyAmount(): Double {
        return qty * preTrade.unitPrice
    }
}