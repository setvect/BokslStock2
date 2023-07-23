package com.setvect.bokslstock2.backtest.dart.model

import com.setvect.bokslstock2.crawl.dart.model.ReportCode

data class DartFilter (
    val year: Set<Int>,
    val quarter: Set<ReportCode>,
    val stockCodes: Set<String>,
)