package com.setvect.bokslstock2.koreainvestment.vbs.service

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.trade.entity.AssetHistoryEntity
import com.setvect.bokslstock2.koreainvestment.trade.entity.TradeEntity
import com.setvect.bokslstock2.koreainvestment.trade.model.request.*
import com.setvect.bokslstock2.koreainvestment.trade.model.response.BalanceResponse
import com.setvect.bokslstock2.koreainvestment.trade.repository.AssetHistoryRepository
import com.setvect.bokslstock2.koreainvestment.trade.repository.TradeRepository
import com.setvect.bokslstock2.koreainvestment.trade.service.PriceGroupService
import com.setvect.bokslstock2.koreainvestment.trade.service.StockClientService
import com.setvect.bokslstock2.koreainvestment.trade.service.TokenService
import com.setvect.bokslstock2.koreainvestment.ws.model.RealtimeExecution
import com.setvect.bokslstock2.koreainvestment.ws.model.WsResponse
import com.setvect.bokslstock2.koreainvestment.ws.service.TradeTimeHelper
import com.setvect.bokslstock2.slack.SlackMessageService
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.NumberUtil.comma
import com.setvect.bokslstock2.util.NumberUtil.percent
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import java.util.concurrent.TimeUnit


/** 매도호가 차이(원) */
private const val SELL_DIFF = 15

/** ETF 호가 단위(원)*/
private const val QUOTE_UNIT = 5

/**
 * 매수 주문 에러 발생 이후 해당 시간동안 재주문 하지 않음
 */
private const val DIFF_MINUTES = 2L

