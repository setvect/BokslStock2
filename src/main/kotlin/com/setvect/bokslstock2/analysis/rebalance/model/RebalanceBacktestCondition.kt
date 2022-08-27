package com.setvect.bokslstock2.analysis.rebalance.model

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType

/**
 * 리벨런싱 백테스트 조건
 */
data class RebalanceBacktestCondition(
    /**
     * 매매 기본 조건
     */
    val tradeCondition: TradeCondition,

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
