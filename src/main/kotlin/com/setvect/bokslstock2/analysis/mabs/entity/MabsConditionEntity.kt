package com.setvect.bokslstock2.analysis.mabs.entity

import com.setvect.bokslstock2.analysis.common.entity.BaseTimeEntity
import com.setvect.bokslstock2.analysis.common.entity.ConditionEntity
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType.STRING
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.AUTO
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OrderBy

/**
 * 이평선 돌파 백테스트 조건
 */
@Entity(name = "HA_MABS_CONDITION")
class MabsConditionEntity(
    /**
     * 주식 종목
     */
    @JoinColumn(name = "STOCK_SEQ")
    @ManyToOne
    override val stock: StockEntity,

    /**
     * 매매 주기
     */
    @Column(name = "PERIOD_TYPE", length = 20, nullable = false)
    @Enumerated(STRING)
    val periodType: PeriodType,

    /**
     * 상승 매수률(양수)
     */
    @Column(name = "UP_BUY_RATE", nullable = false)
    val upBuyRate: Double,

    /**
     * 하락 매도률(양수)
     */
    @Column(name = "DOWN_BUY_RATE", nullable = false)
    val downSellRate: Double,

    /**
     * 단기 이동평균 기간
     */
    @Column(name = "SHORT_PERIOD", nullable = false)
    val shortPeriod: Int,

    /**
     * 장기 이동평균 기간
     */
    @Column(name = "LONG_PERIOD", nullable = false)
    val longPeriod: Int,

    /**
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    @Column(name = "COMMENT", length = 100)
    val comment: String,
) : ConditionEntity, BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = AUTO)
    @Column(name = "CONDITION_SEQ")
    val conditionSeq = 0L

    @OneToMany(mappedBy = "mabsConditionEntity")
    @OrderBy("tradeDate ASC")
    override var tradeList: List<MabsTradeEntity> = ArrayList()
}