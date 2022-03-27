package com.setvect.bokslstock2.analysis.common.model

import com.setvect.bokslstock2.analysis.common.entity.ConditionEntity

abstract class AnalysisCondition {
    /**
     * 분석 조건
     */
    abstract val tradeConditionList: List<ConditionEntity>

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
