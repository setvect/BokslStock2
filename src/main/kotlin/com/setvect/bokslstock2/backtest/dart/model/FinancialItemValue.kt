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
    val itemName: String, // 재무제표 항목명: 매출액, 영업이익, 당기순이익, ...
    val q1Value: Long, // 1분기 값
    val q2Value: Long, // 2분기 값
    val q3Value: Long, // 3분기 값
    val q4Value: Long // 4분기 값
) {
    fun exist(): Boolean {
        return q1Value != 0L || q2Value != 0L || q3Value != 0L || q4Value != 0L
    }

    fun getSum(): Long {
        return getSum(true, true, true, true)
    }

    /**
     * 특정 분기에 값이 있다는 보장으로 합계를 얻어야됨. 수집이 안된 값이 있어 0이 되어 계산오류가 발생하는걸 예방
     */
    fun getSum(existQ1: Boolean, existQ2: Boolean, existQ3: Boolean, existQ4: Boolean): Long {
        if (existQ1 && q1Value == 0L) {
            throw RuntimeException("1분기 값이 0이면 안됨")
        }
        if (!existQ1 && q1Value != 0L) {
            throw RuntimeException("1분기 값이 0이 아니면 안됨")
        }

        if (existQ2 && q2Value == 0L) {
            throw RuntimeException("2분기 값이 0이면 안됨")
        }
        if (!existQ2 && q2Value != 0L) {
            throw RuntimeException("2분기 값이 0이 아니면 안됨")
        }

        if (existQ3 && q3Value == 0L) {
            throw RuntimeException("3분기 값이 0이면 안됨")
        }
        if (!existQ3 && q3Value != 0L) {
            throw RuntimeException("3분기 값이 0이 아니면 안됨")
        }

        if (existQ4 && q4Value == 0L) {
            throw RuntimeException("4분기 값이 0이면 안됨")
        }
        if (!existQ4 && q4Value != 0L) {
            throw RuntimeException("4분기 값이 0이 아니면 안됨")
        }

        return q1Value + q2Value + q3Value + q4Value
    }

    override fun toString(): String {
        return "FinancialItemValue(stockCode='$stockCode', year=$year, accountClose=$accountClose, accountName='$itemName', " +
                "q1Value=${NumberUtil.comma(q1Value)}, q2Value=${NumberUtil.comma(q2Value)}, " +
                "q3Value=${NumberUtil.comma(q3Value)}, q4Value=${NumberUtil.comma(q4Value)})"
    }
}