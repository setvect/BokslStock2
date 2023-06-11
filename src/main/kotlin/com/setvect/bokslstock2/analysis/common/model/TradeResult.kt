package com.setvect.bokslstock2.analysis.common.model

import com.setvect.bokslstock2.common.model.TradeType
import java.time.LocalDateTime

/**
 * 건별 매매 결과
 * 리포트 생성 시 사용
 */
data class TradeResult(
    /** 매매 종목 */
    val stockCode: StockCode,

    val tradeType: TradeType,
    /**
     * 매도시 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     * 매수는 0으로 표현
     */
    val yieldRate: Double,
    /**
     * 거래 단가
     * - 매수일 경우 매수 단가
     * - 매도일 경우 매도 단가
     */
    val price: Double,
    /**매수 수량 */
    val qty: Int,
    /**매매 수수료 */
    val feePrice: Double,
    /**투자 수익 금액 */
    val gains: Double,
    /**거래시간 */
    val tradeDate: LocalDateTime,
    /**해당 거래 후 현금 */
    val cash: Double,
    /** 해당 매매 완료 후 계좌 전체 주식 구입 가격 */
    val purchasePrice: Double,
    /** 해당 매매 완료 후 계좌 전체 주식 평가 금액 */
    val stockEvalPrice: Double,
    /** 종목별 주식잔고 Key: StockCode, Value: StockAccount */
    val stockAccount: Map<StockCode, StockAccount>,
    /** 수익비. 1일 기준 */
    val profitRate: Double,
    /** 거래 메모. 적용 조건 이름 등 저장 */
    val memo: String = ""
) {
    /** 주식 평가금 + 현금 */
    fun getEvalPrice(): Double {
        return cash + stockEvalPrice
    }
}
