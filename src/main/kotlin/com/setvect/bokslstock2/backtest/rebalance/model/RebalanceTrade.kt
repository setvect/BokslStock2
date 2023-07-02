package com.setvect.bokslstock2.backtest.rebalance.model

import java.time.LocalDate
import kotlin.math.abs

/**
 * 리빌런싱 단위별 매매 내역
 * @property date 리벨런싱 날짜
 * @property rebalanceBuyStocks 매수 종목
 * @property cash 매수 후 남은 현금
 * @property rebalance 해당 매매 주기에서 리벨런싱을 했는지 여부
 */
data class RebalanceTrade(
    val date: LocalDate,
    val rebalanceBuyStocks: List<RebalanceBuyStock>,
    val cash: Double,
    val rebalance: Boolean
) {
    /**
     * @return 시초가 기준 현금포함 평가금액
     */
    fun getEvalPriceOpen(): Double {
        return rebalanceBuyStocks.sumOf { it.getEvalPriceOpen() } + cash
    }

    /**
     * @return 종가 기준 현금포함 평가금액
     */
    fun getEvalPriceClose(): Double {
        return rebalanceBuyStocks.sumOf { it.getEvalPriceClose() } + cash
    }

    /**
     * @return 종가 기준 평가금액
     */
    fun getEvalPriceCloseWithoutCash(): Double {
        return rebalanceBuyStocks.sumOf { it.getEvalPriceClose() }
    }

    /**
     * 예를들어 4종목 비중이 모두 25%이면 분산 값은 0
     *
     * A: 0.2
     * B: 0.3
     * C: 0.24
     * D: 0.26
     * 이면 분산값은 0.12
     *
     * @return 종가기준 편차(현금 포함하지 않음)
     */
    fun deviation(): Double {
        val sum = rebalanceBuyStocks.sumOf { it.getEvalPriceClose() }

        return rebalanceBuyStocks.sumOf {
            val d = it.weight / 100.0
            abs(d - (it.getEvalPriceClose() / sum))
        }
    }
}