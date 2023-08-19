package com.setvect.bokslstock2.backtest.dart.model

import com.setvect.bokslstock2.crawl.dart.model.ReportCode

/**
 * 주요 재무제표 검색 항목
 */
class FinancialSearchCondition(
    val stockCode: String? = null,
    val reportCode: Set<ReportCode>? = null,
    val accountNm: Set<String>? = null,
    val fsDiv: Set<FinancialFs>? = null
)