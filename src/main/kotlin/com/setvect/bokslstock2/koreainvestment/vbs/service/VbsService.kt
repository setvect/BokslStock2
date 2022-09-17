package com.setvect.bokslstock2.koreainvestment.vbs.service

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.trade.model.request.*
import com.setvect.bokslstock2.koreainvestment.trade.model.response.BalanceResponse
import com.setvect.bokslstock2.koreainvestment.trade.model.response.CancelableResponse
import com.setvect.bokslstock2.koreainvestment.trade.model.response.CommonResponse
import com.setvect.bokslstock2.koreainvestment.trade.service.StockClientService
import com.setvect.bokslstock2.koreainvestment.trade.service.TokenService
import com.setvect.bokslstock2.koreainvestment.ws.model.RealtimeExecution
import com.setvect.bokslstock2.koreainvestment.ws.model.WsResponse
import com.setvect.bokslstock2.koreainvestment.ws.service.TradeTimeHelper
import com.setvect.bokslstock2.slack.SlackMessageService
import com.setvect.bokslstock2.util.ApplicationUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.concurrent.TimeUnit


/** 매도호가 차이(원) */
private const val SELL_DIFF = 15

/** ETF 호가 단위(원)*/
private const val QUOTE_UNIT = 5

@Service
class VbsService(
    private val stockClientService: StockClientService,
    private val bokslStockProperties: BokslStockProperties,
    private val slackMessageService: SlackMessageService,
    private val tokenService: TokenService,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private var currentDate: LocalDate = LocalDate.now().minusDays(1)
    private var balanceResponse: BalanceResponse? = null

    /** 매수 또는 매수 대기중인 종목 */
    private val buyCode = mutableSetOf<String>()

    private var run = false

    /**목표가 <종목코드, 매수가격>*/
    private var targetPrice = mapOf<String, Int>()

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
            if (!TradeTimeHelper.isTimeToTrade()) {
                return
            }
            checkTrade()
        } finally {
            run = false
        }
    }

    /**
     * 잔고 리포트
     */
    fun report() {
        loadBalance()
        if (this.balanceResponse == null) {
            log.warn("잔고 정보 없음")
            return
        }

        val stockInfo = balanceResponse!!.holdings.map { stock ->
            "${stock.prdtName}(${stock.code}\n" +
                "  - 수량: ${String.format("%,d", stock.hldgQty)}\n" +
                "  - 수익률: ${String.format("%,.2f%%", stock.evluPflsRt)}\n" +
                "  - 평가가격: ${String.format("%,d", stock.evluAmt)}\n"
        }.joinToString { "\n" }

        val message = "<장 종료 리포트>\n$stockInfo"
        log.info(message)
        slackMessageService.sendMessage(message)
    }

    private fun checkTrade() {
        var sellOpenFlag = false
        var sell5Flag = false
        targetPrice = mapOf()
        while (true) {
            val sellOpenTime = TradeTimeHelper.isOpenPriceSellTime()
            val vbsStocks = bokslStockProperties.koreainvestment.vbs.stock
            if (sellOpenTime && !sellOpenFlag) {
                val openSellStockList = vbsStocks.filter { it.openSell }
                sellOrder(openSellStockList)
                sellOpenFlag = true
            }

            val sell5Time = TradeTimeHelper.isOpen5MinPriceSellTime()
            if (sell5Time && !sell5Flag) {
                val openSellStockList = vbsStocks.filter { !it.openSell }
                sellOrder(openSellStockList)
                sell5Flag = true
            }

            if (TradeTimeHelper.isBuyTimeRange()) {
                val targetPriceMessage = StringBuilder()
                targetPrice = vbsStocks.associate { stock ->
                    val stockClientService = stockClientService.requestDatePrice(DatePriceRequest(stock.code, DatePriceRequest.DateType.DAY), tokenService.getAccessToken())
                    val openPrice = stockClientService.output!![0].stckOprc
                    val beforeDayHigh = stockClientService.output[1].stckHgpr
                    val beforeDayLow = stockClientService.output[1].stckLwpr
                    val tempPrice = openPrice + (beforeDayHigh - beforeDayLow) * stock.k
                    val targetPrice = (tempPrice - tempPrice % QUOTE_UNIT).toInt()

                    log.info("[목표가] ${stock.code}: $openPrice + ($beforeDayHigh - $beforeDayLow) * ${stock.k} = $targetPrice")

                    targetPriceMessage.append(
                        "${stock.name}(${stock.code})\n" +
                            "  - 시초가: ${stockClientService.output[0].stckOprc}\n" +
                            "  - 목표가: $targetPrice\n"
                    )

                    stock.code to targetPrice
                }
                slackMessageService.sendMessage(targetPriceMessage.toString())

                // 목표가 계산이후 종료
                return
            }


            // 목표가 계산
            TimeUnit.SECONDS.sleep(1)
        }
    }

    /**
     * 실시간 채결 이벤트
     */
    fun execution(wsResponse: WsResponse) {
        if (!TradeTimeHelper.isBuyTimeRange() || targetPrice.isEmpty()) {
            return
        }

        val realtimeExecution = RealtimeExecution.parsing(wsResponse.responseData)
        if (buyCode.contains(realtimeExecution.code)) {
            return
        }
        val vbsStock = bokslStockProperties.koreainvestment.vbs.stock.stream().filter { it.code == realtimeExecution.code }.findAny().orElse(null)
        if (vbsStock == null) {
            log.warn("매수 대상종목이 아닌데 실시간 체결 이벤트 수신. 종목 코드: ${realtimeExecution.code}")
            return
        }

        log.info("${wsResponse.trId} = $realtimeExecution")

        val code = realtimeExecution.code
        val targetPrice = targetPrice[code] ?: return

        if (targetPrice <= realtimeExecution.stckPrpr) {
            val deposit = balanceResponse!!.deposit[0].dncaTotAmt
            val vbsConfig = bokslStockProperties.koreainvestment.vbs
            val buyCash = ApplicationUtil.getBuyCash(buyCode.size, deposit.toDouble(), vbsConfig.stock.size, vbsConfig.investRatio).toLong()

            val ordqty = (buyCash / targetPrice).toInt()

            val message = "[매수 주문] ${vbsStock.name}(${vbsStock.code}), " +
                "주문가: ${String.format("%,d", this.targetPrice)}, " +
                "수량: ${String.format("%,d", ordqty)}"

            log.info(message)
            stockClientService.requestOrderBuy(
                OrderRequest(
                    cano = vbsConfig.accountNo,
                    code = code,
                    ordunpr = targetPrice,
                    ordqty = ordqty
                ),
                tokenService.getAccessToken()
            )
            buyCode.add(code)
            slackMessageService.sendMessage(message)
            // 주문 접수 후 딜레이
            TimeUnit.SECONDS.sleep(3)
        }
    }

    /**
     * [openSellStockList] 매도 종목
     */
    private fun sellOrder(openSellStockList: List<BokslStockProperties.Vbs.VbsStock>) {
        val holdingsMap = getHoldingStock()

        openSellStockList.forEach {
            val stock = holdingsMap[it.code] ?: return@forEach

            val quoteResponse = stockClientService.requestQuote(QuoteRequest(it.code), tokenService.getAccessToken())
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
                tokenService.getAccessToken()
            )
            buyCode.remove(it.code)
            slackMessageService.sendMessage(message)
        }
    }

    /**
     * 주식 잔고
     * @return <종목코드, 잔고정보>
     */
    private fun getHoldingStock(): Map<String, BalanceResponse.Holdings> {
        return balanceResponse!!.holdings.associateBy { it.code }
    }

    /**
     * 날짜가 변경될 경우 최초 로드
     */
    private fun checkDay() {
        if (currentDate == LocalDate.now()) {
            return
        }
        val accountNo = loadBalance()
        val cancelableStock = stockClientService.requestCancelableList(CancelableRequest(accountNo), tokenService.getAccessToken())
        initBuyCode(cancelableStock)
        currentDate = LocalDate.now()
    }

    private fun loadBalance(): String {
        val accountNo = bokslStockProperties.koreainvestment.vbs.accountNo
        balanceResponse = stockClientService.requestBalance(BalanceRequest(accountNo), tokenService.getAccessToken())
        return accountNo
    }

    /**
     * 현재 매수 또는 매수 대기중인 종목
     */
    private fun initBuyCode(cancelableStock: CommonResponse<List<CancelableResponse>>) {
        buyCode.clear()
        val holdingStock = getHoldingStock()
        buyCode.addAll(holdingStock.keys)
        cancelableStock.output!!.forEach {
            buyCode.add(it.code)
        }
    }

}
