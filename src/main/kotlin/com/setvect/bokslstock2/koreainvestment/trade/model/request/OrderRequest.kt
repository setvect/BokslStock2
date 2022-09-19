package com.setvect.bokslstock2.koreainvestment.trade.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.setvect.bokslstock2.koreainvestment.ws.model.StockTransaction

/**
 * 매수/매도 주문
 */
data class OrderRequest(
    /**계좌번호*/
    @JsonProperty("CANO") val cano: String,
    /**종목코드*/
    @JsonProperty("PDNO") val code: String,

    /**가격*/
    @JsonProperty("ORD_UNPR")
    @JsonSerialize(using = ToStringSerializer::class)
    val ordunpr: Int,

    /**수량*/
    @JsonSerialize(using = ToStringSerializer::class)
    @JsonProperty("ORD_QTY")
    val ordqty: Int,

    /**주문 구분: 지정가(00)*/
    @JsonProperty("ORD_DVSN") val orddvsn: String = "00",
    /**계좌상품코드: 계좌상품코드(01)*/
    @JsonProperty("ACNT_PRDT_CD") val acntprdtcd: String = "01"
) {
    @JsonIgnore
    val buy: StockTransaction = StockTransaction.BUY_ORDER

    @JsonIgnore
    val sell: StockTransaction = StockTransaction.SELL_ORDER

}