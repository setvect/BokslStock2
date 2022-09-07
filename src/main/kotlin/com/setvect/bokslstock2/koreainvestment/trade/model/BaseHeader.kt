package com.setvect.bokslstock2.koreainvestment.trade.model

data class BaseHeader(
    val contentType: String,
    val appkey: String,
    val appsecret: String,
    val authorization: String,
    val trId: String,
    val hashKey: String
)
