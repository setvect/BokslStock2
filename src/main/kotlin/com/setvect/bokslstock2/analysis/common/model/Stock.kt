package com.setvect.bokslstock2.analysis.common.model

import com.setvect.bokslstock2.index.entity.StockEntity

/**
 * 주식 종목
 */
@Deprecated("StockCode로 변경")
data class Stock(
    /**
     * 종목이름
     */
    val name: String,
    /**
     * 종목코드
     */
    val code: String
) {
    companion object {
        fun of(entity: StockEntity): Stock {
            return Stock(entity.name, entity.code)
        }
    }

    override fun toString(): String {
        return "$name(${code})"
    }
}