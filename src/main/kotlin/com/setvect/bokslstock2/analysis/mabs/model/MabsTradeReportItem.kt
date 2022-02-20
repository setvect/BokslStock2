package com.setvect.bokslstock2.analysis.mabs.model

import com.setvect.bokslstock2.analysis.common.model.CommonTradeReportItem
import com.setvect.bokslstock2.analysis.mabs.entity.MabsTradeEntity
import com.setvect.bokslstock2.common.entity.TradeEntity
import com.setvect.bokslstock2.common.entity.TradeReportItem

/**
 * 단위 거래 건별 내역
 */
data class MabsTradeReportItem(
    /**
     * 거래 내용
     */
    val mabsTradeEntity: MabsTradeEntity,
    /**
     * 공통 거래 내역
     */
    override val common: CommonTradeReportItem,
) : TradeReportItem {
    override val tradeEntity: TradeEntity
        get() = mabsTradeEntity

    /**
     * @return 매수 금액
     */
    override fun getBuyAmount(): Double {
        return common.qty * mabsTradeEntity.unitPrice
    }
}