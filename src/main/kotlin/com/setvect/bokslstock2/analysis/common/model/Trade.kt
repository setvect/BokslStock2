package com.setvect.bokslstock2.analysis.common.model

/**
 * 거래 건별 내역
 */
@Deprecated("삭제 예정")
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
     * TODO 필요 없을것 같은데. 이유: 종목코드가 있고 수량이 있으면 데이터를 조회하여 구할 수 있음
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