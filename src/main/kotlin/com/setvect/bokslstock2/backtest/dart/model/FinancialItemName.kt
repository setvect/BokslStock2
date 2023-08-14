package com.setvect.bokslstock2.backtest.dart.model

/**
 * DART 재무재표는 주요제무재표와 전체제무재표로 구분된다. 각각 항목에 제무제표 항목이름이 서로 다른다.
 * 예를 들면
 * 주요제무재표 '영업이익', 전체재무재표는 '영업이익(손실)' 이런식으로 다르다.
 */
data class FinancialItemName(val summaryName: String, val detailName: String)
