package com.setvect.bokslstock2.value.dto

data class CompanyDetail(
    val summary: CompanySummary,
    // true: 일반 주식, false: etf, 리츠 등
    val normalStock: Boolean,
    // 업종
    val industry: String,
    // 투자지표
    val currentIndicator: CurrentIndicator,
    val historyData: List<HistoryData>,
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

    data class HistoryData(
        val date: String, //yyyy.MM
        // 매출액
        val sales: Int?,
        // 영업이익
        val op: Int?,
        // 당기 순이익
        val np: Int?,
        // 부채 비율
        val dr: Double?,
        // 당좌 비율
        val cr: Double?,
        val eps: Double?,
        val per: Double?,
        val pbr: Double?,
        // 시가배당율
        val dvr: Double?,
        // 배당성향
        val dvrPayout: Double?,
    )

}
