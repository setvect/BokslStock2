package com.setvect.bokslstock2.backtest.dart.model

import com.setvect.bokslstock2.crawl.dart.model.ReportCode

data class DetailStatement (
    val year: Int, // 사업연도(4자리)
    val reportCode: ReportCode, // 보고서 코드
    val stockCode: String, // 상장회사의 종목코드(6자리)
    val fs: FinancialFs, // CFS:연결재무제표, OFS:재무제표
)