package com.setvect.bokslstock2.analysis.mabs.entity

import com.setvect.bokslstock2.analysis.common.model.TradeType
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
 * 이평선 돌파 백테스트 매매 건별 정보
 */
@Entity(name = "HB_MABS_TRADE")
@Table(
    indexes = [
        Index(
            name = "HB_MABS_TRADE_TRADE_DATE_INDEX",
            columnList = "TRADE_DATE"
        )]
)
class MabsTradeEntity(

    /**
     * 매매 조건 일련번호
     */
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "CONDITION_SEQ")
    val mabsConditionEntity: MabsConditionEntity,

    /**
     * 매수/매도
     */
    @Column(name = "TRADE_TYPE", length = 20, nullable = false)
    @Enumerated(STRING)
    val tradeType: TradeType,

    /**
     * 최고 수익률
     */
    @Column(name = "HIGH_YIELD")
    val highYield: Double,

    /**
     * 최저 수익률
     */
    @Column(name = "LOW_YIELD")
    val lowYield: Double,

    /**
     * 단기 이동평균 가격
     */
    @Column(name = "MA_SHORT", nullable = false)
    val maShort: Double,

    /**
     * 장기 이동평균 가격
     */
    @Column(name = "MA_LONG", nullable = false)
    val maLong: Double,

    /**
     * 매도시 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     * 매수는 0으로 표현
     */
    @Column(name = "YIELD")
    val yield: Double,

    /**
     * 거래 단가
     * - 매수일 경우 매수 단가
     * - 매도일 경우 매도 단가
     */
    @Column(name = "UNIT_PRICE", nullable = false)
    val unitPrice: Double,

    /**
     * 거래시간
     */
    @Column(name = "TRADE_DATE", nullable = false)
    val tradeDate: LocalDateTime,
) {
    @Id
    @GeneratedValue(strategy = AUTO)
    @Column(name = "TRADE_SEQ")
    val tradeSeq = 0L

    fun tradeId(): Long {
        return tradeSeq
    }

    fun getConditionEntity(): MabsConditionEntity {
        return mabsConditionEntity
    }

}