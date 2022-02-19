package com.setvect.bokslstock2.analysis.common.model

/**
 * 단위 거래 건별 내역
 */
data class CommonTradeReportItem(
    /**
     * 매수 수량
     */
    val qty: Int,
    /**
     * 해당 거래 후 현금
     */
    val cash: Long,
    /**
     * 매매 수수료
     */
    val feePrice: Int,
    /**
     * 투자 수익 금액
     */
    val gains: Long,
    /**
     * 현재시점 주식평가금
     */
    val stockEvalPrice: Long
) {
    /**
     * @return 평가금
     */
    fun getEvalPrice(): Long {
        return stockEvalPrice + cash
    }
}