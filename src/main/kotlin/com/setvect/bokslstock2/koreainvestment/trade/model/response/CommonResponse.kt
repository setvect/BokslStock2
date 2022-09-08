package com.setvect.bokslstock2.koreainvestment.trade.model.response

import com.fasterxml.jackson.annotation.JsonProperty

data class CommonResponse<T>(

    @JsonProperty("output") var output: T,
    @JsonProperty("rt_cd") var rtCd: String,
    @JsonProperty("msg_cd") var msgCd: String,
    @JsonProperty("msg1") var msg1: String

)