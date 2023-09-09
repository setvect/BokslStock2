package com.setvect.bokslstock2.strategy.inheritancetax.model

import com.setvect.bokslstock2.strategy.companyvalue.model.KoreanCompanySummary

/**
 * 상속세 전략 점수
 */
data class InheritanceTaxScore(
    // 기업정보
    val companyInfo: KoreanCompanySummary,
    // 자산(원)
    val assets: Long,
    // 부채(원)
    val liabilities: Long,
    // 최근 4분기 순이익(원)
    val currentYearProfit: Long,
    // 직전 4분기 순이익(원)
    val previousYearProfit: Long,
    // 전전 4분기 순이익(원)
    val twoYearsAgoProfit: Long,
) {
    /**
     * @return 순자산가치(원)
     */
    val netAssetValue = assets - liabilities

    /**
     * @return 순이익가치(원) = 최근 3년 순이익 가중평균 = (최근 연도 3/6 + 전 연도 2/6 + 전전 연도 1/6) * 10
     */
    val netProfitValue: Long = ((currentYearProfit * 3 / 6) + (previousYearProfit * 2 / 6) + (twoYearsAgoProfit * 1 / 6)) * 10

    /**
     * @return 상속세 전략 점수 = (순자산가치 * 60% + 순이익가치 * 40%) / 시총
     */
    val inheritanceTaxScore: Double = (netAssetValue * 0.6 + netProfitValue * 0.4) / (companyInfo.capitalization * 100_000_000L)
}