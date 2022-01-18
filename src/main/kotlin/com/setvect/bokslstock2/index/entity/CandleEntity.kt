package com.setvect.bokslstock2.index.entity

import com.setvect.bokslstock2.index.model.PeriodType
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType.STRING
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.SEQUENCE
import javax.persistence.Id
import javax.persistence.Index
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * 시세 정보
 */
@Entity
@Table(name = "CB_CANDLE", indexes = [
    Index(name = "IDX_CANDLE_ENTITY", columnList = "CANDLE_DATE_TIME"),
    Index(name = "IDX_CANDLE_ENTITY_PERIOD_TYPE", columnList = "PERIOD_TYPE")
])
class CandleEntity(
    /**
     * 주식 종목
     */
    @JoinColumn(name = "STOCK_SEQ")
    @ManyToOne
    val stock: StockEntity,
    /**
     * 거래날짜
     */
    @Column(name = "CANDLE_DATE_TIME", nullable = false)
    val candleDateTime: LocalDateTime,
    /**
     * 캔들 유형
     */
    @Column(name = "PERIOD_TYPE", length = 20, nullable = false)
    @Enumerated(STRING)
    val periodType: PeriodType,
    /**
     * 시가
     */
    @Column(name = "OPEN_PRICE", nullable = false)
    val openPrice: Int,
    /**
     * 고가
     */
    @Column(name = "HIGH_PRICE", nullable = false)
    val highPrice: Int,
    /**
     * 저가
     */
    @Column(name = "LOW_PRICE", nullable = false)
    val lowPrice: Int,
    /**
     * 종가
     */
    @Column(name = "CLOSE_PRICE", nullable = false)
    val closePrice: Int,

    ) {

    /**
     * 일련번호
     */
    @Id
    @GeneratedValue(strategy = SEQUENCE)
    @Column(name = "CANDLE_SEQ", nullable = false)
    var candleSeq: Long? = null
}