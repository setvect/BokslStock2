package com.setvect.bokslstock2.koreainvestment.trade.model

import com.querydsl.core.annotations.QueryProjection
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.koreainvestment.trade.entity.TradeEntity
import com.setvect.bokslstock2.util.ModalMapper
import java.time.LocalDateTime

/**
 * @see TradeEntity 와 맵핑되는 DTO
 */
data class TradeDto @QueryProjection constructor(
    val tradeSeq: Long = 0L,
    val account: String,
    val code: String,
    val tradeType: TradeType,
    val qty: Int,
    val unitPrice: Double,

    val yield: Double,
    val memo: String? = null,
    val regDate: LocalDateTime,
) {
    fun of(tradeEntity: TradeEntity): TradeDto {
        return ModalMapper.mapper.map(tradeEntity, TradeDto::class.java)
    }
}
