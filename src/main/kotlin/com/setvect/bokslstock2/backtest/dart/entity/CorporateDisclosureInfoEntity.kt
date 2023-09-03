package com.setvect.bokslstock2.backtest.dart.entity

import com.setvect.bokslstock2.backtest.common.entity.BaseTimeEntity
import com.setvect.bokslstock2.backtest.dart.model.AccountClose
import com.setvect.bokslstock2.backtest.dart.model.FinancialDetailMetric
import javax.persistence.*
import javax.persistence.GenerationType.SEQUENCE

/**
 * 기업공시정보
 */
@Entity
@Table(
    name = "CC_CORPORATE_DISCLOSURE_INFO",
    uniqueConstraints = [
        UniqueConstraint(
            name = "UK_CODE_FINANCIALMETRIC_YEAR",
            columnNames = ["CODE", "FINANCIAL_METRIC_TYPE", "YEAR"]
        )
    ]
)
class CorporateDisclosureInfoEntity(
    // 종목코드
    @Column(name = "CODE", length = 20, nullable = false)
    var code: String,

    // 재무제표 항목 유형
    @Column(name = "FINANCIAL_METRIC_TYPE", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    var financialDetailMetric: FinancialDetailMetric,

    // 년도()
    @Column(name = "YEAR", nullable = false)
    var year: Int,

    // 회계 마감 기준
    @Column(name = "ACCOUNT_CLOSE", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    var accountClose: AccountClose,

    // 재무제표 항목명: 매출액, 영업이익, 당기순이익, ...
    @Column(name = "ITEM_NAME", length = 50, nullable = false)
    var itemName: String,

    // 1분기 값
    @Column(name = "Q1_VALUE", nullable = false)
    var q1Value: Long,

    // 2분기 값
    @Column(name = "Q2_VALUE", nullable = false)
    var q2Value: Long,

    // 3분기 값
    @Column(name = "Q3_VALUE", nullable = false)
    var q3Value: Long,

    // 4분기 값
    @Column(name = "Q4_VALUE", nullable = false)
    var q4Value: Long

) : BaseTimeEntity() {
    /**
     * 일련번호
     */
    @Id
    @GeneratedValue(strategy = SEQUENCE)
    @Column(name = "CORPORATE_DISCLOSURE_INFO_SEQ", nullable = false)
    var corporateDisclosureInfoSeq: Long? = null
}