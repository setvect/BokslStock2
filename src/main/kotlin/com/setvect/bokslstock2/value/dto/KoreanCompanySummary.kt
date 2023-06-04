package com.setvect.bokslstock2.value.dto

data class KoreanCompanySummary(
    val code: String,
    val name: String,
    val market: String,
    val capitalization: Int,
    val currentPrice: Int
) {
}
