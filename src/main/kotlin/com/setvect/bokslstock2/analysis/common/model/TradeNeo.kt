package com.setvect.bokslstock2.analysis.common.model

import com.setvect.bokslstock2.common.model.TradeType
import java.time.LocalDateTime

/**
 * Trade 클래스 대체
 * 순수하게 매매에 관한 정보만 담고 있음.
 * 예를 들어 매도 수익, 수수료는 없음. 이건 계산으로 얻을 수 있기 때문에 제외했음.
 * 다 정리되면 TradeNeo -> Trade로 변경
 */
data class TradeNeo(
    /** 매매 종목 */
    val stockCode: StockCode,
    /** 매매 종류 */
    val tradeType: TradeType,
    /**
     * 거래 단가
     * - 매수일 경우 매수 단가
     * - 매도일 경우 매도 단가
     */
    val price: Double,
    /** 매매 수량 */
    val qty: Int,
    /**  거래시간 */
    val tradeDate: LocalDateTime,
    /**
     * 거래 메모
     * 적용 조건 이름 등 저장
     */
    val memo: String = ""
)