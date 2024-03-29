package com.setvect.bokslstock2.backtest.common.model

import org.slf4j.Logger
import org.slf4j.LoggerFactory

enum class StockCode(val national: StockType, val code: String, val desc: String) {
    KODEX_200_069500(StockType.KOR, "069500", "KODEX 200"),
    KODEX_2X_122630(StockType.KOR, "122630", "KODEX 레버리지"),
    KODEX_IV_2X_252670(StockType.KOR, "252670", "KODEX 200선물인버스2X"),
    KODEX_KOSDAQ_229200(StockType.KOR, "229200", "KODEX 코스닥 150"),
    KODEX_KOSDAQ_2X_233740(StockType.KOR, "233740", "KODEX 코스닥150 레버리지"),
    KODEX_KOSDAQ_IV_251340(StockType.KOR, "251340", "KODEX 코스닥150선물인버스"),
    KODEX_IV_114800(StockType.KOR, "114800", "KODEX 인버스"),
    SAMSUNG_005930(StockType.KOR, "005930", "삼성전자"),
    TIGER_NASDAQ_133690(StockType.KOR, "133690", "TIGER 미국나스닥100"),
    TIGER_200_102110(StockType.KOR, "102110", "TIGER 200"),
    ARIRANG_HIGH_DV_161510(StockType.KOR, "161510", "ARIRANG 고배당주"),
    TIGER_CSI300_192090(StockType.KOR, "192090", "TIGER 차이나CSI300"),
    KODEX_BANK_091170(StockType.KOR, "091170", "KODEX 은행"),
    KODEX_SHORT_BONDS_153130(StockType.KOR, "153130", "KODEX 단기채권"),
    TIGER_USD_SHORT_BONDS_329750(StockType.KOR, "329750", "TIGER 미국달러단기채권액티브"),
    KODEX_TREASURY_BOND_3_114260(StockType.KOR, "114260", "KODEX 국고채3년"),
    KOSEF_TREASURY_BOND_10_148070(StockType.KOR, "148070", "KOSEF 국고채10년"),
    TIGER_SNP_360750(StockType.KOR, "360750", "TIGER 미국S&P500"),
    KOSEF_200TR_294400(StockType.KOR, "294400", "KOSEF 200TR"),
    KODEX_GLD_H_132030(StockType.KOR, "132030", "KODEX 골드선물(H)"),
    TIGER_USA_TREASURY_BOND_305080(StockType.KOR, "305080", "TIGER 미국채10년선물"),
    ACE_GLD_411060(StockType.KOR, "411060", "ACE KRX금현물"),
    KODEX_200_USD_BOND_284430(StockType.KOR, "284430", "KODEX 200미국채혼합"),

    CASH_1(StockType.CASH, "CASH_1", "현금 수익률 1%"),
    CASH_2(StockType.CASH, "CASH_2", "현금 수익률 2%"),
    CASH_3(StockType.CASH, "CASH_3", "현금 수익률 3%"),
    CASH_4(StockType.CASH, "CASH_4", "현금 수익률 4%"),
    CASH_5(StockType.CASH, "CASH_5", "현금 수익률 5%"),

    OS_CODE_QQQ(StockType.USA, "QQQ", "NASDAQ100"),
    OS_CODE_QLD(StockType.USA, "QLD", "NASDAQ100 * 2"),
    OS_CODE_TQQQ(StockType.USA, "TQQQ", "NASDAQ100 * 3"),
    OS_CODE_SPY(StockType.USA, "SPY", "S&P500"),
    OS_CODE_SSO(StockType.USA, "SSO", "S&P500 * 2"),
    OS_CODE_SPXL(StockType.USA, "SPXL", "S&P500 * 3"),
    OS_CODE_SHY(StockType.USA, "SHY", "단기채"),
    OS_CODE_IEF(StockType.USA, "IEF", "중기채"),
    OS_CODE_TLT(StockType.USA, "TLT", "장기채"),
    OS_CODE_UBT(StockType.USA, "UBT", "장기채 * 2"),
    OS_CODE_TMF(StockType.USA, "TMF", "장기채 * 3"),
    OS_CODE_GLD(StockType.USA, "GLD", "금"),
    OS_CODE_UGL(StockType.USA, "UGL", "금 * 2"),
    OS_CODE_VSS(StockType.USA, "VSS", "소형주"),
    OS_CODE_SCZ(StockType.USA, "SCZ", "소형주"),
    OS_CODE_IWD(StockType.USA, "IWD", "대형 중형 가치주"),

    EXCHANGE_DOLLAR(StockType.EXCHANGE, "WON-DOLLAR", "원달러 환율")
    ;

    companion object {
        val log: Logger = LoggerFactory.getLogger(StockCode::class.java)

        fun findByCode(code: String): StockCode {
            return values().first { it.code == code }
        }

        fun findByCodeOrNull(code: String): StockCode? {
            return values().firstOrNull { it.code == code }
        }
    }

    enum class StockType {
        KOR, USA, EXCHANGE, CASH
    }

}