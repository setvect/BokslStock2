package com.setvect.bokslstock2.analysis.common.model

import com.setvect.bokslstock2.util.DateRange

/**
 * 매매조건 조건
 */
data class TradeCondition(
    /**
     * 분석 대상 기간
     */
    var range: DateRange,
    /**
     * 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자
     */
    val investRatio: Double,
    /**
     * 최초 투자금액
     */
    val cash: Double,
    /**
     * 매수 수수료
     */
    val feeBuy: Double,
    /**
     * 매도 수수료
     */
    val feeSell: Double,
    /**
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    val comment: String,

    /**
     * 밴치마크 종목코드
     */
    val benchmark: List<StockCode> = listOf()
)
