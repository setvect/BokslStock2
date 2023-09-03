package com.setvect.bokslstock2.crawl.dart

import java.io.File

object DartConstants {
    // 기업 코드
    val CORP_CODE_PATH = File("crawl/dart/CORPCODE.xml")
    // 주요 기업 재무
    val FINANCIAL_PATH = File("crawl/dart/financial")
    // 전체 기업 재무
    val FINANCIAL_DETAIL_PATH = File("crawl/dart/financialDetail")
    // 주식 총수
    val QUANTITY_PATH = File("crawl/dart/quantity")
    // 배당
    val DIVIDEND_PATH = File("crawl/dart/dividend")
}