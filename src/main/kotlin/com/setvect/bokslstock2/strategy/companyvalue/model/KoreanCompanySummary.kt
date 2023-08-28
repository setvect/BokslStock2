package com.setvect.bokslstock2.strategy.companyvalue.model

data class KoreanCompanySummary(
    val code: String, // 종목코드
    val name: String,
    val market: String,
    val capitalization: Int, // 시가총액(억)
    val currentPrice: Int
) {
}
