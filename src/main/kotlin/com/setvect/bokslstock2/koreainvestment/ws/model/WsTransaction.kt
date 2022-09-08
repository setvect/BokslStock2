package com.setvect.bokslstock2.koreainvestment.ws.model

import com.fasterxml.jackson.annotation.JsonValue

enum class WsTransaction(
    @get:JsonValue
    val trId: String
) {
    /** 주식현재가 실시간주식체결가[실시간-003] */
    EXECUTION("H0STCNT0"),

    /** 주식현재가 실시간주식호가[실시간-004]*/
    QUOTATION("H0STASP0"),

    /** 주식 현제가 */
    CURRENT_PRICE("FHKST01010100"),

    /** 일자별 가격 */
    DAILY_PRICE("FHKST01010400"),

    /** 잔고 조회 */
    BALANCE("TTTC8434R"),

    /** 매수주문 */
    BUY_ORDER("TTTC0802U"),

    /** 매도주문 */
    SELL_ORDER("TTTC0801U"),


    ;

    companion object {
        fun parsingTrId(trId: String): WsTransaction {
            return values().find { it.trId == trId } ?: throw RuntimeException("없는 TrId: $trId")
        }
    }
}