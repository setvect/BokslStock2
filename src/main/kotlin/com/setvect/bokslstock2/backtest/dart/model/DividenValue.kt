package com.setvect.bokslstock2.backtest.dart.model

/**
 * 년도별 손익계산서 항목 값
 */
data class DividenValue(
    // 종목코드
    val stockCode: String,
    // 년도
    val year: Int,
    // 회계 마감 기준
    val accountClose: AccountClose,
    // 주당액면가액: 주식 하나당 대표되는 금액
    val faceValuePerShare: Long,
    // 주당순이익: 회사의 순이익을 주식 수로 나눈 값
    val eps: Long,
    // 현금배당성향: 배당금을 순이익으로 나눈 값 (%로 표시)
    val dividendPropensity: Double,
    // 현금배당수익률: 주식 하나당 지급되는 현금배당금을 주가로 나눈 값 (%로 표시)
    // 실제 계산과 다른 경우가 있음
    // 예를 들어 삼성전자 2022년 12월 종가: 55,300원, DPS: 1,444원, 배당 수익률: 2.61%가 되어야 하는데 DART 제공값은 2.5%임
    val dividendYield: Double,
    // 현금배당금: 주식 하나당 지급되는 현금 금액
    val dps: Long
)