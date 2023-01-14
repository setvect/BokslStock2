package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty

data class MinutePriceResponse(

    @JsonProperty("output1") val output1: StockPrice?,
    @JsonProperty("output2") val output2: List<MinutePrice>?,
    @JsonProperty("rt_cd") val rtCd: String,
    @JsonProperty("msg_cd") val msgCd: String,
    @JsonProperty("msg1") val msg1: String
) {

    data class StockPrice(

        /**전일 대비*/
        @JsonProperty("prdy_vrss") val prdyVrss: String,
        /**전일 대비 부호*/
        @JsonProperty("prdy_vrss_sign") val prdyVrssSign: String,
        /**전일 대비율*/
        @JsonProperty("prdy_ctrt") val prdyCtrt: String,
        /**주식 전일 종가*/
        @JsonProperty("stck_prdy_clpr") val stckPrdyClpr: String,
        /**누적 거래량*/
        @JsonProperty("acml_vol") val acmlVol: String,
        /**누적 거래 대금*/
        @JsonProperty("acml_tr_pbmn") val acmlTrPbmn: String,
        /**HTS 한글 종목명*/
        @JsonProperty("hts_kor_isnm") val htsKorIsnm: String,
        /**주식 현재가*/
        @JsonProperty("stck_prpr") val stckPrpr: String,
    )

    /**
     * 조회결과 상세
     */
    data class MinutePrice(
        /**주식 영업 일자*/
        @JsonProperty("stck_bsop_date") val stckBsopDate: String,
        /**주식 체결 시간*/
        @JsonProperty("stck_cntg_hour") val stckCntgHour: String,
        /**누적 거래 대금*/
        @JsonProperty("acml_tr_pbmn") val acmlTrPbmn: String,
        /**주식 현재가*/
        @JsonProperty("stck_prpr") val stckPrpr: String,
        /**주식 시가2*/
        @JsonProperty("stck_oprc") val stckOprc: String,
        /**주식 최고가*/
        @JsonProperty("stck_hgpr") val stckHgpr: String,
        /**주식 최저가*/
        @JsonProperty("stck_lwpr") val stckLwpr: String,
        /**체결 거래량*/
        @JsonProperty("cntg_vol") val cntgVol: String,
    )
}