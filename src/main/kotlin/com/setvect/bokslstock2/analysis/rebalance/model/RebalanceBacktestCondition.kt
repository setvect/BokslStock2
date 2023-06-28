package com.setvect.bokslstock2.analysis.rebalance.model

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.util.DateRange

/**
 * 리벨런싱 백테스트 조건
 */
data class RebalanceBacktestCondition(
    /** 매매 기간 */
    val range: DateRange,

    /** 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자 */
    val investRatio: Double,

    /**  투자금액 */
    val cash: Double,

    /**
     * 대상 종목 코드
     */
    val stockCodes: List<TradeStock>,

    /**
     * 리벨런싱 조건
     */
    val rebalanceFacter: RebalanceFacter

) {
    data class TradeStock(
        /**
         * 매매 코드
         * @see StockEntity
         */
        val stockCode: StockCode,
        /**
         * 비중
         */
        val weight: Int
    )

    /**
     * 리벨런싱 조건
     */
    data class RebalanceFacter(
        /**
         * 리밸런싱 주기
         */
        val periodType: PeriodType,

        /**
         * 리벨런싱 임계점
         * 0.1 = 10%
         */
        val threshold: Double
    )

    fun listStock(): List<StockCode> {
        return stockCodes.map { it.stockCode }
    }

    fun sumWeight(): Int {
        return stockCodes.sumOf { it.weight }
    }
}
