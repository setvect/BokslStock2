package com.setvect.bokslstock2.common.entity

import com.setvect.bokslstock2.index.entity.StockEntity

interface ConditionEntity {
    /**
     * @return 조건 아이디
     */
    fun getConditionId(): Long

    /**
     * @return 종목
     */
    val stock: StockEntity

    val tradeList: List<TradeEntity>
}