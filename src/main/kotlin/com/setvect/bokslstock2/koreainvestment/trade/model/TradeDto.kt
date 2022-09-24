package com.setvect.bokslstock2.koreainvestment.trade.model

import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.koreainvestment.trade.entity.TradeEntity
import com.setvect.bokslstock2.util.ModalMapper
import java.time.LocalDateTime

/**
 * @see TradeEntity 와 맵핑되는 DTO
 */
data class TradeDto(
    var tradeSeq: Long? = null,
    var account: String? = null,
    var code: String? = null,
    var tradeType: TradeType? = null,
    var qty: Int? = null,
    var unitPrice: Double? = null,

    var yield: Double? = null,
    var memo: String? = null,
    var regDate: LocalDateTime? = null,
) {
    fun of(tradeEntity: TradeEntity): TradeDto {
        return ModalMapper.mapper.map(tradeEntity, TradeDto::class.java)
    }
}
