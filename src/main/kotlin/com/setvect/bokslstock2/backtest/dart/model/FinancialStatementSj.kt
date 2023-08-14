package com.setvect.bokslstock2.backtest.dart.model

/**
 * 재무제표구분
 */
enum class FinancialStatementSj(val code: String) {
    BS("BS"), // 재무상태표
    IS("IS")  // 손익계산서
}