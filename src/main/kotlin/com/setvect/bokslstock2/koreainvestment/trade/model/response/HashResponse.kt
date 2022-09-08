package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty

data class HashResponse(
    @JsonProperty("BODY") val body: Any,
    @JsonProperty("HASH") val hash: String
)