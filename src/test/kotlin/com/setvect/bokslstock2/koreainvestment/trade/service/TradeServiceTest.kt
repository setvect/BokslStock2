package com.setvect.bokslstock2.koreainvestment.trade.service

import com.setvect.bokslstock2.koreainvestment.trade.model.request.CurrentPriceRequest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

private const val AUTHORIZATION = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0b2tlbiIsImF1ZCI6ImFiYzEzOTkwLTM1M2YtNGNiMi04Mzk2LWNlMTNiNGFiNWY5ZCIsImlzcyI6InVub2d3IiwiZXhwIjoxNjYyNjgyNzYyLCJpYXQiOjE2NjI1OTYzNjIsImp0aSI6IlBTbG1MVzEzNHhBSzRBUEdyaXRESE8wUjE1NE9sMmt2NU5DZyJ9.q5hwHvC9hro24fwIR5yn2SPfQywr1H_2_vj5d0Egiox75kwwbt2wsQb8bOpE900k62rICm-rDtbEeoW6E1fXFQ"

@SpringBootTest
@ActiveProfiles("local")
internal class TradeServiceTest {
    @Autowired
    private lateinit var tradeService: TradeService

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    @Disabled
    fun getToken() {
        val token = tradeService.getToken()
        log.info(token.accessToken)
    }

    @Test
    fun getHashKey() {
        val hashkey = tradeService.getHashKey(mutableMapOf("A" to "B"))
        log.info(hashkey)
    }

    @Test
    fun getCurrentPrice(){
        val currentPrice = tradeService.getCurrentPrice(CurrentPriceRequest(("069500")), AUTHORIZATION)
        log.info(currentPrice.toString())
    }

}