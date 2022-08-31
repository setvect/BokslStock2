package com.setvect.bokslstock2.koreainvestment.ws

import com.fasterxml.jackson.annotation.JsonValue

enum class WsTransaction(
    @get:JsonValue
    val code: String
) {
    /**
     * 주식현재가 실시간주식체결가[실시간-003]
     */
    EXECUTION("H0STCNT0"),

    /**
     * 주식현재가 실시간주식호가[실시간-004]
     */
    QUOTATION("H0STASP0")
}