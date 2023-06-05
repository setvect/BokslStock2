package com.setvect.bokslstock2.value.dto

class UsaCompanyDetail(
    val ticker: String,
    val name: String,
    val sector: String,
    val industry: String,
    val country: String,
    // DJIA, NDX, S&P 500
    val index: Set<String>,
    // 현재 가격
    val price: Double?,
    // 시가총액
    val marketCap: Double?,
    // 투자지표
    val currentIndicator: CurrentIndicator,
    ) {
    /**
     * 투자 지표
     */
    data class CurrentIndicator(
        val per: Double?,
        val eps: Double?,
        val pbr: Double?,
        val dvr: Double? = null,
    )
}