package com.setvect.bokslstock2.analysis.mabs.model

import com.setvect.bokslstock2.analysis.common.model.PreTrade
import com.setvect.bokslstock2.analysis.common.model.Stock
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.analysis.mabs.entity.MabsConditionEntity

/**
 * 이동평균돌파 백테스트
 */
data class MabsAnalysisCondition(
    /**
     * 분석 조건
     */
    val tradeConditionList: List<MabsConditionEntity>,

    /**
     * 매매 기본 조건
     */
    val basic: TradeCondition,
) {

    val conditionList: List<MabsConditionEntity>
        get() = tradeConditionList

    fun getStockCodes(): List<String> {
        return tradeConditionList.map { it.stock.code }.toList()
    }
    
    fun getPreTradeBundles(): List<List<PreTrade>> {
        // TODO 중복 제거
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