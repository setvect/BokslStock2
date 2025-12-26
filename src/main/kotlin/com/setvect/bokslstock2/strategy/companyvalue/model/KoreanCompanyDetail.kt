package com.setvect.bokslstock2.strategy.companyvalue.model

data class KoreanCompanyDetail(
    val summary: KoreanCompanySummary,
    // true: 일반 주식, false: etf, 리츠 등
    val normalStock: Boolean,
    // 투자지표
    val currentIndicator: CurrentIndicator,
) {
    /**
     * 투자 지표
     */
    data class CurrentIndicator(
        // 상장주식수
        val shareNumber: Long,
        val per: Double?,
        val eps: Double?,
        val pbr: Double?,
        // 현금배당 수익률 (%단위)
        val dvr: Double? = null,
    )

}
