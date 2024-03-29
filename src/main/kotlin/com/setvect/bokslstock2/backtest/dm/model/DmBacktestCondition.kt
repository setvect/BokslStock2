package com.setvect.bokslstock2.backtest.dm.model

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.util.DateRange

/**
 * 듀얼모멘텀 백테스트 조건
 */
data class DmBacktestCondition(
    /** 매매 기간 */
    val range: DateRange,

    /** 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자 */
    val investRatio: Double,

    /**  투자금액 */
    val cash: Double,

    /**
     * 듀얼 모멘텀 대상 종목 코드
     * @see StockEntity
     */
    val stockCodes: List<StockCode>,

    /**
     * 거래가 없을 때 살 종목, null 경우 현금 보유
     * @see StockEntity
     */
    val holdCode: StockCode?,

    /**
     * 거래 주기
     */
    val periodType: PeriodType,

    /**
     * 기간 가중치. 가중치의 합이 100이 되야됨
     * <월, 가중치>
     */
    val timeWeight: Map<Int, Double>,

    /**
     * true면 백테스트 기간 종료 시점에 보유 종목 매도
     */
    val endSell: Boolean,
) {
    /**
     * @return holdCode를 포함한 거래 대상 종목
     */
    fun listStock(): List<StockCode> {
        if (holdCode == null) {
            return stockCodes.toList()
        }
        val stockCodes = stockCodes.toMutableList()
        stockCodes.add(holdCode)
        return stockCodes
    }


    /**
     * @return 모맨텀 적용할 때 가장 이전의 월
     */
    // 주석을 썼는데 표현이 잘 안된다 ...
    fun maxWeightMonth(): Int {
        return timeWeight.entries.maxOf { it.key }
    }
}