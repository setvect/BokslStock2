package com.setvect.bokslstock2.koreainvestment.vbs.service

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.trade.model.request.*
import com.setvect.bokslstock2.koreainvestment.trade.model.response.BalanceResponse
import com.setvect.bokslstock2.koreainvestment.trade.model.response.TokenResponse
import com.setvect.bokslstock2.koreainvestment.trade.service.StockClientService
import com.setvect.bokslstock2.koreainvestment.ws.model.RealtimeExecution
import com.setvect.bokslstock2.koreainvestment.ws.model.WsResponse
import com.setvect.bokslstock2.slack.SlackMessageService
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateUtil
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoField
import java.util.concurrent.TimeUnit


/** 매도호가 차이(원) */
private const val SELL_DIFF = 15

/** ETF 호가 단위(원)*/
private const val QUOTE_UNIT = 5

@Service
class VbsService(
    private val stockClientService: StockClientService,
    private val bokslStockProperties: BokslStockProperties,
    private val slackMessageService: SlackMessageService
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private var token: TokenResponse = TokenResponse("", DateUtil.currentDateTime(DateUtil.yyyy_MM_dd_HH_mm_ss), "", 0L)
    private var currentDate: LocalDate = LocalDate.now().minusDays(1)

    private var requestBalance: BalanceResponse? = null


    private var run = false

    /**목표가 <종목코드, 매수가격>*/
    private var targetPrice = mapOf<String, Int>()

    companion object {
        // 매매 시작
        private val START_TIME = LocalTime.of(8, 41, 0).get(ChronoField.MILLI_OF_DAY)

        // 시초가 매도
        private val SELL_OPEN_TIME = LocalTime.of(8, 59, 55).get(ChronoField.MILLI_OF_DAY)

        // 장 시작
        private val OPEN_TIME = LocalTime.of(9, 0, 0).get(ChronoField.MILLI_OF_DAY)

        // 9시 5분 매도 시간
        private val SELL_5_TIME = LocalTime.of(9, 5, 0).get(ChronoField.MILLI_OF_DAY)

        // 매수
        private val BUY_TIME = LocalTime.of(9, 5, 10).get(ChronoField.MILLI_OF_DAY)

        // 장 종료(동시호가 제외)
        private val CLOSE_TIME = LocalTime.of(13, 20, 0).get(ChronoField.MILLI_OF_DAY)
    }

    /**
     * 체결
     */
    fun execution(wsResponse: WsResponse) {
        val now = LocalTime.now().get(ChronoField.MILLI_OF_DAY)
        if (!isBuyTimeRange(now) || targetPrice.isEmpty()) {
            return
        }

        val realtimeExecution = RealtimeExecution.parsing(wsResponse.responseData)
        log.info("${wsResponse.trId} = $realtimeExecution")

        val code = realtimeExecution.code
        val price = targetPrice[code] ?: return


        var holdingsMap = getHoldingStock()

        if (price <= realtimeExecution.stckPrpr) {
            holdingsMap.size

            stockClientService.requestOrderBuy(OrderRequest(bokslStockProperties.koreainvestment.vbs.accountNo, code, price, 100), token.accessToken)
            // 주문 접수 후 딜레이
            TimeUnit.SECONDS.sleep(3)
        }
    }

    /**
     * 호가
     */
    fun quotation(response: WsResponse) {
        // 실시간 호가 사용안함
    }

    @Async
    fun start() {
        // TODO 동기화 문제가 있을 수 있음
        if (run) {
            log.info("이미 시작중")
            return
        } else {
            log.info("매매 시작")
            run = true
        }
        checkDay()

        try {
            if (!checkTradeDay()) {
                return
            }
            val code = bokslStockProperties.koreainvestment.vbs.stock[0].code
            val requestCurrentPrice = stockClientService.requestCurrentPrice(CurrentPriceRequest(code), token.accessToken)
            checkTrade()
        } finally {
            run = false
        }
    }

    private fun checkTrade() {
        var sellOpenFlag = false
        var sell5Flag = false
        targetPrice = mapOf()
        while (true) {
            val now = LocalTime.now().get(ChronoField.MILLI_OF_DAY)


            // 시초가 매도
            val sellOpenTime = now in (SELL_OPEN_TIME + 1) until OPEN_TIME
            val vbsStocks = bokslStockProperties.koreainvestment.vbs.stock
            if (sellOpenTime && !sellOpenFlag) {
                val openSellStockList = vbsStocks.filter { it.openSell }
                sellOrder(openSellStockList)
                sellOpenFlag = true
            }

            // 장 시작 5분후 매도
            val sell5Time = now in (SELL_5_TIME + 1) until SELL_5_TIME
            if (sell5Time && !sell5Flag) {
                val openSellStockList = vbsStocks.filter { !it.openSell }
                sellOrder(openSellStockList)
                sell5Flag = true
            }

            if (isBuyTimeRange(now)) {
                targetPrice = vbsStocks.associate { stock ->
                    val stockClientService = stockClientService.requestDatePrice(DatePriceRequest(stock.code, DatePriceRequest.DateType.DAY), token.accessToken)
                    val openPrice = stockClientService.output!![0].stckOprc
                    val beforeDayHigh = stockClientService.output[1].stckHgpr
                    val beforeDayLow = stockClientService.output[1].stckLwpr
                    val tempPrice = openPrice + (beforeDayHigh - beforeDayLow) * stock.k
                    val targetPrice = (tempPrice - tempPrice % QUOTE_UNIT).toInt()

                    log.info("[목표가] ${stock.code}: $openPrice + ($beforeDayHigh - $beforeDayLow) * ${stock.k} = $targetPrice")
                    stock.code to targetPrice
                }
                // TODO 목표가 슬랙보내기

                // 목표가 계산이후 종료
                return;
            }


            // 목표가 계산
            TimeUnit.SECONDS.sleep(1)
        }
    }

    /**
     * @return 매수 가능 시간
     */
    private fun isBuyTimeRange(now: Int) = now in (BUY_TIME + 1) until CLOSE_TIME

    /**
     * @return 매매 가능
     */
    private fun checkTradeDay(): Boolean {
        // TODO 주말이 아닌 휴장일 판단하는 로직 들어가야 됨
        val now = LocalTime.now().get(ChronoField.MILLI_OF_DAY)
        val dayOfWeek = LocalDate.now().dayOfWeek
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            log.info("휴장일입니다.")
            return false;
        }
        if (now < START_TIME || CLOSE_TIME < now) {
            log.info("매매 시간이 아닙니다.")
            return false
        }
        return true
    }

    /**
     * [openSellStockList] 매도 종목
     */
    private fun sellOrder(openSellStockList: List<BokslStockProperties.Vbs.VbsStock>) {
        var holdingsMap = getHoldingStock()

        openSellStockList.forEach {
            val stock = holdingsMap[it.code] ?: return@forEach

            val quoteResponse = stockClientService.requestQuote(QuoteRequest(it.code), token.accessToken)
            val sellPrice = quoteResponse.output2!!.expectedPrice - SELL_DIFF

            val message = "[매도 주문] ${stock.prdtName}(${stock.code}), " +
                "주문가: ${String.format("%,d", sellPrice)}, " +
                "매수평단가: ${String.format("%,d", stock.pchsAvgPric.toInt())}, " +
                "수량: ${String.format("%,d", stock.hldgQty)}, " +
                "수익률(추정): ${String.format("%,.2f%%", ApplicationUtil.getYield(stock.pchsAvgPric.toInt(), sellPrice))}"
            log.info(message)
            stockClientService.requestOrderSell(
                OrderRequest(
                    cano = bokslStockProperties.koreainvestment.vbs.accountNo,
                    code = it.code,
                    ordunpr = sellPrice,
                    ordqty = stock.hldgQty
                ),
                token.accessToken
            )
            slackMessageService.sendMessage(message)
        }
    }

    /**
     * 주식 잔고
     * @return <종목코드, 잔고정보>
     */
    private fun getHoldingStock(): Map<String, BalanceResponse.Holdings> {
        return requestBalance!!.holdings.associateBy { it.code }
    }

    /**
     * 날짜가 변경될 경우 최초 로드
     */
    private fun checkDay() {
        if (currentDate == LocalDate.now()) {
            return
        }
        loadToken()
        val accountNo = bokslStockProperties.koreainvestment.vbs.accountNo
        requestBalance = stockClientService.requestBalance(BalanceRequest(accountNo), token.accessToken)
        currentDate = LocalDate.now()
    }

    private fun loadToken() {
        token = stockClientService.requestToken()
        log.info("load token: ${StringUtils.substring(token.accessToken, 10)}...")
    }
}
