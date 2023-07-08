package com.setvect.bokslstock2.koreainvestment.trade.model.web

import java.time.LocalDateTime

data class  TradeSearchForm(
    override val from: LocalDateTime? = null,

    override val to: LocalDateTime? = null,

    override val account: String? = null,
    /**
     * 종목코드
     */
    var code: String? = null,
) : DefaultSearchFrom(from, to, account)