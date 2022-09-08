package com.setvect.bokslstock2.koreainvestment.trade.service

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.trade.model.request.BalanceRequest
import com.setvect.bokslstock2.koreainvestment.trade.model.request.CurrentPriceRequest
import com.setvect.bokslstock2.koreainvestment.trade.model.request.DatePriceRequest
import com.setvect.bokslstock2.koreainvestment.trade.model.request.OrderRequest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

private const val AUTHORIZATION =
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0b2tlbiIsImF1ZCI6ImFiYzEzOTkwLTM1M2YtNGNiMi04Mzk2LWNlMTNiNGFiNWY5ZCIsImlzcyI6InVub2d3IiwiZXhwIjoxNjYyNjgyNzYyLCJpYXQiOjE2NjI1OTYzNjIsImp0aSI6IlBTbG1MVzEzNHhBSzRBUEdyaXRESE8wUjE1NE9sMmt2NU5DZyJ9.q5hwHvC9hro24fwIR5yn2SPfQywr1H_2_vj5d0Egiox75kwwbt2wsQb8bOpE900k62rICm-rDtbEeoW6E1fXFQ"

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
        val currentPrice = stockClientService.requestCurrentPrice(CurrentPriceRequest(StockCode.KODEX_200_069500.code), AUTHORIZATION)
        log.info(currentPrice.toString())
    }

    @Test
    fun requestDatePrice() {
        val datePrice = stockClientService.requestDatePrice(DatePriceRequest(StockCode.KODEX_200_069500.code, DatePriceRequest.DateType.DAY), AUTHORIZATION)
        log.info(datePrice.toString())
    }

    @Test
    fun requestBalance() {
        val datePrice = stockClientService.requestBalance(BalanceRequest(bokslStockProperties.koreainvestment.vbs.accountNo), AUTHORIZATION)
        log.info(datePrice.toString())
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
}