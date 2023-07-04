package com.setvect.bokslstock2.util

import com.setvect.bokslstock2.backtest.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.index.model.PeriodType
import okhttp3.internal.toImmutableList
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.apache.http.HttpStatus
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import java.net.URLEncoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.pow
import kotlin.math.sqrt

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
     * @return 수익금
     */
    fun getYieldPrice(base: Double, delta: Double, qty: Int): Double {
        return (delta - base) * qty
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

    /**
     * [periodType]범위를 맞춘다. [date]포함된 범위의 시작 날짜. 글로 쓸라고 하니 엄청 어렵네. ㅡㅡ;
     * 예)
     * [periodType] = WEEK, [date] = 2022-08-12(금), return: 2022-08-08(월)
     * [periodType] = MONTH, [date] = 2022-08-12, return: 2022-08-01
     */
    fun fitStartDate(periodType: PeriodType, date: LocalDate): LocalDate {
        return fitStartDateTime(periodType, date.atTime(0, 0)).toLocalDate()
    }

    /**
     * [periodType]범위를 맞춘다. [dateTime]포함된 범위의 시작 날짜. 글로 쓸라고 하니 엄청 어렵네. ㅡㅡ;
     * 예)
     * [periodType] = WEEK, [dateTime] = 2022-08-12(금), return: 2022-08-08(월)
     * [periodType] = MONTH, [dateTime] = 2022-08-12, return: 2022-08-01
     */
    fun fitStartDateTime(periodType: PeriodType, dateTime: LocalDateTime): LocalDateTime {
        return when (periodType) {
            PeriodType.PERIOD_MINUTE_5 -> dateTime.withMinute(dateTime.minute - dateTime.minute % 5)
                .withSecond(0).withNano(0)

            PeriodType.PERIOD_DAY -> dateTime.withHour(0).withMinute(0).withSecond(0).withNano(0)
            PeriodType.PERIOD_WEEK -> DateUtil.convertDateOfMonday(dateTime)
            PeriodType.PERIOD_MONTH -> dateTime.withDayOfMonth(1)
            PeriodType.PERIOD_QUARTER, PeriodType.PERIOD_HALF, PeriodType.PERIOD_YEAR
            -> DateUtil.fitMonth(
                dateTime.withDayOfMonth(1),
                periodType.getDeviceMonth()
            ).atTime(0, 0)
        }
    }


    /**
     * [periodType]범위를 맞춘다. [date]포함된 범위의 종료 날짜. 글로 쓸라고 하니 엄청 어렵네. ㅡㅡ;
     * 예)
     * [periodType] = WEEK, [date] = 2022-08-11(목), return: 2022-08-12(금)
     * [periodType] = MONTH, [date] = 2022-08-12, return: 2022-08-31
     */
    fun fitEndDate(periodType: PeriodType, date: LocalDate): LocalDate {
        return when (periodType) {
            PeriodType.PERIOD_DAY -> date
            PeriodType.PERIOD_WEEK -> DateUtil.convertDateOfMonday(date)
            PeriodType.PERIOD_MONTH -> date.withDayOfMonth(1)
            PeriodType.PERIOD_QUARTER, PeriodType.PERIOD_HALF, PeriodType.PERIOD_YEAR
            -> DateUtil.fitMonth(
                date.withDayOfMonth(1),
                periodType.getDeviceMonth()
            ).plusMonths(periodType.getDeviceMonth().toLong()).minusDays(1)

            else -> {
                throw RuntimeException("$periodType 잘못 사용했다.")
            }
        }
    }

    fun fitEndDateTime(periodType: PeriodType, dateTime: LocalDateTime): LocalDateTime {
        return fitEndDate(periodType, dateTime.toLocalDate()).atTime(dateTime.toLocalTime())
    }

    fun makeSummaryCompareStock(
        buyHoldTotalYield: CommonAnalysisReportResult.TotalYield,
        buyHoldSharpeRatio: Double
    ): String {
        val report = StringBuilder()
        // TODO '합산 동일비중', '밴치마크' 이름 조건에 따라 변경
        report.append(String.format("합산 동일비중 수익\t %,.2f%%", buyHoldTotalYield.yield * 100))
            .append("\n")
        report.append(String.format("합산 동일비중 MDD\t %,.2f%%", buyHoldTotalYield.mdd * 100)).append("\n")
        report.append(String.format("합산 동일비중 CAGR\t %,.2f%%", buyHoldTotalYield.getCagr() * 100))
            .append("\n")
        report.append(String.format("샤프지수\t %,.2f", buyHoldSharpeRatio)).append("\n")
        return report.toString()
    }

    /**
     * 한 종목을 매수 할때 사용하는 금액 계산. 각종 변수값을 바탕으로 계산한다.
     *
     * 여담: 이거 공식 만드느라 X나 힘들었다. 분명 더 간단한 방법은 있을거다.
     *
     * [currentBuyStockCount] 현재 매수중인 종목 수
     * [cash] 현재 보유 현금
     * [stockBuyTotalCount] 매매 대상 종목 수
     * [investRatio] 전체 현금 대비 투자 비율. 1: 모든 현금을 투자, 0.5 현금의 50%만 매수에 사용
     *
     * @return 매수에 사용될 금액 반환
     */
    fun getBuyCash(
        currentBuyStockCount: Int,
        cash: Double,
        stockBuyTotalCount: Int,
        investRatio: Double
    ): Double {
        // 현재현금과 매수 종목 수를 가지고 역산해 총 현금을 구함
        // 시작현금 역산 = 현재현금 * 직전 매수 종목 수 / 매매 대상 종목수 * 사용비율 * 매매 대상 종목수  / 사용비율 / (매매 대상 종목수 / 사용비율 - 직전 매수 종목 수) + 현재현금
        val startCash =
            cash * currentBuyStockCount / stockBuyTotalCount * investRatio * stockBuyTotalCount / investRatio / (stockBuyTotalCount / investRatio - currentBuyStockCount) + cash
        // 매수에 사용할 현금 = 시작현금 역산 * 사용비율 * (1/매매종목수)
        return startCash * investRatio * (1 / stockBuyTotalCount.toDouble())
    }

    /**
     * 한 종목을 매수 할때 사용하는 금액 계산. 각종 변수값을 바탕으로 계산한다.
     *
     * [purchasedAllRatio] 현재 매수한 모든 종목의 매수 비율 함(0 ~ 1)
     * [cash] 현재 보유 현금
     * [buyRatio] 현재 매수할 종목 투자 비율(0 ~ 1)
     * [investRatio] 전체 현금 대비 투자 비율. 1: 모든 현금을 투자, 0.5 현금의 50%만 매수에 사용
     *
     * @return 매수에 사용될 금액 반환
     */
    fun getBuyCash(
        purchasedAllRatio: Double,
        cash: Double,
        buyRatio: Double,
        investRatio: Double
    ): Double {
        // 현재현금과 매수 종목의 투자비율 합을 가지고 역산해 총 현금을 구함
        // 시작현금 역산 = 현재현금 + (현재현금 * 매수한종목비율합계 / (1 - (매수한종목비율합계 - (1 / 사용비율 - 1))))
        val startCash = cash + (cash * purchasedAllRatio / (1 - (purchasedAllRatio - (1 / investRatio - 1))))
        // 매수에 사용할 현금 = 시작현금 역산 * 매매비율 * 사용비율
        return startCash * buyRatio * investRatio
    }

    /**
     * [yieldList] 투자 비율
     * @return 사프 지수
     */
    fun getSharpeRatio(yieldList: List<Double>): Double {
        return getSharpeRatio(yieldList, 0.0, yieldList.size)
    }

    /**
     *[yieldList] 월 단위 수익률
     * [riskFreeReturn] 무위험 수익률(예를 들어 3%라고 하면 0.03 입력)
     */
    fun getSharpeRatio(yieldList: List<Double>, riskFreeReturn: Double, periodByCount: Int): Double {
        val ds = DescriptiveStatistics(yieldList.toDoubleArray())
        val mean = ds.mean
        val stdev = ds.standardDeviation
        return (mean - riskFreeReturn) / stdev * sqrt(periodByCount.toDouble())
    }


    /**
     * chat GPT 통해서 얻은 계산 방식
     * [returns]가 일 기준 수익률이면 [periodsPerCount]는 247(연간 영업일 수)
     * [returns]가 월 기준 수익률이면 [periodsPerCount]는 12
     * TODO 검증 필요함
     */
    fun getSharpeRatioFromChatGpt(returns: List<Double>, riskFreeRate: Double, periodsPerCount: Int): Double {
        var expectedReturn = 0.0
        var expectedVolatility = 0.0

        for (returnValue in returns) {
            expectedReturn += returnValue
        }
        expectedReturn /= returns.size

        for (returnValue in returns) {
            expectedVolatility += (returnValue - expectedReturn).pow(2.0)
        }
        expectedVolatility = sqrt(expectedVolatility / returns.size)
        return (expectedReturn - riskFreeRate) / (expectedVolatility / sqrt(periodsPerCount.toDouble()))
    }

    /**
     * 가격 변화략에 대한 수익률 이력 계산
     */
    fun calcPriceYield(priceHistory: List<Double>): List<Double> {
        val yieldHistory = mutableListOf<Double>()
        for (i in 1 until priceHistory.size) {
            yieldHistory.add(getYield(priceHistory[i - 1], priceHistory[i]))
        }
        return yieldHistory.toImmutableList()
    }

    /**
     * @return 현제 날짜를 주기에 맞게 증가시킨 날짜 반환
     */
    fun incrementDate(
        periodType: PeriodType,
        current: LocalDate
    ): LocalDate {
        return when (periodType) {
            PeriodType.PERIOD_WEEK -> current.plusWeeks(1)
            PeriodType.PERIOD_MONTH -> current.plusMonths(1)
            PeriodType.PERIOD_QUARTER -> current.plusMonths(3)
            PeriodType.PERIOD_HALF -> current.plusMonths(6)
            PeriodType.PERIOD_YEAR -> current.plusYears(1)
            else -> current
        }
    }

    fun truncateDecimal(number: Double, decimalPoints: Int): Double {
        val powerOfTen = 10.0.pow(decimalPoints.toDouble())
        return (number * powerOfTen).toLong() / powerOfTen
    }

}