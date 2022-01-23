package com.setvect.bokslstock2.util

import kotlin.math.pow

/**
 * 어플리케이션의 의존적인 유틸성 메소드
 */
object ApplicationUtil {
    /**
     * @param prices 시계열 가격 변화
     * @return 최대 낙폭 계산 - MDD(Max. Draw Down)
     */
    fun getMdd(prices: List<Double>): Double {
        var highValue = 0.0
        var mdd = 0.0
        for (v in prices) {
            if (highValue < v) {
                highValue = v
            } else {
                mdd = mdd.coerceAtMost(v / highValue - 1)
            }
        }
        return mdd
    }

    /**
     * @param prices 시계열 가격 변화
     * @return 수익률 1 == 100%
     */
    fun getYield(prices: List<Double>): Double {
        return if (prices.isEmpty()) {
            0.0
        } else getYield(prices[0], prices[prices.size - 1])
    }

    /**
     * @param start   시작값
     * @param current 현재 값
     * @return 수익률 1 == 100%
     */
    fun getYield(start: Double, current: Double): Double {
        return current / start - 1
    }

    /**
     * 연 복리
     * CAGR = (EV / BV) ^ (1 / dayCount) - 1
     *
     * @param bv       초기 값, BV (시작 값)
     * @param ev       종료 값, EV (종료 값)
     * @param dayCount 일수
     * @return 연복리
     */
    fun getCagr(bv: Double, ev: Double, dayCount: Int): Double {
        val year = dayCount / 365.0
        return (ev / bv).pow(1 / year) - 1
    }
}