package com.setvect.bokslstock2.analysis.vbs.model

import com.setvect.bokslstock2.analysis.common.model.CommonCondition
import com.setvect.bokslstock2.analysis.common.entity.TradeEntity
import com.setvect.bokslstock2.common.model.TradeType
import java.time.LocalDateTime

/**
 * 이평선 돌파 백테스트 매매 건별 정보
 */
class VbsTrade(
    /**
     * 매매 조건 일련번호
     */
    val vbsCondition: VbsCondition,

    /**
     * 매수/매도
     */
    override val tradeType: TradeType,

    /**
     * 매매 시 이동평균 가격
     */
    val maPrice: Double,

    /**
     * 매도시 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     * 매수는 0으로 표현
     */
    override val yield: Double,

    /**
     * 거래 단가
     * - 매수일 경우 매수 단가
     * - 매도일 경우 매도 단가
     */
    override val unitPrice: Double,

    /**
     * 거래시간
     */
    override val tradeDate: LocalDateTime,
) : TradeEntity {
    val tradeSeq = 0L
    override fun getConditionEntity(): CommonCondition {
        return vbsCondition
    }
}