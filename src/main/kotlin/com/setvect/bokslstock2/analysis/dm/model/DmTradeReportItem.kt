package com.setvect.bokslstock2.analysis.dm.model

import com.setvect.bokslstock2.analysis.common.model.CommonTradeReportItem
import com.setvect.bokslstock2.common.entity.TradeEntity
import com.setvect.bokslstock2.common.entity.TradeReportItem

data class DmTradeReportItem(
    /**
     * 거래 내용
     */
    val dmTrade: DmTrade,

    /**
     * 공통 거래 내역
     */
    override val common: CommonTradeReportItem,
) : TradeReportItem {
    override val tradeEntity: TradeEntity
        get() = dmTrade

    /**
     * @return 매수 금액
     */
    override fun getBuyAmount(): Double {
        return common.qty * dmTrade.unitPrice
    }
}
