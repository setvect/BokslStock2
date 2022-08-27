package com.setvect.bokslstock2

object StockCode {
    const val KODEX_200_069500 = "069500"
    const val KODEX_2X_122630 = "122630"
    const val KODEX_IV_2X_252670 = "252670"
    const val KODEX_KOSDAQ_229200 = "229200"
    const val KODEX_KOSDAQ_2X_233740 = "233740"
    const val KODEX_KOSDAQ_IV_251340 = "251340"
    const val KODEX_IV_114800 = "114800"
    const val SAMSUNG_005930 = "005930"
    const val TIGER_NASDAQ_133690 = "133690"
    const val TIGER_200_102110 = "102110"
    const val ARIRANG_HIGH_DV_161510 = "161510"
    const val TIGER_CSI300_192090 = "192090"
    const val KODEX_BANK_091170 = "091170"
    const val KODEX_SHORT_BONDS_153130 = "153130"

    val STOCK_CODE_MAP = mapOf(
        KODEX_200_069500 to "KODEX 200",
        KODEX_2X_122630 to "KODEX 레버리지",
        KODEX_IV_2X_252670 to "KODEX 200선물인버스2X",
        KODEX_KOSDAQ_229200 to "KODEX 코스닥 150",
        KODEX_KOSDAQ_2X_233740 to "KODEX 코스닥150 레버리지",
        KODEX_KOSDAQ_IV_251340 to "KODEX 코스닥150선물인버스",
        KODEX_IV_114800 to "KODEX 인버스",
        SAMSUNG_005930 to "삼성전자",
        TIGER_NASDAQ_133690 to "TIGER 미국나스닥100",
        TIGER_200_102110 to "TIGER 200",
        ARIRANG_HIGH_DV_161510 to "ARIRANG 고배당주",
        TIGER_CSI300_192090 to "TIGER 차이나CSI300",
        KODEX_BANK_091170 to "KODEX 은행",
        KODEX_SHORT_BONDS_153130 to "KODEX 단기채권"
    )

    // 해외 주식
    const val OS_CODE_QQQ = "QQQ"
    const val OS_CODE_QLD = "QLD"
    const val OS_CODE_TQQQ = "TQQQ"

    const val OS_CODE_SPY = "SPY"
    const val OS_CODE_SSO = "SSO"
    const val OS_CODE_SPXL = "SPXL"

    // 단기채
    const val OS_CODE_SHY = "SHY"

    // 중기채
    const val OS_CODE_IEF = "IEF"

    // 장기채
    const val OS_CODE_TLT = "TLT"
    const val OS_CODE_UBT = "UBT"
    const val OS_CODE_TMF = "TMF"

    // 금
    const val OS_CODE_GLD = "GLD"
    const val OS_CODE_UGL = "UGL"

    // 소형주
    const val OS_CODE_VSS = "VSS"

    // 소형주
    const val OS_CODE_SCZ = "SCZ"

    // 대형 중형 가치주
    const val OS_CODE_IWD = "IWD"

    val OVERSEAS_STOCK_CODE_MAP = mapOf(
        OS_CODE_QQQ to "QQQ",
        OS_CODE_QLD to "QLD",
        OS_CODE_TQQQ to "TQQQ",
        OS_CODE_SPY to "SPY",
        OS_CODE_SSO to "SSO",
        OS_CODE_SPXL to "SPXL",
        OS_CODE_SHY to "SHY",
        OS_CODE_IEF to "IEF",
        OS_CODE_TLT to "TLT",
        OS_CODE_UBT to "UBT",
        OS_CODE_TMF to "TMF",
        OS_CODE_GLD to "GLD",
        OS_CODE_UGL to "UGL",
        OS_CODE_VSS to "VSS",
        OS_CODE_SCZ to "SCZ",
        OS_CODE_IWD to "IWD",
    )
}