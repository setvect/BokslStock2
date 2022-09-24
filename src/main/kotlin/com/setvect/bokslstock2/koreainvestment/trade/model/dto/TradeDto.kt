package com.setvect.bokslstock2.koreainvestment.trade.model.dto

import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.koreainvestment.trade.entity.TradeEntity
import java.time.LocalDateTime

/**
 * @see TradeEntity 와 맵핑되는 DTO
 */
data class TradeDto(
    var tradeSeq: Long = 0,
    var account: String? = null,
    var code: String? = null,
    /**종목 이름*/
    var name: String? = null,
    var tradeType: TradeType? = null,
    var qty: Int = 0,
    var unitPrice: Double = 0.0,

    var yield: Double = 0.0,
    var memo: String? = null,
    var regDate: LocalDateTime? = null,
) {
    fun getProfitLoss(): Long {
        return if (tradeType == TradeType.SELL) {
            return (unitPrice * qty * `yield`).toLong()
        } else {
            0L
        }
    }
}