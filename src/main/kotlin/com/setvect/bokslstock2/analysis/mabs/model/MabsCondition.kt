package com.setvect.bokslstock2.analysis.mabs.model

import com.setvect.bokslstock2.analysis.common.entity.ConditionEntity
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType

/**
 * 이평선 돌파 백테스트 조건
 */
class MabsCondition(
    /**
     * 주식 종목
     */
    override val stock: StockEntity,

    /**
     * 매매 주기
     */
    val periodType: PeriodType,

    /**
     * 상승 매수률(양수)
     */
    val upBuyRate: Double,

    /**
     * 하락 매도률(양수)
     */
    val downSellRate: Double,

    /**
     * 단기 이동평균 기간
     */
    val shortPeriod: Int,

    /**
     * 장기 이동평균 기간
     */
    val longPeriod: Int,

    /**
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    val comment: String,
) : ConditionEntity{
     override var tradeList: List<MabsTrade> = ArrayList()
}