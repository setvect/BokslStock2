package com.setvect.bokslstock2.analysis.rb.entity

import com.setvect.bokslstock2.analysis.common.model.TradeType
import com.setvect.bokslstock2.common.entity.ConditionEntity
import com.setvect.bokslstock2.common.entity.TradeEntity
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType.STRING
import javax.persistence.Enumerated
import javax.persistence.FetchType.LAZY
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.AUTO
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * 리벨러싱 백테스트 매매 건별 정보
 */
@Entity(name = "VB_RB_TRADE")
@Table(
    indexes = [
        Index(
            name = "VB_RB_TRADE_TRADE_DATE_INDEX",
            columnList = "TRADE_DATE"
        )]
)
class RbTradeEntity(
    /**
     * 매매 조건 일련번호
     */
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "Rb_CONDITION_SEQ")
    val rbConditionEntity: RbConditionEntity,

    /**
     * 매수/매도
     */
    @Column(name = "TRADE_TYPE", length = 20, nullable = false)
    @Enumerated(STRING)
    override val tradeType: TradeType,

    /**
     * 매도시 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     * 매수는 0으로 표현
     */
    @Column(name = "YIELD")
    override val yield: Double,

    /**
     * 거래 단가
     * - 매수일 경우 매수 단가
     * - 매도일 경우 매도 단가
     */
    @Column(name = "UNIT_PRICE", nullable = false)
    override val unitPrice: Double,

    /**
     * 거래시간
     */
    @Column(name = "TRADE_DATE", nullable = false)
    override val tradeDate: LocalDateTime,
) : TradeEntity {
    @Id
    @GeneratedValue(strategy = AUTO)
    @Column(name = "TRADE_SEQ")
    val tradeSeq = 0L

    override fun tradeId(): Long {
        return tradeSeq
    }

    override fun getConditionEntity(): ConditionEntity {
        return rbConditionEntity
    }
}