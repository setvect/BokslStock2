package com.setvect.bokslstock2.crawl.dart.model

/**
 * 보고서 코드
 * 여기서 분기 보고서를 잘 이해 해야된다. 우리가 알고 있는 1분기 2분기 이런거 아니다.
 *
 * 회사 회계 기준에 따라 분기가 달라진다.
 * 예들어 결산 월에 따라 1분기가 달라진다.
 * - 12월 말 결산: 1 ~ 3월
 * - 3월 말 결산: 4 ~ 6월
 * - 6월 말 결산: 7 ~ 9월
 * - 12월 말 결산: 10 ~ 12월
 */
enum class ReportCode(val code: String, val label: String) {
    QUARTER1("11013", "1분기보고서"),
    HALF_ANNUAL("11012", "반기보고서"),
    QUARTER3("11014", "3분기보고서"),
    ANNUAL("11011", "사업보고서");
}
