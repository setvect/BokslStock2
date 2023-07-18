package com.setvect.bokslstock2.crawl.dart.service

import com.setvect.bokslstock2.crawl.dart.model.CompanyCode
import com.setvect.bokslstock2.crawl.dart.model.ReportCode

interface DartMakerJson {
    fun save(
        body: String,
        companyCodeMap: Map<String, CompanyCode>,
        year: Int,
        reportCode: ReportCode
    )
}