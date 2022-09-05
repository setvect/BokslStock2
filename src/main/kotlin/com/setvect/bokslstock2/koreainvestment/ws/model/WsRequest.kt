package com.setvect.bokslstock2.koreainvestment.ws.model

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
        @get:JsonProperty("tr_type")
        val trType: String,
        @get:JsonProperty("content-type")
        val contentType: String,
    )

    data class Input(
        @get:JsonProperty("tr_id")
        val trId: WsTransaction,
        @get:JsonProperty("tr_key")
        val trKey: String
    )
}