package com.setvect.bokslstock2.koreainvestment.trade.model.dto

import java.time.LocalDateTime

data class AssetPeriodHistoryDto(
    var account: String? = null,
    /**
     * 투자금액 합계
     */
    var investment: Double = 0.0,
    /**
     * 평가금액
     */
    var evlPrice: Double = 0.0,
    /**
     * 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     */
    var yield: Double = 0.0,
    /**
     * 보유 종목 갯수
     */
    var stockCount: Long = 0,
    /**
     * 평가 날짜
     */
    var regDate: LocalDateTime? = null,
)