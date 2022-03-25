package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.model.PreTrade
import com.setvect.bokslstock2.analysis.common.model.Trade
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.common.model.TradeType
import kotlin.streams.toList

/**
 * 백테스팅 결과를 이용해 매매 분석
 */
class BacktestTradeService(

) {
    /**
     * @return 수수료등 각종 조건을 적용시킨 매매 내역
     */
    fun trade(condition: TradeCondition, preTrades: List<PreTrade>): List<Trade> {
        return tradeBundle(condition, listOf(preTrades))
    }

    private fun tradeBundle(condition: TradeCondition, preTrades: List<List<PreTrade>>): List<Trade> {
        val bundleCount = preTrades.size

        val tradeAllList = filterPreTrade(preTrades, condition)

        if (tradeAllList.size < 2) {
            throw RuntimeException("매수/매도 기록이 없습니다.")
        }
        var cash = condition.cash
        val tradeItemHistory = mutableListOf<Trade>()
        // <종목코드, 직전 preTrade>
        val buyStock = HashMap<String, Trade>()

        tradeAllList.forEach { tradeItem ->
            if (tradeItem.tradeType == TradeType.BUY) {
                // 매수 처리
                val buyCash =
                    ReportMakerHelperService.getBuyCash(
                        buyStock.size,
                        cash,
                        bundleCount,
                        condition.investRatio
                    )

                val buyQty: Int = (buyCash / tradeItem.unitPrice).toInt()
                val buyAmount = buyQty * tradeItem.unitPrice
                val feePrice = condition.feeBuy * buyAmount
                cash -= buyAmount + feePrice
                val stockEvalPrice = buyStock.entries.map { it.value }
                    .sumOf { it.preTrade.unitPrice * it.qty } + buyQty * tradeItem.unitPrice

                val tradeReportItem = Trade(
                    preTrade = tradeItem,
                    qty = buyQty,
                    cash = cash,
                    feePrice = feePrice,
                    gains = 0.0,
                    stockEvalPrice = stockEvalPrice
                )
                tradeItemHistory.add(tradeReportItem)
                buyStock[tradeItem.stock.code] = tradeReportItem
            } else if (tradeItem.tradeType == TradeType.SELL) {
                // 매수 처리
                // 투자수익금 = 매수금액 * 수익률 - 수수료
                val buyTrade = buyStock[tradeItem.stock.code]
                    ?: throw RuntimeException("${tradeItem.stock.code} 매수 내역이 없습니다.")
                buyStock.remove(tradeItem.stock.code)
                val sellPrice = buyTrade.getBuyAmount() * (1 + tradeItem.yield)
                val sellFee = sellPrice * condition.feeSell
                val gains = sellPrice - buyTrade.getBuyAmount()

                // 매매후 현금
                cash += sellPrice - sellFee

                val stockEvalPrice =
                    buyStock.entries.map { it.value }.sumOf { it.preTrade.unitPrice * it.qty }

                val tradeReportItem = Trade(
                    preTrade = tradeItem,
                    qty = 0,
                    cash = cash,
                    feePrice = sellFee,
                    gains = gains,
                    stockEvalPrice = stockEvalPrice
                )
                tradeItemHistory.add(tradeReportItem)
            }
        }
        return tradeItemHistory
    }

    /**
     * @return 조건에 해당하는 매매 내역만 필터링
     */
    private fun filterPreTrade(
        preTrades: List<List<PreTrade>>,
        condition: TradeCondition
    ): List<PreTrade> {
        val matchPreTrades = preTrades
            .flatMap { p ->
                val rangeTrade = p.stream()
                    .filter { condition.range.isBetween(it.tradeDate) }
                    .toList()

                // 첫 거래가 매도이면 삭제
                val compactTrade = rangeTrade.stream()
                    .skip(if (p[0].tradeType == TradeType.SELL) 1 else 0)
                    .toList()
                if (compactTrade.size > 1) compactTrade else emptyList()
            }
            .sortedWith(compareBy { it.tradeDate }).toList()
        return matchPreTrades
    }
}