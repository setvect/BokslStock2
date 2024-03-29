package com.setvect.bokslstock2.backtest.dart.model

/**
 * 전체 재무제표 항목
 *
 * 같은 값을 나타내는 재무제표라도 이름이 다르다. ㅡㅡ;
 * @param accountId 재무제표 항목 ID, 1순위로 찾음
 * @param accountName 재무제표 항목명, 2순위로 찾음
 */
enum class FinancialDetailMetric(val financialSj: List<FinancialSj>, val accountId: String, val accountName: List<String>) {
    SALES_REVENUE(listOf(FinancialSj.IS, FinancialSj.CIS), "ifrs-full_Revenue", listOf("매출액", "수익(매출액)", "영업수익", "I. 영업수익")),
    OPERATING_PROFIT(listOf(FinancialSj.IS, FinancialSj.CIS), "dart_OperatingIncomeLoss", listOf("영업이익", "영업이익(손실)")),
    NET_PROFIT(listOf(FinancialSj.IS, FinancialSj.CIS), "ifrs-full_ProfitLoss", listOf("당기순이익", "당기순이익(손실)")),
    TOTAL_ASSETS(listOf(FinancialSj.BS), "ifrs-full_Assets", listOf("자산총계")),
    TOTAL_LIABILITIES(listOf(FinancialSj.BS), "ifrs-full_Liabilities", listOf("부채총계")),
    ;

    /**
     * @return 재무상태표 항목이면 true
     */
    fun isBs(): Boolean {
        return financialSj.intersect(listOf(FinancialSj.BS)).isNotEmpty()
    }

    /**
     * @return 손익계산서 항목이면 true
      */
    fun isIs(): Boolean {
        return financialSj.intersect(listOf(FinancialSj.IS, FinancialSj.CIS)).isNotEmpty()
    }

}