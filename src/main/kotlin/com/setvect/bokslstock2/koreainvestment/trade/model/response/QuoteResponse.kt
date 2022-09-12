package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty

data class QuoteResponse(

    @JsonProperty("output1") val output1: QuoteLevel?,
    @JsonProperty("output2") val output2: QuoteInfo?,
    @JsonProperty("rt_cd") val rtCd: String,
    @JsonProperty("msg_cd") val msgCd: String,
    @JsonProperty("msg1") val msg1: String
) {

    data class QuoteLevel(
        /**호가 접수 시간*/
        @JsonProperty("aspr_acpt_hour") val asprAcptHour: String,
        /**매도호가1*/
        @JsonProperty("askp1") val askp1: String,
        /**매도호가2*/
        @JsonProperty("askp2") val askp2: String,
        /**매도호가3*/
        @JsonProperty("askp3") val askp3: String,
        /**매도호가4*/
        @JsonProperty("askp4") val askp4: String,
        /**매도호가5*/
        @JsonProperty("askp5") val askp5: String,
        /**매도호가6*/
        @JsonProperty("askp6") val askp6: String,
        /**매도호가7*/
        @JsonProperty("askp7") val askp7: String,
        /**매도호가8*/
        @JsonProperty("askp8") val askp8: String,
        /**매도호가9*/
        @JsonProperty("askp9") val askp9: String,
        /**매도호가10*/
        @JsonProperty("askp10") val askp10: String,
        /**매수호가1*/
        @JsonProperty("bidp1") val bidp1: String,
        /**매수호가2*/
        @JsonProperty("bidp2") val bidp2: String,
        /**매수호가3*/
        @JsonProperty("bidp3") val bidp3: String,
        /**매수호가4*/
        @JsonProperty("bidp4") val bidp4: String,
        /**매수호가5*/
        @JsonProperty("bidp5") val bidp5: String,
        /**매수호가6*/
        @JsonProperty("bidp6") val bidp6: String,
        /**매수호가7*/
        @JsonProperty("bidp7") val bidp7: String,
        /**매수호가8*/
        @JsonProperty("bidp8") val bidp8: String,
        /**매수호가9*/
        @JsonProperty("bidp9") val bidp9: String,
        /**매수호가10*/
        @JsonProperty("bidp10") val bidp10: String,
        /**매도호가 잔량1*/
        @JsonProperty("askp_rsqn1") val askpRsqn1: String,
        /**매도호가 잔량2*/
        @JsonProperty("askp_rsqn2") val askpRsqn2: String,
        /**매도호가 잔량3*/
        @JsonProperty("askp_rsqn3") val askpRsqn3: String,
        /**매도호가 잔량4*/
        @JsonProperty("askp_rsqn4") val askpRsqn4: String,
        /**매도호가 잔량5*/
        @JsonProperty("askp_rsqn5") val askpRsqn5: String,
        /**매도호가 잔량6*/
        @JsonProperty("askp_rsqn6") val askpRsqn6: String,
        /**매도호가 잔량7*/
        @JsonProperty("askp_rsqn7") val askpRsqn7: String,
        /**매도호가 잔량8*/
        @JsonProperty("askp_rsqn8") val askpRsqn8: String,
        /**매도호가 잔량9*/
        @JsonProperty("askp_rsqn9") val askpRsqn9: String,
        /**매도호가 잔량10*/
        @JsonProperty("askp_rsqn10") val askpRsqn10: String,
        /**매수호가 잔량1*/
        @JsonProperty("bidp_rsqn1") val bidpRsqn1: String,
        /**매수호가 잔량2*/
        @JsonProperty("bidp_rsqn2") val bidpRsqn2: String,
        /**매수호가 잔량3*/
        @JsonProperty("bidp_rsqn3") val bidpRsqn3: String,
        /**매수호가 잔량4*/
        @JsonProperty("bidp_rsqn4") val bidpRsqn4: String,
        /**매수호가 잔량5*/
        @JsonProperty("bidp_rsqn5") val bidpRsqn5: String,
        /**매수호가 잔량6*/
        @JsonProperty("bidp_rsqn6") val bidpRsqn6: String,
        /**매수호가 잔량7*/
        @JsonProperty("bidp_rsqn7") val bidpRsqn7: String,
        /**매수호가 잔량8*/
        @JsonProperty("bidp_rsqn8") val bidpRsqn8: String,
        /**매수호가 잔량9*/
        @JsonProperty("bidp_rsqn9") val bidpRsqn9: String,
        /**매수호가 잔량10*/
        @JsonProperty("bidp_rsqn10") val bidpRsqn10: String,
        /**매도호가 잔량 증감1*/
        @JsonProperty("askp_rsqn_icdc1") val askpRsqnIcdc1: String,
        /**매도호가 잔량 증감2*/
        @JsonProperty("askp_rsqn_icdc2") val askpRsqnIcdc2: String,
        /**매도호가 잔량 증감3*/
        @JsonProperty("askp_rsqn_icdc3") val askpRsqnIcdc3: String,
        /**매도호가 잔량 증감4*/
        @JsonProperty("askp_rsqn_icdc4") val askpRsqnIcdc4: String,
        /**매도호가 잔량 증감5*/
        @JsonProperty("askp_rsqn_icdc5") val askpRsqnIcdc5: String,
        /**매도호가 잔량 증감6*/
        @JsonProperty("askp_rsqn_icdc6") val askpRsqnIcdc6: String,
        /**매도호가 잔량 증감7*/
        @JsonProperty("askp_rsqn_icdc7") val askpRsqnIcdc7: String,
        /**매도호가 잔량 증감8*/
        @JsonProperty("askp_rsqn_icdc8") val askpRsqnIcdc8: String,
        /**매도호가 잔량 증감9*/
        @JsonProperty("askp_rsqn_icdc9") val askpRsqnIcdc9: String,
        /**매도호가 잔량 증감10*/
        @JsonProperty("askp_rsqn_icdc10") val askpRsqnIcdc10: String,
        /**매수호가 잔량 증감1*/
        @JsonProperty("bidp_rsqn_icdc1") val bidpRsqnIcdc1: String,
        /**매수호가 잔량 증감2*/
        @JsonProperty("bidp_rsqn_icdc2") val bidpRsqnIcdc2: String,
        /**매수호가 잔량 증감3*/
        @JsonProperty("bidp_rsqn_icdc3") val bidpRsqnIcdc3: String,
        /**매수호가 잔량 증감4*/
        @JsonProperty("bidp_rsqn_icdc4") val bidpRsqnIcdc4: String,
        /**매수호가 잔량 증감5*/
        @JsonProperty("bidp_rsqn_icdc5") val bidpRsqnIcdc5: String,
        /**매수호가 잔량 증감6*/
        @JsonProperty("bidp_rsqn_icdc6") val bidpRsqnIcdc6: String,
        /**매수호가 잔량 증감7*/
        @JsonProperty("bidp_rsqn_icdc7") val bidpRsqnIcdc7: String,
        /**매수호가 잔량 증감8*/
        @JsonProperty("bidp_rsqn_icdc8") val bidpRsqnIcdc8: String,
        /**매수호가 잔량 증감9*/
        @JsonProperty("bidp_rsqn_icdc9") val bidpRsqnIcdc9: String,
        /**매수호가 잔량 증감10*/
        @JsonProperty("bidp_rsqn_icdc10") val bidpRsqnIcdc10: String,
        /**총 매도호가 잔량*/
        @JsonProperty("total_askp_rsqn") val totalAskpRsqn: String,
        /**총 매수호가 잔량*/
        @JsonProperty("total_bidp_rsqn") val totalBidpRsqn: String,
        /**총 매도호가 잔량 증감*/
        @JsonProperty("total_askp_rsqn_icdc") val totalAskpRsqnIcdc: String,
        /**총 매수호가 잔량 증감*/
        @JsonProperty("total_bidp_rsqn_icdc") val totalBidpRsqnIcdc: String,
        /**시간외 총 매도호가 증감*/
        @JsonProperty("ovtm_total_askp_icdc") val ovtmTotalAskpIcdc: String,
        /**시간외 총 매수호가 증감*/
        @JsonProperty("ovtm_total_bidp_icdc") val ovtmTotalBidpIcdc: String,
        /**시간외 총 매도호가 잔량*/
        @JsonProperty("ovtm_total_askp_rsqn") val ovtmTotalAskpRsqn: String,
        /**시간외 총 매수호가 잔량*/
        @JsonProperty("ovtm_total_bidp_rsqn") val ovtmTotalBidpRsqn: String,
        /**순매수 호가 잔량*/
        @JsonProperty("ntby_aspr_rsqn") val ntbyAsprRsqn: String,
        /**신 장운영 구분 코드*/
        @JsonProperty("new_mkop_cls_code") val newMkopClsCode: String
    )


    data class QuoteInfo(
        /**예상 장운영 구분 코드*/
        @JsonProperty("antc_mkop_cls_code") val antcMkopClsCode: String,
        /**주식 현재가*/
        @JsonProperty("stck_prpr") val stckPrpr: String,
        /**주식 시가*/
        @JsonProperty("stck_oprc") val stckOprc: String,
        /**주식 최고가*/
        @JsonProperty("stck_hgpr") val stckHgpr: String,
        /**주식 최저가*/
        @JsonProperty("stck_lwpr") val stckLwpr: String,
        /**주식 기준가*/
        @JsonProperty("stck_sdpr") val stckSdpr: String,
        /**예상 체결가*/
        @JsonProperty("antc_cnpr") val expectedPrice: Int,
        /**예상 체결 대비 부호*/
        @JsonProperty("antc_cntg_vrss_sign") val antcCntgVrssSign: String,
        /**예상 체결 대비*/
        @JsonProperty("antc_cntg_vrss") val antcCntgVrss: String,
        /**예상 체결 전일 대비율*/
        @JsonProperty("antc_cntg_prdy_ctrt") val antcCntgPrdyCtrt: String,
        /**예상 거래량*/
        @JsonProperty("antc_vol") val antcVol: String,
        /**주식 단축 종목코드*/
        @JsonProperty("stck_shrn_iscd") val stckShrnIscd: String,
        /**VI적용구분코드*/
        @JsonProperty("vi_cls_code") val viClsCode: String
    )
}