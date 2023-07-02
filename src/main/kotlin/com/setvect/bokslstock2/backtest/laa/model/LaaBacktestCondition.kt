package com.setvect.bokslstock2.backtest.laa.model

import com.setvect.bokslstock2.backtest.common.model.StockCode
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
    val stockCodes: List<TradeStock>,

    /**
     * 고정자산 리벨리싱
     */
    val rebalanceFacter: RebalanceFacter,

    /**
     * LAA 변경 자산 비중 0.25 == 25%
     */
    val laaWeight: Int,

    /**
     * 공경자산/방어자산 평가를 위한 테스트 종목 테스트 기준
     */
    val testStockCode: StockCode,

    /**
     * 공경자산/방어자산 평가를 위한 테스트 종목 이동평균 기간
     */
    val testMa: Int,

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

    /**
     * @return 사용하는 전체 종목 코드
     */
    fun listStock(): List<StockCode> {
        val codes = stockCodes.map { it.stockCode }.toMutableList()
        codes.add(offenseCode)
        codes.add(defenseCode)
        return codes
    }

    fun sumWeight(): Int {
        return stockCodes.sumOf { it.weight }
    }
}
