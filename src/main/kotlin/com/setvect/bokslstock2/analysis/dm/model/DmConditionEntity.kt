package com.setvect.bokslstock2.analysis.dm.model

import com.setvect.bokslstock2.analysis.common.model.PreTrade
import com.setvect.bokslstock2.index.entity.StockEntity

@Deprecated("안씀", replaceWith = ReplaceWith("폐기"))
class DmConditionEntity(
    val stock: StockEntity,
    val tradeList: List<PreTrade>
)