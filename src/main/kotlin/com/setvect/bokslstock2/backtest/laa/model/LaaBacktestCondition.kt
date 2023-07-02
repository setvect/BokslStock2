package com.setvect.bokslstock2.backtest.laa.model

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.backtest.rebalance.model.RebalanceBacktestCondition
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.util.DateRange

/**
 * 리벨런싱 백테스트 조건
 */
data class LaaBacktestCondition(
    /** 매매 기간 */
    var range: DateRange,

    /**  투자금액 */
    val cash: Double,

    /**
     * 고정자산 종류
     */
    val stockCodes: List<RebalanceBacktestCondition.TradeStock>,

    /**
     * 고정자산 리벨리싱
     */
    val rebalanceFacter: RebalanceBacktestCondition.RebalanceFacter,

    /**
     * LAA 변경 자산 비중 0.25 == 25%
     */
    val laaWeight: Double,

    /**
     * LAA 공격자산 종목
     */
    val offenseCode: StockCode,

    /**
     * LAA 방어자산 종목
     */
    val defenseCode: StockCode,

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
