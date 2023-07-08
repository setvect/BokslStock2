package com.setvect.bokslstock2.backtest.stoploss.model

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.util.DateRange
import java.time.YearMonth

class StopLoosBacktestCondition(
    /** 매매 시작 기간*/
    var from: YearMonth,
    /** 매매 종료 기간*/
    var to: YearMonth,

    /** 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자 */
    val investRatio: Double,

    /**  투자금액 */
    val cash: Double,

    /** 주식 종목 */
    val stockCode: StockCode,

    /** 평균 변동성을 구한 기간 */
    val averageMonthCount: Int,

    /** 평균치에 대한 손절 비율 */
    val stopLossRate: Double,
) {
    fun getRange(): DateRange {
        return DateRange(from.atDay(1), to.atEndOfMonth())
    }
}