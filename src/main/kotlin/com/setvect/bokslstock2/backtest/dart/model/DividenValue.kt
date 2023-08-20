package com.setvect.bokslstock2.backtest.dart.model

import com.setvect.bokslstock2.util.NumberUtil

/**
 * 년도별 손익계산서 항목 값
 */
data class DividenValue(
    val stockCode: String, // 종목코드
    val year: Int, // 년도()
    // 주당액면가액: 주식 하나당 대표되는 금액
    val faceValuePerShare: Long,
    // 주당순이익: 회사의 순이익을 주식 수로 나눈 값
    val netProfitPerShare: Long,
    // 현금배당성향: 배당금을 순이익으로 나눈 값 (%로 표시)
    val cashDividendPropensity: Double,
    // 현금배당수익률: 주식 하나당 지급되는 현금배당금을 주가로 나눈 값 (%로 표시)
    val cashDividendYield: Double,
    // 현금배당금: 주식 하나당 지급되는 현금 금액
    val cashDividendAmount: Long
){
    override fun toString(): String {
        return "DividenValue(stockCode='$stockCode', year=$year, " +
                "faceValuePerShare=${NumberUtil.comma(faceValuePerShare)}, " +
                "netProfitPerShare=${NumberUtil.comma(netProfitPerShare)}, " +
                "cashDividendPropensity=${NumberUtil.comma(cashDividendPropensity)}, " +
                "cashDividendYield=${NumberUtil.comma(cashDividendYield)}, " +
                "cashDividendAmount=${NumberUtil.comma(cashDividendAmount)})"
    }
}