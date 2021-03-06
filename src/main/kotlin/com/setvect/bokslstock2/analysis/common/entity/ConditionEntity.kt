package com.setvect.bokslstock2.analysis.common.entity

import com.setvect.bokslstock2.index.entity.StockEntity

interface ConditionEntity {
    val conditionSeq: Long

    /**
     * @return 종목
     */
    val stock: StockEntity

    val tradeList: List<TradeEntity>
}