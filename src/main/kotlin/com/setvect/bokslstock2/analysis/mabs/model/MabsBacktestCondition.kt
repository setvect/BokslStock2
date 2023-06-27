package com.setvect.bokslstock2.analysis.mabs.model

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.util.DateRange

/**
 * 이평선 돌파 백테스트 조건
 */
class MabsBacktestCondition(
    /** 매매 기간 */
    val range: DateRange,

    /** 총 현금을 기준으로 투자 비율. 1은 전액, 0.5은 50% 투자 */
    val investRatio: Double,

    /**  투자금액 */
    val cash: Double,

    /**
     * 주식 종목
     */
    val stockCode: StockCode,

    /**
     * 매매 주기
     */
    val periodType: PeriodType,

    /**
     * 상승 매수률(양수)
     */
    val upBuyRate: Double,

    /**
     * 하락 매도률(양수)
     */
    val downSellRate: Double,

    /**
     * 단기 이동평균 기간
     */
    val shortPeriod: Int,

    /**
     * 장기 이동평균 기간
     */
    val longPeriod: Int,

    /**
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    val comment: String,
)