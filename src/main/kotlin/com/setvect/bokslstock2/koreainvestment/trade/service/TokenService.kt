package com.setvect.bokslstock2.koreainvestment.trade.service

import com.setvect.bokslstock2.koreainvestment.trade.model.response.TokenResponse
import com.setvect.bokslstock2.util.DateUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TokenService(
    private val stockClientService: StockClientService,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    private var token: TokenResponse = TokenResponse("", DateUtil.currentDateTime(DateUtil.yyyy_MM_dd_HH_mm_ss), "", 0L)
    private var currentDate: LocalDate = LocalDate.now().minusDays(1)

    @Synchronized
    fun getAccessToken(): String {
        if (currentDate != LocalDate.now()) {
            loadToken()
            currentDate = LocalDate.now()
        }

        return token.accessToken
    }

    private fun loadToken() {
        token = stockClientService.requestToken()
        log.info("load token: ${token.accessToken}")
    }
}