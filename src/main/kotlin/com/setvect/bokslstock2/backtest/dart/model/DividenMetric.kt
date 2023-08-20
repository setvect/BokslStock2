package com.setvect.bokslstock2.backtest.dart.model

/**
 * 년도별 손익계산서 항목 값
 */
enum class DividenMetric(val se: String, val stockKnd: String? = null) {
    FACE_VALUE_PER_SHARE("주당액면가액(원)"),
    NET_PROFIT_PER_SHARE("(연결)주당순이익(원)"),
    CASH_DIVIDEND_PROPENSITY("(연결)현금배당성향(%)"),
    CASH_DIVIDEND_YIELD("현금배당수익률(%)", "보통주"),
    CASH_DIVIDEND_AMOUNT("주당 현금배당금(원)", "보통주"),
    ;
}