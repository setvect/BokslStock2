package com.setvect.bokslstock2.analysis.common.model

import java.time.LocalDateTime

/**
 * Buy&Hold 및 전략 평가금액
 */
data class EvaluationAmountItem(
    /**
     * 기준 날짜
     */
    val baseDate: LocalDateTime,
    /**
     * buy&hold 평가금
     */
    val buyHoldAmount: Long,
    /**
     * 백테스팅 전략 평가금
     */
    val backtestAmount: Long,
)