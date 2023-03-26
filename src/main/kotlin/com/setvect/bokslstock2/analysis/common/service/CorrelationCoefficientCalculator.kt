package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.DateRange
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * 상관계수
 */
@Component
class CorrelationCoefficientCalculator(
    val movingAverageService: MovingAverageService,
    val backtestTradeService: BacktestTradeService
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 월단위 수익률을
     * @return 상관계수
     */
    fun calculateByMonth(stockCode1: StockCode, stockCode2: StockCode, dateRange: DateRange): CorrelationResult {
        // 계산 범위보다 한 달 더 넓게 계산. 그렇게 해야 직전달의 정보가 있음
        val corrDateRange = DateRange(dateRange.fromDate.minusMonths(1), dateRange.toDate)

        val fitRange = backtestTradeService.fitBacktestRange(listOf(stockCode1, stockCode2), corrDateRange, 1)
        log.debug("범위 조건 변경: $corrDateRange -> $fitRange")

        val yieldRate1 = getYield(fitRange, stockCode1)
        val yieldRate2 = getYield(fitRange, stockCode2)

        val corr = PearsonsCorrelation()
        val correlation = corr.correlation(yieldRate1.toDoubleArray(), yieldRate2.toDoubleArray())
        return CorrelationResult(DateRange(fitRange.fromDate.plusMonths(1), fitRange.toDate), correlation)
    }

    private fun getYield(
        fitRange: DateRange,
        stockCode: StockCode
    ): MutableList<Double> {
        return movingAverageService.getMovingAverage(
            stockCode,
            PeriodType.PERIOD_DAY,
            PeriodType.PERIOD_MONTH,
            listOf(),
            fitRange
        ).stream()
            // 시작달은 직전달의 정보가 없기 때문에 제외
            .skip(1)
            .map { it.getYield() }.toList()
    }

    data class CorrelationResult(val dateRange: DateRange, val correlation: Double)
}