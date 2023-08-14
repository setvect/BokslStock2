package com.setvect.bokslstock2.backtest.dart.model

/**
 * 재무제표 항목
 *
 * 같은 값을 나타내는 재무제표라도 이름이 다르다. ㅡㅡ;
 */
enum class FinancialMetric(val itemName: List<String>) {
    SALES_REVENUE(listOf("매출액", "수익(매출액)", "영업수익", "I. 영업수익")),
    OPERATING_PROFIT(listOf("영업이익", "영업이익(손실)")),
    TOTAL_ASSETS(listOf("자산총계")),
    TOTAL_LIABILITIES(listOf("부채총계")),
    NET_PROFIT(listOf("당기순이익", "당기순이익(손실)")),
    ETC_PROFIT(listOf("기타수익")),
    ;
}