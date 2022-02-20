package com.setvect.bokslstock2.analysis.rb.model

import com.setvect.bokslstock2.analysis.common.model.CommonTradeReportItem
import com.setvect.bokslstock2.analysis.rb.entity.RbTradeEntity
import com.setvect.bokslstock2.common.entity.TradeEntity
import com.setvect.bokslstock2.common.entity.TradeReportItem

/**
 * 단위 거래 건별 내역
 */
data class RbTradeReportItem(
    /**
     * 거래 내용
     */
    val rbTradeEntity: RbTradeEntity,
    /**
     * 공통 거래 내역
     */
    override val common: CommonTradeReportItem,
) : TradeReportItem {
    override val tradeEntity: TradeEntity
        get() = rbTradeEntity

    /**
     * @return 매수 금액
     */
    override fun getBuyAmount(): Double {
        return common.qty * rbTradeEntity.unitPrice
    }
}