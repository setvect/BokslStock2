package com.setvect.bokslstock2.koreainvestment.trade.service

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.trade.model.request.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalTime

private const val AUTHORIZATION =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0b2tlbiIsImF1ZCI6IjM0Nzk2YTc3LTVhMjEtNGI3ZC1iMWZjLWJlMjI2MjI5MDczNSIsImlzcyI6InVub2d3IiwiZXhwIjoxNjczNzI5OTYwLCJpYXQiOjE2NzM2NDM1NjAsImp0aSI6IlBTbG1MVzEzNHhBSzRBUEdyaXRESE8wUjE1NE9sMmt2NU5DZyJ9.IIwxuJf2ABkrpQFc6LnPqNOgC_ecBb8zkz27TusKlV98NLgnHpajNboM_hFiHVFQ1p1lcWE7ZUFGMShhXsflkw"

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
        log.info(datePrice.toString())
    }

    @Test
    fun requestMinutePrice() {
        val datePrice = stockClientService.requestMinutePrice(
            MinutePriceRequest(
                StockCode.KODEX_200_069500.code,
                LocalTime.of(10, 10, 0)
            ), AUTHORIZATION
        )
        log.info(datePrice.toString())
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