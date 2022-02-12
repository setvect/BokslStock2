package com.setvect.bokslstock2.analysis.entity.vbs

import com.setvect.bokslstock2.common.entity.BaseTimeEntity
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

/**
 * 변동성돌파 전략 조건
 */
@Entity(name = "WA_VBS_CONDITION")
class VbsConditionEntity(
    /**
     * 주식 종목
     */
    @JoinColumn(name = "STOCK_SEQ")
    @ManyToOne
    val stock: StockEntity,

    /**
     * 매매 주기
     */
    @Column(name = "PERIOD_TYPE", length = 20, nullable = false)
    @Enumerated(STRING)
    val periodType: PeriodType,

    /**
     * 변동성 돌파 비율
     */
    @Column(name = "K_RATE", nullable = false)
    val kRate: Double,

    /**
     * 매매 이동평균 상단
     * 1 이하는 이동평균 의미 없음
     * 현재 매도가가 이동평균 이상인경우 매도
     */
    @Column(name = "MA", nullable = false)
    val maPeriod: Int,

    /**
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    @Column(name = "COMMENT", length = 100)
    val comment: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = AUTO)
    @Column(name = "VBS_CONDITION_SEQ")
    val vbsConditionSeq = 0
}