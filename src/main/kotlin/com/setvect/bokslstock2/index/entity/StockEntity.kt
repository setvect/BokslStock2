package com.setvect.bokslstock2.index.entity

import com.setvect.bokslstock2.common.entity.BaseTimeEntity
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.SEQUENCE
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.OrderBy
import javax.persistence.Table
import javax.persistence.UniqueConstraint

/**
 * 주식 종목
 */
@Entity
@Table(
    name = "CA_STOCK", uniqueConstraints = [
        UniqueConstraint(name = "UC_STOCK_ENTITY_CODE", columnNames = ["CODE"])
    ]
)
class StockEntity(
    /**
     * 종목이름
     */
    @Column(name = "NAME", length = 100, nullable = false)
    val name: String,
    /**
     * 종목코드
     */
    @Column(name = "CODE", length = 20, nullable = false)
    val code: String
) : BaseTimeEntity() {

    /**
     * 일련번호
     */
    @Id
    @GeneratedValue(strategy = SEQUENCE)
    @Column(name = "STOCK_ID", nullable = false)
    var stockSeq: Long? = null

    /**
     * 시세 정보
     */
    @OneToMany(mappedBy = "stock")
    @OrderBy("candleDateTime asc")
    val candleList: List<CandleEntity> = ArrayList()
}