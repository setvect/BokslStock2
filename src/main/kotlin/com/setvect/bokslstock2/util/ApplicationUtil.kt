package com.setvect.bokslstock2.util

import java.net.URLEncoder
import java.util.stream.Collectors
import java.util.stream.IntStream
import org.apache.http.HttpStatus
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import kotlin.math.pow

/**
 * 어플리케이션의 의존적인 유틸성 메소드
 */
object ApplicationUtil {
    /**
     * API 타임 아웃
     * connection, socket 둘다 적용
     */
    private const val TIMEOUT_MS = 2000

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

    fun getQueryString(params: Map<String, String>): String {
        return params.entries.stream()
            .map { (key, value): Map.Entry<String, String> ->
                "$key=" + urlEncodeUTF8(value)
            }
            .reduce { p1: String, p2: String -> "$p1&$p2" }
            .orElse("")
    }

    private fun urlEncodeUTF8(s: String): String? {
        return URLEncoder.encode(s, "UTF-8")
    }


    fun request(url: String, request: HttpRequestBase): String? {
        val client: HttpClient = HttpClientBuilder.create().build()
        val config = RequestConfig.custom()
            .setConnectTimeout(TIMEOUT_MS)
            .setConnectionRequestTimeout(TIMEOUT_MS)
            .setSocketTimeout(TIMEOUT_MS).build()
        request.config = config

        val response = client.execute(request)
        val statusCode = response.statusLine.statusCode
        val entity = response.entity
        val responseText = EntityUtils.toString(entity, "UTF-8")
        if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_CREATED) {
            val message = String.format("Error, Status: %d, URL: %s, Message: %s", statusCode, url, responseText)
            throw RuntimeException(message)
        }
        return responseText
    }

}