package com.setvect.bokslstock2.analysis.common.model

abstract class AnalysisCondition {
    /**
     * 분석 조건
     */
    abstract val tradeConditionList: List<CommonCondition>

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
