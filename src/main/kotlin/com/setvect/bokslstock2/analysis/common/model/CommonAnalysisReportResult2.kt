package com.setvect.bokslstock2.analysis.common.model

import com.setvect.bokslstock2.util.ApplicationUtil
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import kotlin.math.sqrt
import kotlin.streams.toList

/**
 * 멀티 종목 매매 백테스트 분석 결과
 */
data class CommonAnalysisReportResult2(
    /**
     * 날짜별 평가금 변화 이력
     */
    val evaluationAmountHistory: List<EvaluationRateItem>,

    /**
     * 전체 수익 결과
     */
    val yieldTotal: CommonAnalysisReportResult.TotalYield,

    /**
     * 조건 기준 승률 합
     * <종목코드, 승률>
     */
    val winningRateCondition: Map<String, CommonAnalysisReportResult.WinningRate>,

    /**
     * 조건별 종목 Buy&Hold 수익률
     * <종목코드, 수익률>
     */
    val buyHoldYieldCondition: Map<String, CommonAnalysisReportResult.YieldMdd>,

    /**
     * 종목 Buy&Hold 수익률
     */
    val buyHoldYieldTotal: CommonAnalysisReportResult.TotalYield,
) {
    /**
     *@return 전체 매매 내역 승률 합
     */
    fun getWinningRateTotal(): CommonAnalysisReportResult.WinningRate {
        return CommonAnalysisReportResult.WinningRate(
            gainCount = winningRateCondition.values.sumOf { it.gainCount },
            lossCount = winningRateCondition.values.sumOf { it.lossCount },
            invest = winningRateCondition.values.sumOf { it.invest },
        )
    }

    /**
     * @return 월별 buy&hold 수익률, 전략 수익률 정보
     */
    fun getMonthlyYield(): List<YieldRateItem> {
        val monthEval = evaluationAmountHistory.groupBy { it.baseDate.withDayOfMonth(1) }
        return monthEval.entries.stream().map {
            YieldRateItem(
                baseDate = it.key,
                buyHoldYield = ApplicationUtil.getYield(it.value.first().buyHoldRate, it.value.last().buyHoldRate),
                backtestYield = ApplicationUtil.getYield(it.value.first().backtestRate, it.value.last().backtestRate),
            )
        }
            .toList()
    }

    /**
     * @return 년별 buy&hold 수익률, 전략 수익률 정보
     */
    fun getYearlyYield(): List<YieldRateItem> {
        val yearEval = evaluationAmountHistory.groupBy { it.baseDate.withMonth(1).withDayOfMonth(1) }
        return yearEval.entries.stream().map {
            YieldRateItem(
                baseDate = it.key,
                buyHoldYield = ApplicationUtil.getYield(it.value.first().buyHoldRate, it.value.last().buyHoldRate),
                backtestYield = ApplicationUtil.getYield(it.value.first().backtestRate, it.value.last().backtestRate),
            )
        }
            .toList()
    }

    /**
     * @return Buy&Hold 사프지수
     */
    fun getBuyHoldSharpeRatio(): Double {
        return getSharpeRatio(evaluationAmountHistory.stream().map { it.buyHoldYield }.toList())
    }

    /**
     * @return 전략 사프지수
     */
    fun getBacktestSharpeRatio(): Double {
        return getSharpeRatio(evaluationAmountHistory.stream().map { it.backtestYield }.toList())
    }

    /**
     * [yieldList] 투자 비율
     * @return 사프 지수
     */
    private fun getSharpeRatio(yieldList: List<Double>): Double {
        val ds = DescriptiveStatistics(yieldList.toDoubleArray())
        val mean = ds.mean
        val stdev = ds.standardDeviation
        return mean / stdev * sqrt(yieldList.size.toDouble())
    }
}