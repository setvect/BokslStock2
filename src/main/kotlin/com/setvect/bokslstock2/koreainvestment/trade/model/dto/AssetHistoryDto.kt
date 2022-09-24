package com.setvect.bokslstock2.koreainvestment.trade.model.dto

import com.setvect.bokslstock2.koreainvestment.trade.entity.AssetHistoryEntity
import java.time.LocalDateTime

/**
 * @see AssetHistoryEntity 와 맵핑되는 DTO
 */
data class AssetHistoryDto(
    var assetHistorySeq: Long = 0,
    var account: String? = null,
    var assetCode: String? = null,
    var investment: Double = 0.0,
    var yield: Double = 0.0,
    var memo: String? = null,
    var regDate: LocalDateTime? = null,
)