package com.setvect.bokslstock2.backtest.dart.model

import com.setvect.bokslstock2.crawl.dart.model.ReportCode

/**
 * 상세 재무제표 키. 중복 데이터 방지를 위한 키
 */
data class FinancialDetailKey (
    val stockCode: String, // 상장회사의 종목코드(6자리)
    val year: Int, // 사업연도(4자리)
    val reportCode: ReportCode, // 보고서 코드
    val fsDiv: FinancialFs,  // CFS:연결재무제표, OFS:재무제표
)