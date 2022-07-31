package com.setvect.bokslstock2.value.dto

data class CompanySummary(
    val code: String,
    val name: String,
    val market: String,
    val capitalization: Int,
    val currentPrice: Int
) {
}
