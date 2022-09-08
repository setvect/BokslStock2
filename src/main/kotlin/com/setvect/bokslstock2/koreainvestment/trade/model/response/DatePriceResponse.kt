package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty

data class DatePriceResponse(
    @JsonProperty("stck_bsop_date") var stckBsopDate: String,
    @JsonProperty("stck_oprc") var stckOprc: String,
    @JsonProperty("stck_hgpr") var stckHgpr: String,
    @JsonProperty("stck_lwpr") var stckLwpr: String,
    @JsonProperty("stck_clpr") var stckClpr: String,
    @JsonProperty("acml_vol") var acmlVol: String,
    @JsonProperty("prdy_vrss_vol_rate") var prdyVrssVolRate: String,
    @JsonProperty("prdy_vrss") var prdyVrss: String,
    @JsonProperty("prdy_vrss_sign") var prdyVrssSign: String,
    @JsonProperty("prdy_ctrt") var prdyCtrt: String,
    @JsonProperty("hts_frgn_ehrt") var htsFrgnEhrt: String,
    @JsonProperty("frgn_ntby_qty") var frgnNtbyQty: String,
    @JsonProperty("flng_cls_code") var flngClsCode: String,
    @JsonProperty("acml_prtt_rate") var acmlPrttRate: String
)