package com.setvect.bokslstock2.backtest.dart.model

import com.setvect.bokslstock2.util.NumberUtil

/**
 * 년도별 손익계산서 항목 값
 *
 * 회계연도 기준 아님
 */
data class FinancialItemValue(
    val stockCode: String, // 종목코드
    val year: Int, // 년도()
    // 회계 마감 기준
    val accountClose: AccountClose,
    val itemName: String, // 손액계산서 항목이름: 매출액, 영업이익, 당기순이익, ...
    val q1Value: Long, // 1분기 값
    val q2Value: Long, // 2분기 값
    val q3Value: Long, // 3분기 값
    val q4Value: Long // 4분기 값
) {
    override fun toString(): String {
        return "FinancialItemValue(stockCode='$stockCode', year=$year, accountClose=$accountClose, accountName='$itemName', " +
                "q1Value=${NumberUtil.comma(q1Value)}, q2Value=${NumberUtil.comma(q2Value)}, " +
                "q3Value=${NumberUtil.comma(q3Value)}, q4Value=${NumberUtil.comma(q4Value)})"
    }
}