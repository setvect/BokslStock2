package com.setvect.bokslstock2.backtest.common.model

import java.time.LocalDateTime

/**
 * Buy&Hold, 밴치마크, 전략 기간별 수익률 표현
 */
data class YieldRateItem(
    /**
     * 기준 날짜
     */
    val baseDate: LocalDateTime,
    /**
     * buy&hold 수익률
     */
    val buyHoldYield: Double,
    /**
     * 밴치마크 수익률
     */
    val benchmarkYield: Double,
    /**
     * 백테스팅 전략 수익률
     */
    val backtestYield: Double,
)