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
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

private const val AUTHORIZATION =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0b2tlbiIsImF1ZCI6ImFlZTJhZTA4LWU5YmQtNDZiZS1hNmYwLWZlNjA4MGFlZjhkYyIsImlzcyI6InVub2d3IiwiZXhwIjoxNjc0NjA2NjQ5LCJpYXQiOjE2NzQ1MjAyNDksImp0aSI6IlBTbG1MVzEzNHhBSzRBUEdyaXRESE8wUjE1NE9sMmt2NU5DZyJ9.Q4Dums66BIRI-Fm1ITCx-fQ_r5FvmuRolwXE8BbvC3wcjDbRjP3wI4RI1ToyhWl1_WRNQihqCiEIBv7a3e878w"

@SpringBootTest
@ActiveProfiles("test")
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
        val now = LocalTime.of(10,10,0)
        val minutePrice = stockClientService.requestMinutePrice(
            MinutePriceRequest(
                stockCode.code,
                now
            ), AUTHORIZATION
        )

        val groupingCandleList = PriceGroupService.groupByMinute5(minutePrice, stockCode)
        val json = JsonUtil.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(groupingCandleList)
        log.info(json)
        log.info("now: $now")
        log.info("직전 5분봉: ${groupingCandleList[groupingCandleList.size - 2]}")
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

    @Test
    fun requestHoliday() {
        val result = stockClientService.requestHoliday(
            HolidayRequest(LocalDate.now()),
            AUTHORIZATION
        )
        log.info(result.toString())

        result.output!!.forEach {
            log.info("${it.date()} : ${it.isBusinessDay()}, ${it.isHoliday()}")
        }
    }
}