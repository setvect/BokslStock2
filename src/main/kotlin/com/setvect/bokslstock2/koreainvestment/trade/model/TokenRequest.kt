package com.setvect.bokslstock2.koreainvestment.trade.model

import com.fasterxml.jackson.annotation.JsonProperty

data class TokenRequest(
    override val appkey: String,
    override val appsecret: String,
    @JsonProperty("grant_type") val grantType: String
) : BaseRequest(appkey, appsecret)
