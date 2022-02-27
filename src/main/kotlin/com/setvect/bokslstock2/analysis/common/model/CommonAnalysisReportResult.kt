package com.setvect.bokslstock2.analysis.common.model

import com.setvect.bokslstock2.util.ApplicationUtil

/**
 * 멀티 종목 매매 백테스트 분석 결과
 */
data class CommonAnalysisReportResult(
    /**
     * 날짜별 평가금 변화 이력
     */
    val evaluationAmountHistory: List<EvaluationRateItem>,
    /**
     * 월별 수익률
     */
    val monthlyYield: List<YieldRateItem>,
    /**
     * 전체 수익 결과
     */
    val yieldTotal: TotalYield,

    /**
     * 조건 기준 승률 합
     * <조건아이디, 승률>
     */
    val winningRateCondition: Map<Long, WinningRate>,

    /**
     * 조건별 종목 Buy&Hold 수익률
     * <조건아이디, 수익률>
     */
    val buyAndHoldYieldCondition: Map<Long, YieldMdd>,

    /**
     * 종목 Buy&Hold 수익률
     */
    val buyAndHoldYieldTotal: TotalYield,
) {
    /**
     *@return 전체 매매 내역 승률 합
     */
    fun getWinningRateTotal(): WinningRate {
        return WinningRate(
            gainCount = winningRateCondition.values.sumOf { it.gainCount },
            lossCount = winningRateCondition.values.sumOf { it.lossCount },
            invest = winningRateCondition.values.sumOf { it.invest },
        )
    }

    /*
     * 수익률과 MDD
     */
    data class YieldMdd(
        /**
         * 수익률
         */
        val yield: Double,

        /**
         * 최대 낙폭
         */
        val mdd: Double,
    )

    data class TotalYield(
        /**
         * 수익률
         */
        val yield: Double,

        /**
         * 최대 낙폭
         */
        val mdd: Double,
        /**
         * 분석 일자
         */
        val dayCount: Int,

        ) {
        /**
         * @return 연복리
         */
        fun getCagr(): Double {
            return ApplicationUtil.getCagr(1.0, 1 + `yield`, dayCount)
        }
    }

    /**
     * 단위 수익 정보
     */
    data class WinningRate(
        /**
         * 수익 카운트
         */
        val gainCount: Int,

        /**
         * 이익 카운트
         */
        val lossCount: Int,

        /**
         * 수익 합계
         */
        val invest: Double,

        ) {
        /**
         * @return 총 매매 횟수 (매수-매도가 한쌍)
         */
        fun getTradeCount(): Int {
            return gainCount + lossCount
        }

        /**
         * @return 총 매매에서 이익을 본 비율
         */
        fun getWinRate(): Double {
            return gainCount.toDouble() / getTradeCount()
        }
    }
}