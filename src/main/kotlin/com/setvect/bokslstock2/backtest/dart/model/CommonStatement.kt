package com.setvect.bokslstock2.backtest.dart.model

import com.setvect.bokslstock2.crawl.dart.model.ReportCode

data class CommonStatement(
    val stockCode: String, // 사업연도(4자리)
    val year: Int, // 보고서 코드
    val reportCode: ReportCode,
)