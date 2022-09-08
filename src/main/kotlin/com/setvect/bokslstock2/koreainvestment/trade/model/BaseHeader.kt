package com.setvect.bokslstock2.koreainvestment.trade.model

import org.springframework.http.HttpHeaders

data class BaseHeader(
    val appkey: String,
    val appsecret: String,
    val authorization: String? = null,
    val trId: String? = null,
    val hashKey: String? = null,
    val contentType: String = "application/json"
) {
    fun headers(): HttpHeaders {
        val headers = HttpHeaders()
        headers.set("appkey", appkey)
        headers.set("appsecret", appsecret)
        if (authorization != null) {
            headers.set("authorization", "Bearer ${authorization}")
        }
        if (trId != null) {
            headers.set("tr_id", trId)
        }
        if (hashKey != null) {
            headers.set("hashkey", hashKey)
        }
        return headers
    }
}
