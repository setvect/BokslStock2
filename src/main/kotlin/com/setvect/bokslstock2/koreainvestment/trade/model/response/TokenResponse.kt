package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty

data class TokenResponse(
    @JsonProperty("access_token") var accessToken: String,
    @JsonProperty("access_token_token_expired") var accessTokenTokenExpired: String,
    @JsonProperty("token_type") var tokenType: String,
    @JsonProperty("expires_in") var expiresIn: Long
)