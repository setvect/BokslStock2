package com.setvect.bokslstock2.koreainvestment.trade.service

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.trade.model.request.*
import com.setvect.bokslstock2.util.JsonUtil
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalTime
import java.util.*

private const val AUTHORIZATION =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0b2tlbiIsImF1ZCI6ImYxNjQ2MDc2LTRlNzQtNDA5MC05MDgwLWJjZDc2ZWZiNTZmNiIsImlzcyI6InVub2d3IiwiZXhwIjoxNjczOTk5MTAwLCJpYXQiOjE2NzM5MTI3MDAsImp0aSI6IlBTbG1MVzEzNHhBSzRBUEdyaXRESE8wUjE1NE9sMmt2NU5DZyJ9.WLmHzayDcvCEoWWibwzysaGy8yXR_KsDglr2hGL0vf3fuffaYI6h3dHNNvzdQ85JnSRx522XCMF-Dqzopue-NA"

@SpringBootTest
@ActiveProfiles("local")
internal class StockClientServiceTest {
    @Autowired
    private lateinit var stockClientService: StockClientService

    @Autowired
    private lateinit var bokslStockProperties: BokslStockProperties

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    @Disabled
    fun requestToken() {
        val token = stockClientService.requestToken()
        log.info(token.accessToken)
    }

    @Test
    fun requestHashKey() {
        val hashkey = stockClientService.requestHashKey(mutableMapOf("A" to "B"))
        log.info(hashkey)
    }

    @Test
    fun requestCurrentPrice() {
        val currentPrice =
            stockClientService.requestCurrentPrice(CurrentPriceRequest(StockCode.KODEX_200_069500.code), AUTHORIZATION)
        log.info(currentPrice.toString())
    }

    @Test
    fun requestDatePrice() {
        val datePrice = stockClientService.requestDatePrice(
            DatePriceRequest(
                StockCode.KODEX_200_069500.code,
                DatePriceRequest.DateType.DAY
            ), AUTHORIZATION
        )
        val json = JsonUtil.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(datePrice)
        log.info(json)
    }

    @Test
    fun requestMinutePrice() {
        val stockCode = StockCode.KODEX_BANK_091170
        val minutePrice = stockClientService.requestMinutePrice(
            MinutePriceRequest(
                stockCode.code,
                LocalTime.of(9, 29, 0)
            ), AUTHORIZATION
        )
        val groupingCandleList = PriceGroupService.groupByMinute5(minutePrice, stockCode)
        val json = JsonUtil.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(groupingCandleList)
        log.info(json)
    }

    @Test
    fun requestQuote() {
        val quote = stockClientService.requestQuote(QuoteRequest(StockCode.KODEX_200_069500.code), AUTHORIZATION)
        log.info(quote.toString())
    }

    @Test
    fun requestBalance() {
        val balance = stockClientService.requestBalance(
            BalanceRequest(bokslStockProperties.koreainvestment.vbs.accountNo),
            AUTHORIZATION
        )
        log.info(balance.toString())
    }

    @Test
    fun requestOrderBuy() {
        val koreainvestment = bokslStockProperties.koreainvestment
        val request = OrderRequest(koreainvestment.vbs.accountNo, StockCode.KODEX_200_069500.code, 29_000, 1)
        val datePrice = stockClientService.requestOrderBuy(request, AUTHORIZATION)
        log.info(datePrice.toString())
    }

    @Test
    fun requestOrderSell() {
        val koreainvestment = bokslStockProperties.koreainvestment
        val request = OrderRequest(koreainvestment.vbs.accountNo, StockCode.KODEX_200_069500.code, 39_000, 1)
        val datePrice = stockClientService.requestOrderSell(request, AUTHORIZATION)
        log.info(datePrice.toString())
    }

    @Test
    fun requestCancelableList() {
        val balance = stockClientService.requestCancelableList(
            CancelableRequest(bokslStockProperties.koreainvestment.vbs.accountNo),
            AUTHORIZATION
        )
        log.info(balance.toString())
    }
}