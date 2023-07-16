package com.setvect.bokslstock2.crawl.dart.model

/**
 * 보고서 코드
 */
enum class ReportCode(val code: String, val label: String) {
    QUARTER1("11013", "1분기보고서"),
    HALF_ANNUAL("11012", "반기보고서"),
    QUARTER3("11014", "3분기보고서"),
    ANNUAL("11011", "사업보고서");
}
