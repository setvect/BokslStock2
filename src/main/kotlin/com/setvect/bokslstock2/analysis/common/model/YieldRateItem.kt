package com.setvect.bokslstock2.analysis.common.model

import java.time.LocalDateTime

/**
 * Buy&Hold 및 전략 기간별 수익률 표현
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
     * 백테스팅 전략 수익률
     */
    val backtestYield: Double,
)