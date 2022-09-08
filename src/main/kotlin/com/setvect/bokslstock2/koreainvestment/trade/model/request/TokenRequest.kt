package com.setvect.bokslstock2.koreainvestment.trade.model.request

import com.fasterxml.jackson.annotation.JsonProperty

data class TokenRequest(
    val appkey: String,
    val appsecret: String,
    @JsonProperty("grant_type") val grantType: String
)