@Service
class VbsService(
    private val stockClientService: StockClientService,
    private val bokslStockProperties: BokslStockProperties,
    private val slackMessageService: SlackMessageService,
    private val tokenService: TokenService,
    private val tradeRepository: TradeRepository,
    private val assetHistoryRepository: AssetHistoryRepository,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private var currentDate: LocalDate = LocalDate.now().minusDays(1)

    /** true이면 currentDate가 가르키는 날 휴장*/
    private var todayClosed = true

    // TODO 속성으로 사용하지 않고 지역변수로 사용하도록 변경할 필요가 있을것 같은데 검토
    private var balanceResponse: BalanceResponse? = null

    /** 매수 또는 매수 대기중인 종목 */
    private val buyCode = mutableSetOf<String>()

    private var run = false

    /**목표가 <종목코드, 매수가격>*/
    private var targetPriceMap = mapOf<String, Int>()

    /**
     * 목표가 돌파 여부 <종목코드, 돌파 여부>
     * 여기서 말하는 '돌파'는 이상(more)다
     * 하루단위로 측정함
     */
    private var overTargetPriceCheck = mutableMapOf<String, Boolean>()

    /**직전 채결가격. 가격 변화를 로그로 찍기 위한 목적 <종목코드, 매수가격>*/
    private var beforePriceMap = mutableMapOf<String, Int>()

    /** 주문 에러가 발생한 시간 <종목코드, 에러 발생 시간>*/
    private val errorOccursTime = mutableMapOf<String, LocalDateTime>()

    @Async(value = "applicationTaskExecutor")
    fun start() {
        // TODO 동기화 문제가 있을 수 있음
        if (run) {
            log.info("이미 시작중")
            return
        } else {
            log.info("매매 시작")
            run = true
        }
        todayClosed = false

        checkDay()

        try {
            if (!TradeTimeHelper.isTimeToTrade()) {
                log.info("매매 가능시간 아님")
                return
            }
            if (!isTradingDay()) {
                log.info("휴장일입니다.")
                slackMessageService.sendMessage("휴장일입니다.")
                todayClosed = true
                return
            }
            sellSimultaneousPrice()
            targetPriceMap = getTargetPrice()
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

        var total = 0L
        val stockInfo = balanceResponse!!.holdings.map { stock ->
            total += stock.evluAmt
            return@map "- ${stock.prdtName}(${stock.code})\n" +
                    "  ㆍ수량: ${comma(stock.hldgQty)}\n" +
                    "  ㆍ수익률: ${percent(stock.evluPflsRt)}\n" +
                    "  ㆍ매입금액: ${comma(stock.pchsAmt)}\n" +
                    "  ㆍ평가금액: ${comma(stock.evluAmt)}\n" +
                    "  ㆍ평가손익: ${comma(stock.evluPflsAmt)}\n"
        }.joinToString("\n")

        val deposit = balanceResponse!!.deposit[0].prvsRcdlExccAmt
        total += deposit
        val message = "<장 종료 리포트>\n- 전체금액: ${comma(total)}\n- 예수금: ${comma(deposit)}\n$stockInfo"
        log.info(message)
        slackMessageService.sendMessage(message)

        saveAssetStatus()
    }

    /**
     * 보유 종목과 예수금 정보를 기록함
     */
    private fun saveAssetStatus() {
        val regDate = LocalDateTime.now()
        balanceResponse!!.holdings.forEach { stock ->
            val assetHistory = AssetHistoryEntity(
                account = DigestUtils.md5Hex(bokslStockProperties.koreainvestment.vbs.accountNo),
                assetCode = stock.code,
                investment = stock.evluAmt.toDouble(),
                yield = stock.evluPflsRt / 100.0,
                regDate = regDate
            )
            assetHistoryRepository.save(assetHistory)
        }
        val assetHistory = AssetHistoryEntity(
            account = DigestUtils.md5Hex(bokslStockProperties.koreainvestment.vbs.accountNo),
            assetCode = AssetHistoryEntity.DEPOSIT,
            investment = balanceResponse!!.deposit[0].prvsRcdlExccAmt.toDouble(),
            yield = 0.0,
            regDate = regDate
        )
        assetHistoryRepository.save(assetHistory)
    }

    /**
     * 장 시작 동시호가
     */
    private fun sellSimultaneousPrice() {
        while (true) {
            if (TradeTimeHelper.isAfterOpen()) {
                log.info("장시작 이후는 동시호가 매도 하지 않음")
                return
            }

            // 장시작 동시호가 매도
            val vbsStocks = bokslStockProperties.koreainvestment.vbs.stock
            if (!TradeTimeHelper.isOpenPriceSellTime()) {
                log.debug("동시호가 매도 범위 아님. 대기함")
                TimeUnit.MILLISECONDS.sleep(500)
                continue
            }

            val openSellStockList = vbsStocks.filter { !it.stayGapRise }
            log.info("stayGapRise == false인 종목 매도: $openSellStockList")
            sellOrder(openSellStockList)

            val gapDropSellStockList = vbsStocks.filter { it.stayGapRise }
                .filter {
                    val bidPrice = getBidPrice(it.code)
                    val dayPriceCandle = stockClientService.requestDatePrice(
                        DatePriceRequest(it.code, DatePriceRequest.DateType.DAY),
                        tokenService.getAccessToken()
                    )
                    val previousClosePrice = dayPriceCandle.output!![1].stckClpr
                    log.info("${it.getName()}(${it.code}) 전일종가: $previousClosePrice, 예상체결가: $bidPrice")
                    // 예상체결가가 전일 종가보다 낮으면 매도
                    return@filter bidPrice <= previousClosePrice
                }
            log.info("stayGapRise == true AND 예상 체결가가 전일 종가 이하 -> 매도 : $gapDropSellStockList")
            sellOrder(gapDropSellStockList)
            return
        }
    }

    /**
     * 목표가 계산
     * @return <종목코드, 목표가>
     */
    private fun getTargetPrice(): Map<String, Int> {
        val vbsStocks = bokslStockProperties.koreainvestment.vbs.stock

        while (true) {
            if (!TradeTimeHelper.isBuyTimeRange()) {
                log.info("목표가 계산 기간 아님")
                TimeUnit.SECONDS.sleep(3)
                continue
            }

            val targetPriceMessage = StringBuilder()
            val targetPrice = vbsStocks.associate { stock ->
                val dayPriceCandle = stockClientService.requestDatePrice(
                    DatePriceRequest(stock.code, DatePriceRequest.DateType.DAY),
                    tokenService.getAccessToken()
                )
                val openPrice = dayPriceCandle.output!![0].stckOprc
                val beforeDayHigh = dayPriceCandle.output[1].stckHgpr
                val beforeDayLow = dayPriceCandle.output[1].stckLwpr
                val tempPrice = openPrice + (beforeDayHigh - beforeDayLow) * stock.k
                val targetPrice = (tempPrice - tempPrice % QUOTE_UNIT).toInt()

                log.info("[목표가] ${stock.code}: $openPrice + ($beforeDayHigh - $beforeDayLow) * ${stock.k} = $targetPrice")

                targetPriceMessage.append(
                    "${stock.getName()}(${stock.code})\n" +
                            "  - 시초가: ${comma(dayPriceCandle.output[0].stckOprc)}\n" +
                            "  - 목표가: ${comma(targetPrice)}\n"
                )

                stock.code to targetPrice
            }
            log.info(targetPriceMessage.toString())
            slackMessageService.sendMessage(targetPriceMessage.toString())
            return targetPrice
        }
    }

    /**
     * 실시간 채결 이벤트 수신
     */
    fun execution(wsResponse: WsResponse) {
        if (!TradeTimeHelper.isBuyTimeRange() || targetPriceMap.isEmpty()) {
            return
        }

        val realtimeExecution = RealtimeExecution.parsing(wsResponse.responseData)
        val vbsStock =
            bokslStockProperties.koreainvestment.vbs.stock.stream().filter { it.code == realtimeExecution.code }
                .findAny().orElse(null)
        if (vbsStock == null) {
            log.warn("매수 대상종목이 아닌데 실시간 체결 이벤트 수신. 종목 코드: ${realtimeExecution.code}")
            return
        }

        val targetPrice = targetPriceMap[vbsStock.code] ?: return
        log.debug("[${vbsStock.code}] 목표가: $targetPrice, 최고가: ${realtimeExecution.stckHgpr}")
        // 오늘 최고가가 목표가를 돌파 여부 체크
        if (targetPrice <= realtimeExecution.stckHgpr) {
            overTargetPriceCheck[vbsStock.code] = true
        }

        logChangePrice(realtimeExecution, vbsStock)
        if (buyCode.contains(realtimeExecution.code)) {
            return
        }

        log.debug("${wsResponse.trId} = $realtimeExecution")

        val targetPriceExceeded = targetPrice <= realtimeExecution.stckPrpr

        if (targetPriceExceeded) {
            val errorWait = Optional.ofNullable(errorOccursTime[vbsStock.code])
                .map { LocalDateTime.now().isBefore(it.plusMinutes(DIFF_MINUTES)) }
                .orElse(false)

            if (errorWait) {
                log.warn("매수 주문 에러 후 ${errorOccursTime[vbsStock.code]?.plusMinutes(DIFF_MINUTES)}동안 재 주문 할 수 없음. 해당종목 종목: ${vbsStock.getName()}")
            } else {
                buyOrder(vbsStock, targetPrice)
            }
        }
    }

    /**
     * @return true: 영업일, false: 휴장일
     */
    private fun isTradingDay(): Boolean {
        val requestHoliday = stockClientService.requestHoliday(
            HolidayRequest(LocalDate.now()),
            tokenService.getAccessToken()
        )
        return requestHoliday.output!![0].isBusinessDay()
    }

    /**
     * 가격이 변화면 로그 기록
     */
    private fun logChangePrice(
        realtimeExecution: RealtimeExecution,
        vbsStock: BokslStockProperties.Vbs.VbsStock
    ) {
        val beforePrice = beforePriceMap.getOrDefault(vbsStock.code, 0)
        if (beforePrice != realtimeExecution.stckPrpr) {
            log.info(
                "${vbsStock.getName()}(${vbsStock.code}): ${comma(beforePrice)} -> ${comma(realtimeExecution.stckPrpr)} (${
                    percent(
                        realtimeExecution.prdyCtrt
                    )
                }) (매수 목표가: ${comma(targetPriceMap[vbsStock.code] ?: 0)}, 오늘 최고가: ${comma(realtimeExecution.stckHgpr)})"
            )
            beforePriceMap[vbsStock.code] = realtimeExecution.stckPrpr
        }
    }

    /**
     * 매수 주문
     */
    private fun buyOrder(vbsStock: BokslStockProperties.Vbs.VbsStock, targetPrice: Int) {
        loadBalance()

        val deposit = balanceResponse!!.deposit[0].prvsRcdlExccAmt
        val vbsConfig = bokslStockProperties.koreainvestment.vbs
        val buyCash =
            ApplicationUtil.getBuyCash(buyCode.size, deposit.toDouble(), vbsConfig.stock.size, vbsConfig.investRatio)
                .toLong()

        val ordqty = (buyCash / targetPrice).toInt()

        val message = "[매수 주문] ${vbsStock.getName()}(${vbsStock.code}), " +
                "주문가: ${comma(targetPrice)}, " +
                "수량: ${comma(ordqty)}"

        log.info(message)

        val requestOrderBuy = stockClientService.requestOrderBuy(
            OrderRequest(
                cano = vbsConfig.accountNo,
                code = vbsStock.code,
                ordunpr = targetPrice,
                ordqty = ordqty
            ),
            tokenService.getAccessToken()
        )

        if (requestOrderBuy.isError()) {
            log.info("주문요청 에러: $requestOrderBuy")
            slackMessageService.sendMessage("@channel 주문요청 에러: $requestOrderBuy")
            errorOccursTime[vbsStock.code] = LocalDateTime.now()
            TimeUnit.SECONDS.sleep(2)
        } else {
            buyCode.add(vbsStock.code)
            log.info("주문요청 응답: $requestOrderBuy")

            val tradeEntity = TradeEntity(
                account = DigestUtils.md5Hex(vbsConfig.accountNo),
                code = vbsStock.code,
                tradeType = TradeType.BUY,
                qty = ordqty,
                unitPrice = targetPrice.toDouble(),
                yield = 0.0,
                regDate = LocalDateTime.now()
            )
            tradeRepository.save(tradeEntity)
            TimeUnit.SECONDS.sleep(2)
        }
        slackMessageService.sendMessage(message)
    }

    /**
     * 매도 주문
     * [openSellStockList] 매도 종목
     */
    private fun sellOrder(openSellStockList: List<BokslStockProperties.Vbs.VbsStock>) {
        val holdingsMap = getHoldingStock(false)

        openSellStockList.forEach {
            val stock = holdingsMap[it.code] ?: return@forEach
            sellOrder(stock)
        }
    }

    /**
     * 매도 주문. 만약 잔고가 0이면 매도 하지 않음
     */
    private fun sellOrder(stock: BalanceResponse.Holdings) {
        if (stock.hldgQty == 0) {
            log.warn("보유수량이 없는 종목을 매도 요청했음. $stock")
            return
        }
        val bidPrice = getBidPrice(stock.code)
        val sellPrice = bidPrice - SELL_DIFF

        val yieldValue = ApplicationUtil.getYield(stock.pchsAvgPric.toInt(), bidPrice)
        val message = "[매도 주문] ${stock.prdtName}(${stock.code}), " +
                "현재가: ${comma(bidPrice)}, " +
                "주문가: ${comma(sellPrice)}, " +
                "매수평단가: ${comma(stock.pchsAvgPric.toInt())}, " +
                "수량: ${comma(stock.hldgQty)}, " +
                "수익률(추정): ${percent(yieldValue * 100)}, "
        "수익금(추정): ${stock.evluAmt * yieldValue}"
        log.info(message)

        val accountNo = bokslStockProperties.koreainvestment.vbs.accountNo
        val requestOrderSell = stockClientService.requestOrderSell(
            OrderRequest(
                cano = accountNo,
                code = stock.code,
                ordunpr = sellPrice,
                ordqty = stock.hldgQty
            ),
            tokenService.getAccessToken()
        )
        if (requestOrderSell.isError()) {
            log.error("주문요청 에러: $requestOrderSell")
            slackMessageService.sendMessage("@channel 주문요청 에러: $requestOrderSell")
        } else {
            buyCode.remove(stock.code)
            log.info("주문요청 응답: $requestOrderSell")

            val tradeEntity = TradeEntity(
                account = DigestUtils.md5Hex(accountNo),
                code = stock.code,
                tradeType = TradeType.SELL,
                qty = stock.hldgQty,
                // TODO 채결 기준이 아니라 주문 기준이라 가격이 정확하지 않음
                unitPrice = bidPrice.toDouble(),
                yield = yieldValue,
                regDate = LocalDateTime.now()
            )
            tradeRepository.save(tradeEntity)
            slackMessageService.sendMessage(message)
        }
    }


    /**
     * 현재 시간대에 따라 얻어오는 방식이 다름
     * - 오전 통시호가: 체곌 예상
     * - 그외: 마지막 채결 가격
     * @return 메수 호가
     */
    private fun getBidPrice(code: String): Int {
        return if (TradeTimeHelper.isMorningSimultaneity()) {
            val quoteResponse = stockClientService.requestQuote(QuoteRequest(code), tokenService.getAccessToken())
            quoteResponse.output2!!.expectedPrice
        } else {
            val requestCurrentPrice =
                stockClientService.requestCurrentPrice(CurrentPriceRequest(code), tokenService.getAccessToken())
            requestCurrentPrice.output!!.stckPrpr
        }
    }

    /**
     * 주식 잔고
     * [reloadBalance] true면 계좌 잔고를 조회, false면 기존 조회한 값을 그대로 반환
     * @return <종목코드, 잔고정보>
     */
    private fun getHoldingStock(reloadBalance: Boolean): Map<String, BalanceResponse.Holdings> {
        if (reloadBalance) {
            loadBalance()
        }
        return balanceResponse!!.holdings.associateBy { it.code }
    }

    /**
     * 날짜가 변경될 경우 최초 로드
     */
    private fun checkDay() {
        if (currentDate == LocalDate.now()) {
            return
        }
        loadBalance()
        initBuyCode()
        sendBalance()

        overTargetPriceCheck = bokslStockProperties.koreainvestment.vbs.stock
            .associate { it.code to false } as MutableMap<String, Boolean>

        currentDate = LocalDate.now()
    }

    /**
     * 잔고 정보 슬랙 전송
     */
    private fun sendBalance() {
        val message = StringBuilder()
        message.append("예수금(D+2): ${comma(balanceResponse!!.deposit[0].prvsRcdlExccAmt)}\n")

        val balanceMessage = balanceResponse!!.holdings.joinToString("\n") {
            "보유종목: ${StockCode.findByCodeOrNull(it.code)?.desc}(${it.code}), " +
                    "수량 ${comma(it.hldgQty)}, " +
                    "수익률 ${percent(it.evluPflsRt)}, " +
                    "매입금액 ${comma(it.pchsAmt)}, " +
                    "평가금액 ${comma(it.evluAmt)}"
        }
        message.append(balanceMessage)
        log.info(message.toString())
        slackMessageService.sendMessage(message.toString())
    }

    /**
     * 잔고 조회
     */
    private fun loadBalance() {
        val accountNo = bokslStockProperties.koreainvestment.vbs.accountNo
        balanceResponse = stockClientService.requestBalance(BalanceRequest(accountNo), tokenService.getAccessToken())
    }

    /**
     * 현재 매수 또는 매수 대기중인 종목
     */
    private fun initBuyCode() {
        buyCode.clear()
        val accountNo = bokslStockProperties.koreainvestment.vbs.accountNo
        val cancelableStock =
            stockClientService.requestCancelableList(CancelableRequest(accountNo), tokenService.getAccessToken())
        val holdingStock = getHoldingStock(false)
        // 잔고가 1이상인 경우만 보유 주식으로 인정
        val hasStock = holdingStock.entries.filter { it.value.hldgQty >= 1 }.map { it.key }
        buyCode.addAll(hasStock)
        cancelableStock.output!!.forEach {
            buyCode.add(it.code)
        }
    }

    /** 5분마다 실행되는 매도 체크 */
    fun sellCheck() {
        log.info("매도 체크")
        if (todayClosed) {
            log.info("휴장")
            return
        }

        if (!TradeTimeHelper.isStayGapRiseTimeRange()) {
            log.info("매도 가능시간 아님")
            return
        }
        val holdingStock = getHoldingStock(true)
        senderPurchaseStock(holdingStock)

        bokslStockProperties.koreainvestment.vbs.stock.forEach {
            val stock = holdingStock[it.code] ?: return@forEach
            if (stock.hldgQty == 0) {
                log.info("${it.code}] 보유 수량 없음")
                return@forEach
            }

            // 오늘 매수한 종목은 다시 매도하지 않음
            if (stock.thdtBuyqty != 0) {
                log.info("${it.code}] 오늘 매수한 종목. 매도 하지 않음")
                return@forEach
            }

            // 오늘 최고가가 목표가 이상이면 매도 하지 않음
            log.info("종목별 오늘 최고가: $targetPriceMap")
            if (overTargetPriceCheck[stock.code] == true) {
                log.info("${it.code}] 목표가를 넘겼음. 매도 하지 않음")
                return@forEach
            }

            val now = LocalTime.now()
            val minutePrice = stockClientService.requestMinutePrice(
                MinutePriceRequest(
                    it.code,
                    now
                ), tokenService.getAccessToken()
            )
            val groupingCandleList = PriceGroupService.groupByMinute5(minutePrice, StockCode.findByCode(it.code))
            // 09:05, 09:10, 09:15, ... 이런식으로 호출 
            // 끝에서 2번째가 직전 5분봉
            val beforeCandle = groupingCandleList[groupingCandleList.size - 2]
            log.info("호출 시간: $now")
            log.info("${it.code}] 직전 5분봉: $beforeCandle")

            val belowOpeningPrice = beforeCandle.openPrice >= beforeCandle.closePrice
            log.info("[${it.code}] $beforeCandle.openPrice >= $beforeCandle.closePrice = $belowOpeningPrice")
            if (belowOpeningPrice) {
                log.info("${it.code} 매도")
                sellOrder(stock)
            } else {
                log.info("${it.code} 매수상태 유지")
            }
        }
    }

    /**
     * 매수 잔고 슬랙 전달
     * 매수 종목이 없으면 메시지 전달하지 않음
     */
    private fun senderPurchaseStock(holdingStock: Map<String, BalanceResponse.Holdings>) {
        if (holdingStock.isEmpty()) {
            return
        }

        if (holdingStock.entries.none { it.value.hldgQty != 0 }) {
            return
        }

        val stockInfo = holdingStock.entries.map { entry ->
            return@map "- ${entry.value.prdtName}(${entry.value.code}), 수익률: ${percent(entry.value.evluPflsRt)}, 평가손익: ${comma(entry.value.evluPflsAmt)}"
        }.joinToString("\n")
        if (StringUtils.isNotBlank(stockInfo)) {
            slackMessageService.sendMessage(stockInfo)
        }
    }
}
