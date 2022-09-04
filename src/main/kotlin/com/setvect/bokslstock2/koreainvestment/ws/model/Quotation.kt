package com.setvect.bokslstock2.koreainvestment.ws.model

/**
 * 실시간 호가
 */
data class Quotation(
    val code: String, // 종목코드
    val askPrice: Map<Int, Quote>, // 매도호가 10단계, Key: 10 ~ 1, 10 높은가격 1 낮은가격
    val bidPrice: Map<Int, Quote>, // 매수호가 10단계, Key: 1 ~ 10, 1 높은가격 10 낮은가격
    val totalAskpRsqn: Long, // 총 매도호가 잔량
    val totalBidpRsqn: Long, // 총 매수호가 잔량
    val ovtmTotalAskpRsqn: Long, // 시간외 총 매도호가 잔량
    val ovtmTotalBidpRsqn: Long, // 시간외 총 매수호가 잔량
    val antcCnpr: Long, // 예상 체결가
    val antcCnqn: Long, // 예상 체결량
    val antcVol: Long, // 예상 거래량
    val antcCntgVrss: Double, // 예상 체결 대비
    val antcCntgVrssSign: String, // 예상 체결 대비 부호, 1: 상한, 2: 상승, 3: 보합, 4: 하한, 5: 하락
    val antcCntgPrdyCtrt: Double, // 예상 체결 전일 대비율
    val acmlVol: Long, // 누적 거래량
    val totalAskpRsqnIcdc: Long, // 총 매도호가 잔량 증감
    val totalBidpRsqnIcdc: Long, // 총 매수호가 잔량 증감
    val ovtmTotalAskpIcdc: Long, // 시간외 총 매도호가 증감
    val ovtmTotalBidpIcdc: Long, // 시간외 총 매수호가 증감
    val stckDealClsCode: String, // 주식 매매 구분 코드

) {

    companion object {
        /**
         * 예시값
         * [rawText] 233740^152809^A^8960^8965^8970^0^0^0^0^0^0^0^8955^8950^8945^0^0^0^0^0^0^0^29^636^684^0^0^0^0^0^0^0^1070^14889^10942^0^0^0^0^0^0^0^0^0^0^0^8955^229477^229477^180^2^2.05^12263346^0^0^0^0^0
         */
        fun parsing(rawText: String): Quotation {

            return Quotation(
                code = "100",
                askPrice = mapOf(),
                bidPrice = mapOf(),
                totalAskpRsqn = 100,
                totalBidpRsqn = 100,
                ovtmTotalAskpRsqn = 100,
                ovtmTotalBidpRsqn = 100,
                antcCnpr = 100,
                antcCnqn = 100,
                antcVol = 100,
                antcCntgVrss = 100.0,
                antcCntgVrssSign = "1",
                antcCntgPrdyCtrt = 100.0,
                acmlVol = 100,
                totalAskpRsqnIcdc = 100,
                totalBidpRsqnIcdc = 100,
                ovtmTotalAskpIcdc = 100,
                ovtmTotalBidpIcdc = 100,
                stckDealClsCode = "AA",
            )
        }
    }


    /**
     * [count] 수량, [price] 가격
     */
    data class Quote(val count: Long, val price: Double)
}

