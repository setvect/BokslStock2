package com.setvect.bokslstock2.analysis.common.model

import com.setvect.bokslstock2.analysis.common.entity.TradeEntity
import com.setvect.bokslstock2.index.entity.StockEntity

interface CommonCondition {
    /**
     * @return 조건명
     */
    val name : String
    /**
     * @return 종목
     */
    val stock: StockEntity

    val tradeList: List<TradeEntity>
}