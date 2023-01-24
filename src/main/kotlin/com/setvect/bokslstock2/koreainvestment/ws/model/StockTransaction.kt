package com.setvect.bokslstock2.koreainvestment.ws.model

import com.fasterxml.jackson.annotation.JsonValue

enum class StockTransaction(
    @get:JsonValue
    val trId: String
) {
    /** 주식 현제가 */
    CURRENT_PRICE("FHKST01010100"),

    /** 일자별 가격 */
    DATE_PRICE("FHKST01010400"),

    /** 분봉 가격 */
    MINUTE_PRICE("FHKST03010200"),

    /** 잔고 조회 */
    BALANCE("TTTC8434R"),

    /** 호가 */
    QUOTE("FHKST01010200"),

    /** 휴장일 조회 */
    HOLIDAY("CTCA0903R"),

    /** 취소가능 주문 조회 */
    CANCELABLE("TTTC8036R"),

    /** 매수주문 */
    BUY_ORDER("TTTC0802U"),

    /** 매도주문 */
    SELL_ORDER("TTTC0801U"),
    ;

    companion object {
        fun parsingTrId(trId: String): StockTransaction {
            return values().find { it.trId == trId } ?: throw RuntimeException("없는 TrId: $trId")
        }
    }
}