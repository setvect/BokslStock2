package com.setvect.bokslstock2.analysis.dm.model

import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType

/**
 * 듀얼모멘텀 백테스트 조건
 */
data class DmBacktestCondition(
    /**
     * 매매 기본 조건
     */
    val tradeCondition: TradeCondition,

    /**
     * 듀얼 모멘텀 대상 종목 코드
     * @see StockEntity
     */
    val stockCodes: List<String>,

    /**
     * 거래가 없을 때 살 종목, null 경우 현금 보유
     * @see StockEntity
     */
    val holdCode: String?,

    /**
     * 거래 주기
     */
    val periodType: PeriodType,

    /**
     * 기간 가중치. 가중치의 합이 100이 되야됨
     * <월, 가중치>
     */
    val timeWeight: Map<Int, Double>
) {
    /**
     * @return holdCode를 포함한 거래 대상 종목
     */
    fun listStock(): Set<String> {
        if (holdCode == null) {
            return HashSet(stockCodes)
        }
        val stocks = stockCodes.toMutableList()
        stocks.add(holdCode)
        return HashSet(stocks)
    }
}