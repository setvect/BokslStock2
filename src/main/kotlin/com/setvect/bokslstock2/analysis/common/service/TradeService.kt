package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.analysis.common.model.CommonTradeReportItem
import com.setvect.bokslstock2.analysis.common.model.TradeType
import com.setvect.bokslstock2.common.entity.AnalysisCondition
import com.setvect.bokslstock2.common.entity.AnalysisReportResult
import com.setvect.bokslstock2.common.entity.TradeEntity
import com.setvect.bokslstock2.common.entity.TradeReportItem
import kotlin.streams.toList

/**
 * 백테스팅 결과를 이용해 매매 분석
 */
@Deprecated("의존성이 너무 높음", ReplaceWith("BacktestTradeService"))
class TradeService<C : AnalysisCondition, E : TradeEntity, I : TradeReportItem, R : AnalysisReportResult>(
    private val reportMakerHelperService: ReportMakerHelperService,
    private val makerTrade: MakerTreadReportItem<E, I>,
    private val makerAnalysisReport: MakerAnalysisReportResult<C, I, R>,
) {
    interface MakerTreadReportItem<I : TradeEntity, O : TradeReportItem> {
        fun make(tradeEntity: I, common: CommonTradeReportItem): O
    }

    interface MakerAnalysisReportResult<C : AnalysisCondition, I : TradeReportItem, R : AnalysisReportResult> {
        fun make(analysisCondition: C, tradeEntity: List<I>, common: CommonAnalysisReportResult): R
    }

    /**
     * 매매 결과에 대한 통계적 분석을 함
     */
    fun analysis(
        tradeItemHistory: List<I>, condition: C
    ): R {
        // 날짜별로 Buy&Hold 및 투자전략 평가금액 얻기
        val evaluationAmountHistory = reportMakerHelperService.applyEvaluationAmount(tradeItemHistory, condition)

        val buyAndHoldYieldMdd: CommonAnalysisReportResult.TotalYield =
            ReportMakerHelperService.calculateTotalBuyAndHoldYield(evaluationAmountHistory, condition.basic.range)
        val buyAndHoldYieldCondition: Map<Long, CommonAnalysisReportResult.YieldMdd> =
            reportMakerHelperService.calculateBuyAndHoldYield(condition)

        val yieldTotal: CommonAnalysisReportResult.TotalYield =
            ReportMakerHelperService.calculateTotalYield(evaluationAmountHistory, condition.basic.range)
        val winningRate: Map<Long, CommonAnalysisReportResult.WinningRate> =
            ReportMakerHelperService.calculateCoinInvestment(tradeItemHistory)


        val common = CommonAnalysisReportResult(
            evaluationAmountHistory = evaluationAmountHistory,
            yieldTotal = yieldTotal,
            winningRateCondition = winningRate,
            buyHoldYieldCondition = buyAndHoldYieldCondition,
            buyHoldYieldTotal = buyAndHoldYieldMdd,
        )
        return makerAnalysisReport.make(condition, tradeItemHistory, common)
    }

    /**
     * 매매 백테스트
     */
    fun trade(condition: C): List<I> {
        val rangeInList: List<List<E>> =
            condition.conditionList.map { mainList ->
                mainList.tradeList.filter { condition.basic.range.isBetween(it.tradeDate) }
            }
                .toList() as List<List<E>>

        val tradeAllList: List<E> = rangeInList.flatMap { tradeList ->
            val subList: List<E> = tradeList.stream()
                // 첫 거래가 매도이면 삭제
                .skip(if (tradeList[0].tradeType == TradeType.SELL) 1 else 0)
                .toList()
            if (subList.size > 1) subList else emptyList()
        }.sortedWith(compareBy { it.tradeDate }).toList()

        if (tradeAllList.size < 2) {
            throw RuntimeException("매수/매도 기록이 없습니다.")
        }

        var cash = condition.basic.cash
        val tradeItemHistory = ArrayList<I>()
        val buyStock = HashMap<String, I>()
        tradeAllList.forEach { tradeItem ->
            if (tradeItem.tradeType == TradeType.BUY) {
                // 매도 처리
                val buyCash =
                    ReportMakerHelperService.getBuyCash(
                        buyStock.size,
                        cash,
                        condition.conditionList.size,
                        condition.basic.investRatio
                    )

                val buyQty: Int = (buyCash / tradeItem.unitPrice).toInt()
                val buyAmount = buyQty * tradeItem.unitPrice
                val feePrice = condition.basic.feeBuy * buyAmount
                cash -= buyAmount + feePrice
                val stockEvalPrice = buyStock.entries.map { it.value }
                    .sumOf { it.tradeEntity.unitPrice * it.common.qty } + buyQty * tradeItem.unitPrice
                val common = CommonTradeReportItem(
                    qty = buyQty,
                    cash = cash,
                    feePrice = feePrice,
                    gains = 0.0,
                    stockEvalPrice = stockEvalPrice
                )

                val tradeReportItem = makerTrade.make(tradeItem, common)
                tradeItemHistory.add(tradeReportItem)
                buyStock[tradeItem.getConditionEntity().stock.code] = tradeReportItem
            } else if (tradeItem.tradeType == TradeType.SELL) {
                // 매수 처리
                // 투자수익금 = 매수금액 * 수익률 - 수수료
                val buyTrade = buyStock[tradeItem.getConditionEntity().stock.code]
                    ?: throw RuntimeException("${tradeItem.getConditionEntity().stock.code} 매수 내역이 없습니다.")
                buyStock.remove(tradeItem.getConditionEntity().stock.code)
                val sellPrice = buyTrade.getBuyAmount() * (1 + tradeItem.yield)
                val sellFee = sellPrice * condition.basic.feeSell
                val gains = sellPrice - buyTrade.getBuyAmount()

                // 매매후 현금
                cash += sellPrice - sellFee

                val stockEvalPrice =
                    buyStock.entries.map { it.value }.sumOf { it.tradeEntity.unitPrice * it.common.qty }

                val common = CommonTradeReportItem(
                    qty = 0,
                    cash = cash,
                    feePrice = sellFee,
                    gains = gains,
                    stockEvalPrice = stockEvalPrice
                )
                val tradeReportItem = makerTrade.make(tradeItem, common)
                tradeItemHistory.add(tradeReportItem)
            }
        }
        return tradeItemHistory
    }
}