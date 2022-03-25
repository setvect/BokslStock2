package com.setvect.bokslstock2.analysis.dm.model

import com.setvect.bokslstock2.analysis.common.model.CommonTradeReportItem
import com.setvect.bokslstock2.analysis.common.model.PreTrade

@Deprecated("안씀", replaceWith = ReplaceWith("폐기"))
data class DmTradeReportItem(
    /**
     * 거래 내용
     */
    val dmTrade: PreTrade,

    /**
     * 공통 거래 내역
     */
    val common: CommonTradeReportItem,
)