package com.setvect.bokslstock2.analysis.model

import com.setvect.bokslstock2.analysis.entity.MabsTradeEntity

/**
 * 단위 거래 건별 내역
 */
data class TradeReportItem(
    /**
     * 거래 내용
     */
    val mabsTradeEntity: MabsTradeEntity,
    /**
     * 매매 수량
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
    val gains: Long
) {
    /**
     * @return 매수 금액
     */
    fun getBuyAmount(): Long {
        return (qty * mabsTradeEntity.unitPrice).toLong()
    }

    /**
     * @return 평가금
     */
    fun getEvaluationPrice(): Long {
        return getBuyAmount() + cash
    }

}