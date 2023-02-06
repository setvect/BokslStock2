package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 주식정정 취소 가능 주문
 */
data class CancelableResponse(
    /**주문채번지점번호*/
    @JsonProperty("ord_gno_brno") val ordGnoBrno: String,
    /**주문번호*/
    @JsonProperty("odno") val odno: String,
    /**원주문번호*/
    @JsonProperty("orgn_odno") val orgnOdno: String,
    /**주문구분명*/
    @JsonProperty("ord_dvsn_name") val ordDvsnName: String,
    /**상품번호 - 종목코드*/
    @JsonProperty("pdno") val code: String,
    /**상품명*/
    @JsonProperty("prdt_name") val prdtName: String,
    /**정정취소구분명*/
    @JsonProperty("rvse_cncl_dvsn_name") val rvseCnclDvsnName: String,
    /**주문수량*/
    @JsonProperty("ord_qty") val ordQty: String,
    /**주문단가*/
    @JsonProperty("ord_unpr") val ordUnpr: String,
    /**주문시각*/
    @JsonProperty("ord_tmd") val ordTmd: String,
    /**총체결수량*/
    @JsonProperty("tot_ccld_qty") val totCcldQty: String,
    /**총체결금액*/
    @JsonProperty("tot_ccld_amt") val totCcldAmt: String,
    /**가능수량*/
    @JsonProperty("psbl_qty") val psblQty: String,
    /**매도매수구분코드 01 : 매도, 02 : 매수*/
    @JsonProperty("sll_buy_dvsn_cd") val sllBuyDvsnCd: String,
    /**주문구분코드*/
    @JsonProperty("ord_dvsn_cd") val ordDvsnCd: String,
    /**운용사지정주문번호*/
    @JsonProperty("mgco_aptm_odno") val mgcoAptmOdno: String,
)