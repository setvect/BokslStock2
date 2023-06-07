package com.setvect.bokslstock2.analysis.price

import com.setvect.bokslstock2.analysis.common.model.StockCode.*
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.NumberUtil
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

/**
 * 책 "주식 투자 ETF로 시작하라" 백테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class BookEtfBacktest {
    @Autowired
    private lateinit var movingAverageService: MovingAverageService

    @Autowired
    private lateinit var candleRepository: CandleRepository
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 매월 말 코스피지수의 12개월 평균 모멘텀 스코어 구함
     * - 미리 정해놓은 고정비율(25%)와 평균 모멘텀 스코어를 곱한 비율이 주식투자비율, 나머지가 현금비율
     * - 매월 말 이 비율을 계산하여 리밸런싱
     * - 코스피 고정 투자 비율 10%, 코스피 평균 모멘텀 스코어 0.5 인 경우
     *  - 주식투자비율 = 25% × 0.5 = 12.5%,
     *  - 현금 투자비율 = 100% - 12.5% = 87.5%
     */
    @Test
    @DisplayName("고정비율 투자법+평균 모멘텀 스코어 비중 전략")
    fun test1() {
        val stockRatio = 0.25
        val cash = 10_000_000.0
        val balanceHistory = mutableListOf<Balance>()

        val dateRange = DateRange(LocalDate.of(2018, 1, 1), LocalDate.of(2022, 1, 31))
        val candleList = movingAverageService.getMovingAverage(
            KODEX_200_069500,
            PeriodType.PERIOD_DAY,
            PeriodType.PERIOD_MONTH,
            listOf(1),
            dateRange
        )
        balanceHistory.add(Balance(candleList.first().candleDateTimeStart.toLocalDate(), cash, 0))

        for (candle in candleList) {
            val balanceAmount = balanceHistory.last().getBalance(candle.closePrice)
            val investment = balanceAmount * stockRatio
            val qty = (investment / candle.closePrice).toInt()
            val purchaseAmount = (qty * candle.closePrice).toInt()
            balanceHistory.add(Balance(candleList.first().candleDateTimeEnd.toLocalDate(), balanceAmount - purchaseAmount, qty))
        }
        val balanceAmount = balanceHistory.last().getBalance(candleList.last().closePrice)
        println("최종 평가금액: ${NumberUtil.comma(balanceAmount.toInt())}, 수익률: ${NumberUtil.percent((balanceAmount / cash - 1) * 100)}")

    }

    data class Balance(val date: LocalDate, val cash: Double, val qty: Int) {
        fun getBalance(unitPrice: Double): Double {
            return cash + unitPrice * qty
        }
    }
}