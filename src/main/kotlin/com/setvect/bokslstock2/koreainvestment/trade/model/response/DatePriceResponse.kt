package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty
import com.setvect.bokslstock2.util.DateUtil
import java.time.LocalDate

data class DatePriceResponse(
    /**주식 영업 일자*/
    @JsonProperty("stck_bsop_date") val stckBsopDate: String,
    /**주식 시가*/
    @JsonProperty("stck_oprc") val stckOprc: Int,
    /**주식 최고가*/
    @JsonProperty("stck_hgpr") val stckHgpr: Int,
    /**주식 최저가*/
    @JsonProperty("stck_lwpr") val stckLwpr: Int,
    /**주식 종가*/
    @JsonProperty("stck_clpr") val stckClpr: Int,
    /**누적 거래량*/
    @JsonProperty("acml_vol") val acmlVol: Long,
    /**전일 대비 거래량 비율*/
    @JsonProperty("prdy_vrss_vol_rate") val prdyVrssVolRate: String,
    /**전일 대비*/
    @JsonProperty("prdy_vrss") val prdyVrss: String,
    /**전일 대비 부호*/
    @JsonProperty("prdy_vrss_sign") val prdyVrssSign: String,
    /**전일 대비율*/
    @JsonProperty("prdy_ctrt") val prdyCtrt: String,
    /**HTS 외국인 소진율*/
    @JsonProperty("hts_frgn_ehrt") val htsFrgnEhrt: String,
    /**외국인 순매수 수량*/
    @JsonProperty("frgn_ntby_qty") val frgnNtbyQty: String,
    /**락 구분 코드*/
    @JsonProperty("flng_cls_code") val flngClsCode: String,
    /**누적 분할 비율*/
    @JsonProperty("acml_prtt_rate") val acmlPrttRate: String
) {
    fun date(): LocalDate {
        return DateUtil.getLocalDate(stckBsopDate, DateUtil.yyyyMMdd)
    }
}
