package com.setvect.bokslstock2.common.entity

import com.setvect.bokslstock2.analysis.common.model.CommonTradeReportItem

/**
 * 단위 거래 건별 내역
 */
@Deprecated("안씀", replaceWith = ReplaceWith("Trade 사용"))
interface TradeReportItem {
    /**
     * 거래 내용
     */
    val tradeEntity: TradeEntity

    /**
     * 공통 거래 내역
     */
    val common: CommonTradeReportItem

    /**
     * @return 매수 금액
     */
    fun getBuyAmount(): Double
}