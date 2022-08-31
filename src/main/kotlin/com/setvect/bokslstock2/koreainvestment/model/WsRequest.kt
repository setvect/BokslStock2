package com.setvect.bokslstock2.koreainvestment.model

import com.fasterxml.jackson.annotation.JsonProperty

data class WsRequest(
    val header: Header,
    val body: Body
) {
    data class Body(
        val input: Input
    )

    data class Header(
        val appkey: String,
        val appsecret: String,
        val custtype: String,
        val trType: String,
        @JsonProperty("content-type")
        val contentType: String,
    )

    data class Input(
        val trId: String,
        val trKey: String
    )
}


