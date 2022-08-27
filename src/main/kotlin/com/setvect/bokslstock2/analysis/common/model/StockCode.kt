package com.setvect.bokslstock2.analysis.common.model

enum class StockCode(val national: National, val code: String, val desc: String) {
    KODEX_200_069500(National.KOR, "069500", "KODEX 200"),
    KODEX_2X_122630(National.KOR, "122630", "KODEX 레버리지"),
    KODEX_IV_2X_252670(National.KOR, "252670", "KODEX 200선물인버스2X"),
    KODEX_KOSDAQ_229200(National.KOR, "229200", "KODEX 코스닥 150"),
    KODEX_KOSDAQ_2X_233740(National.KOR, "233740", "KODEX 코스닥150 레버리지"),
    KODEX_KOSDAQ_IV_251340(National.KOR, "251340", "KODEX 코스닥150선물인버스"),
    KODEX_IV_114800(National.KOR, "114800", "KODEX 인버스"),
    SAMSUNG_005930(National.KOR, "005930", "삼성전자"),
    TIGER_NASDAQ_133690(National.KOR, "133690", "TIGER 미국나스닥100"),
    TIGER_200_102110(National.KOR, "102110", "TIGER 200"),
    ARIRANG_HIGH_DV_161510(National.KOR, "161510", "ARIRANG 고배당주"),
    TIGER_CSI300_192090(National.KOR, "192090", "TIGER 차이나CSI300"),
    KODEX_BANK_091170(National.KOR, "091170", "KODEX 은행"),
    KODEX_SHORT_BONDS_153130(National.KOR, "153130", "KODEX 단기채권"),

    OS_CODE_QQQ(National.USA, "QQQ", "NASDAQ100"),
    OS_CODE_QLD(National.USA, "QLD", "NASDAQ100 * 2"),
    OS_CODE_TQQQ(National.USA, "TQQQ", "NASDAQ100 * 3"),
    OS_CODE_SPY(National.USA, "SPY", "S&P500"),
    OS_CODE_SSO(National.USA, "SSO", "S&P500 * 2"),
    OS_CODE_SPXL(National.USA, "SPXL", "SPXL"),
    OS_CODE_SHY(National.USA, "SHY", "단기채"),
    OS_CODE_IEF(National.USA, "IEF", "중기채"),
    OS_CODE_TLT(National.USA, "TLT", "장기채"),
    OS_CODE_UBT(National.USA, "UBT", "장기채 * 2"),
    OS_CODE_TMF(National.USA, "TMF", "장기채 * 3"),
    OS_CODE_GLD(National.USA, "GLD", "금"),
    OS_CODE_UGL(National.USA, "UGL", "금 * 2"),
    OS_CODE_VSS(National.USA, "VSS", "소형주"),
    OS_CODE_SCZ(National.USA, "SCZ", "소형주"),
    OS_CODE_IWD(National.USA, "IWD", "대형 중형 가치주");

    companion object {
        fun findByCode(code: String): StockCode {
            return values().first { it.code == code }
        }
    }

    enum class National {
        KOR, USA
    }

}