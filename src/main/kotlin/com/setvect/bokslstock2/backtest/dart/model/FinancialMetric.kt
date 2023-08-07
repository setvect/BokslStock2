package com.setvect.bokslstock2.backtest.dart.model

/**
 * 재무제표 항목
 *
 * 여기서 주요 재무제포, 상세 제무재표 의미는
 * - 주요 재무제표: CrawlerDartService.crawlCompanyFinancialInfo() 수집한 결과
 * - 상세 재무제표: CrawlerDartService.??() 수집한 결과
 *
 * @param summaryMfg 재조업 경우 주요 재무제표 항목 이름
 * @param summaryService 서비스업 경우 주요 재무제표 항목 이름
 * @param detailMfg 재조업 경우 상세 재무제표 항목 이름
 * @param detailService 서비스업 경우 상세 재무제표 항목 이름
 */
enum class FinancialMetric(val summaryMfg: String, val summaryService: String?, val detailMfg: String, val detailService: String?) {
    SALES_REVENUE("매출액", null, "수익(매출액)", "영업수익"),
    OPERATING_PROFIT("영업이익", "영업이익", "영업이익(손실)", "영업이익(손실)"), // 영업이익
    TOTAL_ASSETS("자산총계", "자산총계", "자산총계", "자산총계"), // 자산총계
    TOTAL_LIABILITIES("부채총계", "부채총계", "부채총계", "부채총계"), // 부채총계
    NET_PROFIT("당기순이익", "당기순이익", "당기순이익(손실)", "당기순이익(손실)"), // 순이익
}