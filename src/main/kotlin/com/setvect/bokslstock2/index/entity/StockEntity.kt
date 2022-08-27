package com.setvect.bokslstock2.index.entity

import com.setvect.bokslstock2.analysis.common.entity.BaseTimeEntity
import com.setvect.bokslstock2.analysis.common.model.StockCode
import javax.persistence.*
import javax.persistence.GenerationType.SEQUENCE

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
    @Column(name = "STOCK_SEQ", nullable = false)
    var stockSeq: Long? = null

    /**
     * 시세 정보
     */
    @OneToMany(mappedBy = "stock")
    @OrderBy("candleDateTime asc")
    val candleList: List<CandleEntity> = ArrayList()


    fun getNameCode(): String {
        return "${name}(${code})"
    }

    fun convertStockCode(): StockCode {
        return StockCode.findByCode(code)
    }
}