package com.setvect.bokslstock2.bokslstock2.backtest.entity

import com.setvect.bokslstock2.bokslstock2.entity.BaseTimeEntity
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.SEQUENCE
import javax.persistence.Id
import javax.persistence.Table

/**
 * 주식 종목
 */
@Entity
@Table(name = "CA_STOCK")
class StockEntity(
    /**
     * 종목이름
     */
    @Column(name = "NAME", length = 100, nullable = false) val name: String,
    /**
     * 종목코드
     */
    @Column(name = "CODE", length = 20, nullable = false) val code: String
) : BaseTimeEntity() {

    /**
     * 일련번호
     */
    @Id
    @GeneratedValue(strategy = SEQUENCE)
    @Column(name = "STOCK_ID", nullable = false)
    var stockId: Long? = null
}