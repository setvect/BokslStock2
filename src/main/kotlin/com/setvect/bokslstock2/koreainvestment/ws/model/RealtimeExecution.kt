package com.setvect.bokslstock2.koreainvestment.ws.model

import com.setvect.bokslstock2.util.DateUtil
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate
import java.time.LocalTime

/**
 * 실시간 체결 정보
 */
data class RealtimeExecution(
    val code: String, // 유가증권 단축 종목코드
    val stckCntgHour: LocalTime, // 주식 체결 시간
    val stckPrpr: Int, // 주식 현재가
    val prdyVrssSign: String, // 전일 대비 부호
    val prdyVrss: Int, // 전일 대비
    val prdyCtrt: Double, // 전일 대비율
    val wghnAvrgStckPrc: Double, // 가중 평균 주식 가격
    val stckOprc: Int, // 주식 시가
    val stckHgpr: Int, // 주식 최고가
    val stckLwpr: Int, // 주식 최저가
    val askp1: Int, // 매도호가1
    val bidp1: Int, // 매수호가1
    val cntgVol: Long, // 체결 거래량
    val acmlVol: Long, // 누적 거래량
    val acmlTrPbmn: Long, // 누적 거래 대금
    val selnCntgCsnu: Long, // 매도 체결 건수
    val shnuCntgCsnu: Long, // 매수 체결 건수
    val ntbyCntgCsnu: Long, // 순매수 체결 건수
    val cttr: Double, // 체결강도
    val selnCntgSmtn: Long, // 총 매도 수량
    val shnuCntgSmtn: Long, // 총 매수 수량
    val ccldDvsn: String, // 체결구분
    val shnuRate: Double, // 매수비율
    val prdyVolVrssAcmlVolRate: Double, // 전일 거래량 대비 등락율
    val oprcHour: LocalTime, // 시가 시간
    val oprcVrssPrprSign: String, // 시가대비구분
    val oprcVrssPrpr: Long, // 시가대비
    val hgprHour: LocalTime, // 최고가 시간
    val hgprVrssPrprSign: String, // 고가대비구분
    val hgprVrssPrpr: Long, // 고가대비
    val lwprHour: LocalTime, // 최저가 시간
    val lwprVrssPrprSign: String, // 저가대비구분
    val lwprVrssPrpr: Long, // 저가대비
    val bsopDate: LocalDate, // 영업 일자
    val newMkopClsCode: String, // 신 장운영 구분 코드
    val trhtYn: String, // 거래정지 여부
    val askpRsqn1: Long, // 매도호가 잔량1
    val bidpRsqn1: Long, // 매수호가 잔량1
    val totalAskpRsqn: Long, // 총 매도호가 잔량
    val totalBidpRsqn: Long, // 총 매수호가 잔량
    val volTnrt: Double, // 거래량 회전율
    val prdySmnsHourAcmlVol: Long, // 전일 동시간 누적 거래량
    val prdySmnsHourAcmlVolRate: Double, // 전일 동시간 누적 거래량 비율
    val hourClsCode: String, // 시간 구분 코드
    val mrktTrtmClsCode: String, // 임의종료구분코드
    val viStndPrc: Long, // 정적VI발동기준가
) {
    companion object {


        /**
         * 예시값
         * [rawText] 005930^151619^59700^2^900^1.53^59055.30^58200^59900^58200^59700^59600^34^10247065^605142384800^21595^33584^11989^155.15^3922631^6086067^1^0.60^105.75^090008^2^1500^150053^5^-200^090008^2^1500^20220831^20^N^33918^140697^663790^898987^0.17^9048877^113.24^0^^58200
         */
        fun parsing(rawText: String): RealtimeExecution {
            val dataArray = StringUtils.splitByWholeSeparatorPreserveAllTokens(rawText, "^")
            return RealtimeExecution(
                code = dataArray[0],
                stckCntgHour = DateUtil.getLocalTime(dataArray[1], DateUtil.HHmmss),
                stckPrpr = dataArray[2].toInt(),
                prdyVrssSign = dataArray[3],
                prdyVrss = dataArray[4].toInt(),
                prdyCtrt = dataArray[5].toDouble(),
                wghnAvrgStckPrc = dataArray[6].toDouble(),
                stckOprc = dataArray[7].toInt(),
                stckHgpr = dataArray[8].toInt(),
                stckLwpr = dataArray[9].toInt(),
                askp1 = dataArray[10].toInt(),
                bidp1 = dataArray[11].toInt(),
                cntgVol = dataArray[12].toLong(),
                acmlVol = dataArray[13].toLong(),
                acmlTrPbmn = dataArray[14].toLong(),
                selnCntgCsnu = dataArray[15].toLong(),
                shnuCntgCsnu = dataArray[16].toLong(),
                ntbyCntgCsnu = dataArray[17].toLong(),
                cttr = dataArray[18].toDouble(),
                selnCntgSmtn = dataArray[19].toLong(),
                shnuCntgSmtn = dataArray[20].toLong(),
                ccldDvsn = dataArray[21],
                shnuRate = dataArray[22].toDouble(),
                prdyVolVrssAcmlVolRate = dataArray[23].toDouble(),
                oprcHour = DateUtil.getLocalTime(dataArray[24], DateUtil.HHmmss),
                oprcVrssPrprSign = dataArray[25],
                oprcVrssPrpr = dataArray[26].toLong(),
                hgprHour = DateUtil.getLocalTime(dataArray[27], DateUtil.HHmmss),
                hgprVrssPrprSign = dataArray[28],
                hgprVrssPrpr = dataArray[29].toLong(),
                lwprHour = DateUtil.getLocalTime(dataArray[30], DateUtil.HHmmss),
                lwprVrssPrprSign = dataArray[31],
                lwprVrssPrpr = dataArray[32].toLong(),
                bsopDate = DateUtil.getLocalDate(dataArray[33], DateUtil.yyyyMMdd),
                newMkopClsCode = dataArray[34],
                trhtYn = dataArray[35],
                askpRsqn1 = dataArray[36].toLong(),
                bidpRsqn1 = dataArray[37].toLong(),
                totalAskpRsqn = dataArray[38].toLong(),
                totalBidpRsqn = dataArray[39].toLong(),
                volTnrt = dataArray[40].toDouble(),
                prdySmnsHourAcmlVol = dataArray[41].toLong(),
                prdySmnsHourAcmlVolRate = dataArray[42].toDouble(),
                hourClsCode = dataArray[43],
                mrktTrtmClsCode = dataArray[44],
                viStndPrc = dataArray[45].toLong(),
            )
        }
    }
}

