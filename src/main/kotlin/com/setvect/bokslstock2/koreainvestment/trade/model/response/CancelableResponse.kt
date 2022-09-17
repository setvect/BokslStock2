package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty

data class CancelableResponse(
    /**주문채번지점번호*/
    @JsonProperty("ORD_GNO_BRNO") val ordGnoBrno: String,
    /**주문번호*/
    @JsonProperty("ODNO") val odno: String,
    /**원주문번호*/
    @JsonProperty("ORGN_ODNO") val orgnOdno: String,
    /**주문구분명*/
    @JsonProperty("ORD_DVSN_NAME") val ordDvsnName: String,
    /**상품번호 - 종목코드*/
    @JsonProperty("PDNO") val code: String,
    /**상품명*/
    @JsonProperty("PRDT_NAME") val prdtName: String,
    /**정정취소구분명*/
    @JsonProperty("RVSE_CNCL_DVSN_NAME") val rvseCnclDvsnName: String,
    /**주문수량*/
    @JsonProperty("ORD_QTY") val ordQty: String,
    /**주문단가*/
    @JsonProperty("ORD_UNPR") val ordUnpr: String,
    /**주문시각*/
    @JsonProperty("ORD_TMD") val ordTmd: String,
    /**총체결수량*/
    @JsonProperty("TOT_CCLD_QTY") val totCcldQty: String,
    /**총체결금액*/
    @JsonProperty("TOT_CCLD_AMT") val totCcldAmt: String,
    /**가능수량*/
    @JsonProperty("PSBL_QTY") val psblQty: String,
    /**매도매수구분코드*/
    @JsonProperty("SLL_BUY_DVSN_CD") val sllBuyDvsnCd: String,
    /**주문구분코드*/
    @JsonProperty("ORD_DVSN_CD") val ordDvsnCd: String,
    /**운용사지정주문번호*/
    @JsonProperty("MGCO_APTM_ODNO") val mgcoAptmOdno: String,
)