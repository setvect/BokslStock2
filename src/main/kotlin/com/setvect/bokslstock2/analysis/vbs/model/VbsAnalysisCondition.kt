package com.setvect.bokslstock2.analysis.vbs.model

import com.setvect.bokslstock2.analysis.common.model.PreTrade
import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.analysis.common.model.TradeCondition

/**
 * 변동성돌파 백테스트
 */
data class VbsAnalysisCondition(
    /**
     * 분석 조건
     */
    val tradeConditionList: List<VbsCondition>,

    /**
     * 매매 기본 조건
     */
    val basic: TradeCondition,
) {
    fun getStockCodes(): List<StockCode> {
        return tradeConditionList.map { StockCode.findByCode(it.stock.code) }.toList()
    }

    /**
     * 분석 조건에 해당하는 전략을 실행한 결과를 리턴
     */
    fun getPreTradeBundles(): List<List<PreTrade>> {
        return tradeConditionList
            .map { vc ->
                vc.tradeList.map {
                    PreTrade(
                        conditionName = vc.name,
                        stockCode = StockCode.findByCode(vc.stock.code),
                        tradeDate = it.tradeDate,
                        tradeType = it.tradeType,
                        unitPrice = it.unitPrice,
                        yield = it.yield
                    )
                }
            }
    }
}