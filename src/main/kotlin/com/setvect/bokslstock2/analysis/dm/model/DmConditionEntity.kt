package com.setvect.bokslstock2.analysis.dm.model

import com.setvect.bokslstock2.common.entity.ConditionEntity
import com.setvect.bokslstock2.index.entity.StockEntity

class DmConditionEntity(
    override val stock: StockEntity,
    override val tradeList: List<DmTrade>
) : ConditionEntity {
    override fun getConditionId(): Long {
        return 0
    }
}
