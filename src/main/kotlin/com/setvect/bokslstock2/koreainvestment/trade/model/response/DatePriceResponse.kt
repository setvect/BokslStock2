package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty

data class DatePriceResponse(
    @JsonProperty("stck_bsop_date") val stckBsopDate: String,
    @JsonProperty("stck_oprc") val stckOprc: String,
    @JsonProperty("stck_hgpr") val stckHgpr: String,
    @JsonProperty("stck_lwpr") val stckLwpr: String,
    @JsonProperty("stck_clpr") val stckClpr: String,
    @JsonProperty("acml_vol") val acmlVol: String,
    @JsonProperty("prdy_vrss_vol_rate") val prdyVrssVolRate: String,
    @JsonProperty("prdy_vrss") val prdyVrss: String,
    @JsonProperty("prdy_vrss_sign") val prdyVrssSign: String,
    @JsonProperty("prdy_ctrt") val prdyCtrt: String,
    @JsonProperty("hts_frgn_ehrt") val htsFrgnEhrt: String,
    @JsonProperty("frgn_ntby_qty") val frgnNtbyQty: String,
    @JsonProperty("flng_cls_code") val flngClsCode: String,
    @JsonProperty("acml_prtt_rate") val acmlPrttRate: String
)