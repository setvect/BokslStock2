package com.setvect.bokslstock2.backtest.ivbs.model

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.util.DateRange

/**
 * 변동성돌파 전략 조건
 */
class IvbsBacktestCondition(
    /** 매매 기간 */
    val range: DateRange,

    /** 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자 */
    val investRatio: Double,

    /**  투자금액 */
    val cash: Double,

    /**
     * 조건들
     */
    val conditionList: ArrayList<IvbsConditionItem>
) {
    // 전체 투자비율 합
    private fun getInvestRatioSum(): Double {
        return conditionList.sumOf { it.investmentRatio }
    }

    // 전체 투자비율이 1일 넘으면 예외 발생 함수
    fun checkInvestRatioSum() {
        val sum = getInvestRatioSum()
        if (sum > 1) {
            throw IllegalArgumentException("전체 투자 비율 합이 1을 넘습니다. 합: $sum")
        }
    }

}
