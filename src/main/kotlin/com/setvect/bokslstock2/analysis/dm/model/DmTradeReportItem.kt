package com.setvect.bokslstock2.analysis.dm.model

import com.setvect.bokslstock2.analysis.common.model.CommonTradeReportItem
import com.setvect.bokslstock2.analysis.common.model.Trade

@Deprecated("안씀")
data class DmTradeReportItem(
    /**
     * 거래 내용
     */
    val dmTrade: Trade,

    /**
     * 공통 거래 내역
     */
    val common: CommonTradeReportItem,
)