package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty

data class HashResponse(
    @JsonProperty("BODY") var body: Any,
    @JsonProperty("HASH") var hash: String
)