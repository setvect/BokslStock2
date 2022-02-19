package com.setvect.bokslstock2.analysis.vbs.model

import com.setvect.bokslstock2.analysis.common.model.CommonTradeReportItem
import com.setvect.bokslstock2.analysis.vbs.entity.VbsTradeEntity
import com.setvect.bokslstock2.common.entity.TradeEntity
import com.setvect.bokslstock2.common.entity.TradeReportItem

/**
 * 단위 거래 건별 내역
 */
data class VbsTradeReportItem(
    /**
     * 거래 내용
     */
    val vbsTradeEntity: VbsTradeEntity,

    /**
     * 공통 거래 내역
     */
    override val common: CommonTradeReportItem,
) : TradeReportItem {
    override val tradeEntity: TradeEntity
        get() = vbsTradeEntity

    /**
     * @return 매수 금액
     */
    override fun getBuyAmount(): Long {
        return common.qty * vbsTradeEntity.unitPrice.toLong()
    }
}