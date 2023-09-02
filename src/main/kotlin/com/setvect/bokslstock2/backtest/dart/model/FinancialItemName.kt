package com.setvect.bokslstock2.backtest.dart.model

/**
 * DART 재무제표는 주요재무제표와 전체재무제표로 구분된다. 각각 항목에 재무제표 항목이름이 서로 다른다.
 * 예를 들면
 * 주요재무제표 '영업이익', 전체재무제표는 '영업이익(손실)' 이런식으로 다르다.
 */
data class FinancialItemName(val summaryName: String, val detailName: String)
