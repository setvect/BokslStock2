package com.setvect.bokslstock2.analysis.rb.entity

import com.setvect.bokslstock2.analysis.common.entity.BaseTimeEntity
import com.setvect.bokslstock2.analysis.common.entity.ConditionEntity
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType
import javax.persistence.*
import javax.persistence.EnumType.STRING
import javax.persistence.GenerationType.AUTO

/**
 * 리벨런싱 전략 조건
 */
@Entity(name = "FA_RB_CONDITION")
@Deprecated("삭제할 백테스트")
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
    @Column(name = "CONDITION_SEQ")
    override val conditionSeq = 0L

    @OneToMany(mappedBy = "rbConditionEntity")
    @OrderBy("tradeDate ASC")
    override var tradeList: List<RbTradeEntity> = ArrayList()
}