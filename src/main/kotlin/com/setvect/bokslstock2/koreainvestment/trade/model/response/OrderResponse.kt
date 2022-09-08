package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty

data class OrderResponse(

    @JsonProperty("KRX_FWDG_ORD_ORGNO") val KRXFWDGORDORGNO: String,
    @JsonProperty("ODNO") val ODNO: String,
    @JsonProperty("ORD_TMD") val ORDTMD: String

)