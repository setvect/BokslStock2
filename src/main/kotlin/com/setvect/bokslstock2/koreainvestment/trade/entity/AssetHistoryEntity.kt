package com.setvect.bokslstock2.koreainvestment.trade.entity

import java.time.LocalDateTime
import javax.persistence.*

/**
 * 자산 기록
 */
@Entity(name = "BB_ASSET_HISTORY")
@Table(indexes = [Index(name = "IDX_REG_DATE_BA", columnList = "REG_DATE DESC")])
class AssetHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ASSET_HISTORY_SEQ")
    val assetHistorySeq: Long = 0L,

    /**
     * 계좌번호, MD5로 변환해서 저장
     */
    @Column(name = "ACCOUNT", length = 50, nullable = false)
    val account: String,

    /**
     * 자산 종류
     * 자산 종류(예수금, 종목코드)
     * DEPOSIT, 005930, 069500
     */
    @Column(name = "ASSET_CODE", length = 20, nullable = false)
    val assetCode: String,

    /**
     * 투자금
     */
    @Column(name = "INVESTMENT", nullable = false)
    val investment: Double,

    /**
     * 수익률
     * 소수로 표현, 1->100%, -0.02 -> -2%
     */
    @Column(name = "YIELD")
    val yield: Double,

    /**
     * 자산 조회 시간
     * 한 계좌의 여러 자산을 조회 할 경우 동일한 시간을 보장함
     */
    @Column(name = "REG_DATE", nullable = false)
    val regDate: LocalDateTime,
) {

    companion object {
        const val DEPOSIT = "DEPOSIT"
    }

    /**
     * @return 평가금
     */
    val appraisal: Double
        get() = investment + investment * `yield`
}