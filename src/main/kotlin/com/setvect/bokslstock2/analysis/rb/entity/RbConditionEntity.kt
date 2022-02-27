package com.setvect.bokslstock2.analysis.rb.entity

import com.setvect.bokslstock2.common.entity.BaseTimeEntity
import com.setvect.bokslstock2.common.entity.ConditionEntity
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
 * 리벨런싱 전략 조건
 */
@Entity(name = "VA_RB_CONDITION")
class RbConditionEntity(
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
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    @Column(name = "COMMENT", length = 100)
    val comment: String?,
) : ConditionEntity, BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = AUTO)
    @Column(name = "RB_CONDITION_SEQ")
    val rbConditionSeq = 0L

    @OneToMany(mappedBy = "rbConditionEntity")
    @OrderBy("tradeDate ASC")
    override var tradeList: List<RbTradeEntity> = ArrayList()

    override fun getConditionId(): Long {
        return rbConditionSeq
    }
}