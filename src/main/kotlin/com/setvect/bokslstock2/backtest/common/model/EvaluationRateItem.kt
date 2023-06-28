package com.setvect.bokslstock2.backtest.common.model

import java.time.LocalDateTime

/**
 * Buy&Hold 및 전략 평가금액 변화 비율
 */
data class EvaluationRateItem(
    /**
     * 기준 날짜
     */
    val baseDate: LocalDateTime,
    /**
     * buy&hold 비율
     */
    val buyHoldRate: Double,
    /**
     * 밴치마크 비율
     */
    val benchmarkRate: Double,
    /**
     * 백테스팅 전략 비율
     */
    val backtestRate: Double,
    /**
     * buy&hold 전일대비 수익률
     */
    val buyHoldYield: Double,
    /**
     * 밴치 마크 비율
     */
    val benchmarkYield: Double,
    /**
     * 백테스팅 전략 전일대비 수익률
     */
    val backtestYield: Double,
)