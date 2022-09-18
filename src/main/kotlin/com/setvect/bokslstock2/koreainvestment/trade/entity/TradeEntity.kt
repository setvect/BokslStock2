package com.setvect.bokslstock2.koreainvestment.trade.entity

import com.setvect.bokslstock2.common.model.TradeType
import java.time.LocalDateTime
import javax.persistence.*

@Entity(name = "BA_TRADE")
@Table(indexes = [Index(name = "IDX_REG_DATE_AA", columnList = "REG_DATE DESC")])
class TradeEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "TRADE_SEQ")
    val tradeSeq: Long = 0L,

    /**
     * 계좌번호, MD5로 변환해서 저장
     */
    @Column(name = "ACCOUNT", length = 50, nullable = false)
    val account: String,

    /**
     * 종목 코드
     */
    @Column(name = "CODE", length = 20, nullable = false)
    val code: String,

    /**
     * 매수/매도
     */
    @Column(name = "TRADE_TYPE", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    val tradeType: TradeType,

    /**
     * 수량
     */
    @Column(name = "QTY", nullable = false)
    val qty: Int,

    /**
     * 거래 단가
     */
    @Column(name = "UNIT_PRICE", nullable = false)
    val unitPrice: Double,

    /**
     * 매도시 수익률, 매수일 경우 0
     * 소수로 표현, 1->100%, -0.02 -> -2%
     */
    @Column(name = "YIELD")
    val yield: Double,

    /**
     * 거래 시간
     */
    @Column(name = "REG_DATE", nullable = false)
    val regDate: LocalDateTime,
)