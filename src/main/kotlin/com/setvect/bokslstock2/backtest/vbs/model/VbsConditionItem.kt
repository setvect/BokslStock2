package com.setvect.bokslstock2.backtest.vbs.model

import com.setvect.bokslstock2.backtest.common.model.StockCode

data class VbsConditionItem(
    /**
     * 주식 종목
     */
    val stockCode: StockCode,

    /**
     * 변동성 돌파 비율
     */
    val kRate: Double,
    /**
     * 갭 상승 시 5분 마다 시세 체크, 직전 5분봉 하락 반전 시 매도
     */
    val stayGapRise: Boolean,
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
     * 조건에 대한 설명. 리포트에서 사용하기 위함
     */
    val comment: String?,

    /**
     * 매수가능 금액에서 투자 비율
     * 0 초과 1이하 값
     */
    val investmentRatio: Double,
)