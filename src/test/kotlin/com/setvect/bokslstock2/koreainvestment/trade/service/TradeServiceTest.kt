package com.setvect.bokslstock2.koreainvestment.trade.service

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

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
}