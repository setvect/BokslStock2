package com.setvect.bokslstock2.koreainvestment.trade.model.web

import java.time.LocalDateTime

abstract class DefaultSearchFrom(
    /**
     * 시작 날짜
     */
    open val from: LocalDateTime? = null,

    /**
     * 종료 날짜
     */
    open val to: LocalDateTime? = null,

    /**
     * 계좌 번호
     */
    open val account: String? = null,
)
