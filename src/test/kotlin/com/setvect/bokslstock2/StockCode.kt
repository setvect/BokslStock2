package com.setvect.bokslstock2

object StockCode {
    const val CODE_KODEX_200_069500 = "069500"
    const val CODE_KODEX_2X_122630 = "122630"
    const val CODE_KODEX_IV_2X_252670 = "252670"
    const val CODE_KODEX_KOSDAQ_229200 = "229200"
    const val CODE_KODEX_KOSDAQ_2X_233740 = "233740"
    const val CODE_KODEX_KOSDAQ_IV_251340 = "251340"
    const val CODE_KODEX_IV_114800 = "114800"
    const val CODE_SAMSUNG_005930 = "005930"
    const val CODE_TIGER_NASDAQ_133690 = "133690"
    const val CODE_TIGER_200_102110 = "102110"
    const val CODE_ARIRANG_HIGH_DV_161510 = "161510"
    const val CODE_TIGER_CSI300_192090 = "192090"
    const val CODE_KODEX_BANK_091170 = "091170"
    const val CODE_KODEX_SHORT_BONDS_153130 = "153130"

    val STOCK_CODE_MAP = mapOf(
        CODE_KODEX_200_069500 to "KODEX 200",
        CODE_KODEX_2X_122630 to "KODEX 레버리지",
        CODE_KODEX_IV_2X_252670 to "KODEX 200선물인버스2X",
        CODE_KODEX_KOSDAQ_229200 to "KODEX 코스닥 150",
        CODE_KODEX_KOSDAQ_2X_233740 to "KODEX 코스닥150 레버리지",
        CODE_KODEX_KOSDAQ_IV_251340 to "KODEX 코스닥150선물인버스",
        CODE_KODEX_IV_114800 to "KODEX 인버스",
        CODE_SAMSUNG_005930 to "삼성전자",
        CODE_TIGER_NASDAQ_133690 to "TIGER 미국나스닥100",
        CODE_TIGER_200_102110 to "TIGER 200",
        CODE_ARIRANG_HIGH_DV_161510 to "ARIRANG 고배당주",
        CODE_TIGER_CSI300_192090 to "TIGER 차이나CSI300",
        CODE_KODEX_BANK_091170 to "KODEX 은행",
        CODE_KODEX_SHORT_BONDS_153130 to "KODEX 단기채권"
    )

    // 해외 주식
    const val OS_CODE_TQQQ = "TQQQ"
    const val OS_CODE_SPY = "SPY"
    val OVERSEAS_STOCK_CODE_MAP = mapOf(
        OS_CODE_TQQQ to "TQQQ",
        OS_CODE_SPY to "SPY",
    )
}