package com.setvect.bokslstock2.backtest.dart.model

/**
 * 개별/연결구분
 */
enum class FinancialStatementFs(val code: String) {
    CFS("CFS"), // 연결재무제표
    OFS("OFS")  // 재무제표, 단일법인이면 CFS는 없고, OFS만 있음
}