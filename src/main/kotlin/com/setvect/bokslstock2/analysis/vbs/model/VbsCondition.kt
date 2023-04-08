package com.setvect.bokslstock2.analysis.vbs.model

import com.setvect.bokslstock2.analysis.common.entity.ConditionEntity
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.util.DateRange
import javax.persistence.*

/**
 * 변동성돌파 전략 조건
 */
class VbsCondition(
    /**
     * 주식 종목
     */
    override val stock: StockEntity,

    /**
     * 매매 기간
     */
    val range: DateRange,

    /**
     * 매매 주기
     */
    val periodType: PeriodType,

    /**
     * 변동성 돌파 비율
     */
    val kRate: Double,

    /**
     * 매매 이동평균 상단
     * 1 이하는 이동평균 의미 없음
     * 현재 매도가가 이동평균 이상인경우 매도
     */
    val maPeriod: Int,

    /**
     * 호가 단위, ETF는 5임
     */
    val unitAskPrice: Double,

    /**
     * false: 매수 상태에서 다음날 시가 매도
     * true: 매수 상태에서 오늘 시가가 전일 종가보다 높으면 매도하지 않고 다음날로 넘김
     * TODO 의미 없는 조건 같다. 삭제 필요
     */
    @Deprecated("의미 없는 조건 같다. 삭제 필요")
    val gapRisenSkip: Boolean,

    /**
     * false: 매도 일에 매수 조건이 되면 매수
     * true: 오늘 매도 하면 조건이 만족해도 그날 매수 안함
     */
    val onlyOneDayTrade: Boolean,

    /**
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    val comment: String?,

    /**
     * 갭 상승 시 5분 마다 시세 체크, 직전 5분봉 하락 반전 시 매도
     */
    val stayGapRise: Boolean
) : ConditionEntity {
    override val conditionSeq = 0L

    override var tradeList: List<VbsTrade> = ArrayList()
}