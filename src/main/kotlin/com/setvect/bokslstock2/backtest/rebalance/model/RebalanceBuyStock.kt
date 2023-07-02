package com.setvect.bokslstock2.backtest.rebalance.model

import com.setvect.bokslstock2.index.dto.CandleDto

/**
 * @property candle 매수 종목 정보
 * @property qty 매수 수량
 * @property weight 해당 종목의 전체 투자대비 비중(%)
 */
data class RebalanceBuyStock(val candle: CandleDto, val qty: Int, val weight: Int) {

    fun getEvalPriceOpen(): Double {
        return candle.openPrice * qty
    }

    fun getEvalPriceClose(): Double {
        return candle.closePrice * qty
    }

    /**
     * 종가기준 전체 [totalPrice] 대비 비중(%)
     */
    fun realWeight(totalPrice: Double): Double {
        return getEvalPriceClose() / totalPrice * 100
    }
}