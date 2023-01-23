package com.setvect.bokslstock2.analysis.vbs.entity

import com.setvect.bokslstock2.analysis.common.entity.BaseTimeEntity
import com.setvect.bokslstock2.analysis.common.entity.ConditionEntity
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType
import org.hibernate.annotations.Type
import javax.persistence.*
import javax.persistence.EnumType.STRING
import javax.persistence.GenerationType.AUTO

/**
 * 변동성돌파 전략 조건
 */
@Entity(name = "GA_VBS_CONDITION")
class VbsConditionEntity(
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
     * 변동성 돌파 비율
     */
    @Column(name = "K_RATE", nullable = false)
    val kRate: Double,

    /**
     * 매매 이동평균 상단
     * 1 이하는 이동평균 의미 없음
     * 현재 매도가가 이동평균 이상인경우 매도
     */
    @Column(name = "MA_PERIOD", nullable = false)
    val maPeriod: Int,

    /**
     * 호가 단위, ETF는 5임
     */
    @Column(name = "UNIT_ASK_PRICE", nullable = false)
    val unitAskPrice: Double,

    /**
     * false: 매수 상태에서 다음날 시가 매도
     * true: 매수 상태에서 오늘 시가가 전일 종가보다 높으면 매도하지 않고 다음날로 넘김
     * TODO 의미 없는 조건 같다. 삭제 필요
     */
    @Column(name = "GAP_RISEN_SKIP", nullable = false, length = 1)
    @Type(type = "yes_no")
    @Deprecated("의미 없는 조건 같다. 삭제 필요")
    val gapRisenSkip: Boolean,

    /**
     * false: 매도 일에 매수 조건이 되면 매수
     * true: 오늘 매도 하면 조건이 만족해도 그날 매수 안함
     */
    @Column(name = "ONLY_ONE_DAY_TRADE", nullable = false, length = 1)
    @Type(type = "yes_no")
    val onlyOneDayTrade: Boolean,

    /**
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    @Column(name = "COMMENT", length = 100)
    val comment: String?,

    /**
     * 갭 상승 시 5분 마다 시세 체크, 직전 5분봉 하락 반전 시 매도
     */
    @Transient
    val stayGapRise: Boolean
) : ConditionEntity, BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = AUTO)
    @Column(name = "CONDITION_SEQ")
    override val conditionSeq = 0L

    @OneToMany(mappedBy = "vbsConditionEntity")
    @OrderBy("tradeDate ASC")
    override var tradeList: List<VbsTradeEntity> = ArrayList()
}