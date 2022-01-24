package com.setvect.bokslstock2.analysis.model

import com.setvect.bokslstock2.util.ApplicationUtil

/**
 * 멀티 코인 매매 백테스트 분석 결과
 */
data class AnalysisReportResult(
    /**
     * 리포트 조건
     */
    val analysisMabsCondition: AnalysisMabsCondition,

    /**
     * 매매 이력
     */
    val tradeHistory: List<TradeReportItem>,

    /**
     * 전체 수익 결과
     */
    val totalYield: TotalYield,

    /**
     * 종목 승률
     */
    val coinWinningRate: WinningRate,

    /**
     * 종목 Buy&Hold 수익률
     */
    val stockHoldYield: YieldMdd,
) {
    /**
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
    ) {
    }

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
        /**
         * 수익 카운트
         */
        var gainCount: Int = 0,
        /**
         * 이익 카운트
         */
        var lossCount: Int = 0,
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
            return if (getTradeCount() == 0) 0.0 else gainCount.toDouble() / getTradeCount()
        }

        /**
         * @return 연복리
         */
        fun getCagr(): Double {
            return ApplicationUtil.getCagr(1.0, 1 + `yield`, dayCount)
        }

        /**
         * 수익 카운트 증가
         */
        fun incrementGainCount() {
            gainCount++
        }

        /**
         * 손실 카운트 증가
         */
        fun incrementLossCount() {
            lossCount++
        }
    }

    /**
     * 단위 수익 정보
     * TODO 필요 없을것 같음. 화인해 보기
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
        val invest: Long,

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