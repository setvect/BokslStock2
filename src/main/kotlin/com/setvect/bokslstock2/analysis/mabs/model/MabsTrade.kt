package com.setvect.bokslstock2.analysis.mabs.model

import com.setvect.bokslstock2.common.model.TradeType
import java.time.LocalDateTime

/**
 * 이평선 돌파 백테스트 매매 건별 정보
 */
class MabsTrade(

    /**
     * 매매 조건 일련번호
     */
    val mabsCondition: MabsCondition,

    /**
     * 매수/매도
     */
    val tradeType: TradeType,

    /**
     * 최고 수익률
     */
    val highYield: Double,

    /**
     * 최저 수익률
     */
    val lowYield: Double,

    /**
     * 단기 이동평균 가격
     */
    val maShort: Double,

    /**
     * 장기 이동평균 가격
     */
    val maLong: Double,

    /**
     * 매도시 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     * 매수는 0으로 표현
     */
    val yield: Double,

    /**
     * 거래 단가
     * - 매수일 경우 매수 단가
     * - 매도일 경우 매도 단가
     */
    val unitPrice: Double,

    /**
     * 거래시간
     */
    val tradeDate: LocalDateTime,
) {
    val tradeSeq = 0L

    fun getConditionEntity(): MabsCondition {
        return mabsCondition
    }
}