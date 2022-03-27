package com.setvect.bokslstock2.analysis.vbs.model

import com.setvect.bokslstock2.analysis.common.model.PreTrade
import com.setvect.bokslstock2.analysis.common.model.Stock
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.vbs.entity.VbsConditionEntity
import com.setvect.bokslstock2.common.entity.ConditionEntity

/**
 * 변동성돌파 백테스트
 */
data class VbsAnalysisCondition(
    /**
     * 분석 조건
     */
    val tradeConditionList: List<VbsConditionEntity>,

    /**
     * 매매 기본 조건
     */
    val basic: TradeCondition,
) {
    val conditionList: List<ConditionEntity>
        get() = tradeConditionList

    fun getStockCodes(): List<String> {
        return tradeConditionList.map { it.stock.code }.toList()
    }

    fun getPreTradeBundles(): List<List<PreTrade>> {
        return tradeConditionList
            .map { vc ->
                vc.tradeList.map {
                    PreTrade(
                        stock = Stock(vc.stock.name, vc.stock.code),
                        tradeDate = it.tradeDate,
                        tradeType = it.tradeType,
                        unitPrice = it.unitPrice,
                        yield = it.yield
                    )
                }
            }
    }
}