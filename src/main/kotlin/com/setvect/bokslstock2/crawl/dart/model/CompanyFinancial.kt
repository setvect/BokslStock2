package com.setvect.bokslstock2.crawl.dart.model

data class CompanyFinancial(
    val status: String,
    val message: String,
    val list: List<FinancialStatement>
)