package com.setvect.bokslstock2.util

import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.pow

/**
 * 어플리케이션의 의존적인 유틸성 메소드
 */
object ApplicationUtil {

    /**
     * [prices] 시계열 가격 변화
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

    fun getMddByInt(prices: List<Int>): Double {
        return getMdd(prices.map { it.toDouble() })
    }

    fun getMddByLong(prices: List<Long>): Double {
        return getMdd(prices.map { it.toDouble() })
    }

    /**
     * [prices] 시계열 가격 변화
     * @return 수익률 1 == 100%
     */
    fun getYieldByLong(prices: List<Long>): Double {
        return if (prices.isEmpty()) {
            0.0
        } else getYield(prices[0], prices[prices.size - 1])
    }

    /**
     * [prices] 시계열 가격 변화
     * @return 수익률 1 == 100%
     */
    fun getYieldByInt(prices: List<Int>): Double {
        return if (prices.isEmpty()) {
            0.0
        } else getYield(prices[0], prices[prices.size - 1])
    }

    /**
     * [prices] 시계열 가격 변화
     * @return 수익률 1 == 100%
     */
    fun getYield(prices: List<Double>): Double {
        return if (prices.isEmpty()) {
            0.0
        } else getYield(prices[0], prices[prices.size - 1])
    }

    /**
     * 수익률 계산
     * [base] 기준 값, [delta] 변화 값
     * @return 수익률 1 == 100%
     */
    fun getYield(base: Int, delta: Int): Double {
        return getYield(base.toDouble(), delta.toDouble())
    }

    /**
     * 수익률 계산
     * [base] 기준 값, [delta] 변화 값
     * @return 수익률 1 == 100%
     */
    fun getYield(base: Long, delta: Long): Double {
        return getYield(base.toDouble(), delta.toDouble())
    }

    /**
     * 수익률 계산
     * [base] 기준 값, [delta] 변화 값
     * @return 수익률 1 == 100%
     */
    fun getYield(base: Double, delta: Double): Double {
        return delta / base - 1
    }

    /**
     * 연 복리
     * CAGR = (EV / BV) ^ (1 / dayCount) - 1
     *
     * [bv]       초기 값, BV (시작 값)
     * [ev]       종료 값, EV (종료 값)
     * [dayCount] 일수
     * @return 연복리
     */
    fun getCagr(bv: Double, ev: Double, dayCount: Int): Double {
        val year = dayCount / 365.0
        return (ev / bv).pow(1 / year) - 1
    }


    /**
     * [elementList] 부분 집합을 만들 원소
     * @return 공집합 제외한 부분집합
     */
    fun getSubSet(elementList: List<Long>): MutableList<Set<Long>> {
        val conditionSetList = mutableListOf<Set<Long>>()
        makeSubSet(elementList, conditionSetList, 0, BooleanArray(elementList.size))
        return conditionSetList
    }


    /**
     * [list] 집합을 만들 원소
     * [subSet] 부분집합
     * 부분집합 만듦
     */
    private fun makeSubSet(list: List<Long>, subSet: MutableList<Set<Long>>, idx: Int, check: BooleanArray) {
        if (list.size == idx) {
            val subSetItem = IntStream.range(0, list.size)
                .filter { num: Int -> check[num] }
                .mapToObj { num: Int -> list[num] }
                .collect(Collectors.toSet())

            // 공집합은 제외
            if (subSetItem.isNotEmpty()) {
                subSet.add(subSetItem)
            }
            return
        }
        check[idx] = true
        makeSubSet(list, subSet, idx + 1, check)
        check[idx] = false
        makeSubSet(list, subSet, idx + 1, check)
    }

}