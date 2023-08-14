package com.setvect.bokslstock2.backtest.dart.model

/**
 * 재무제표구분
 */
enum class FinancialStatementSj(val code: String) {
    BS("BS"), // 재무상태표
    IS("IS"),  // 손익계산서
    CIS("CIS"), // 포괄손익계산서
    CF("CF"), // 현금흐름표
    SCE("SCE"), // 자본변동표
}