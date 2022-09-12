package com.setvect.bokslstock2.koreainvestment.vbs.service

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.koreainvestment.trade.model.request.BalanceRequest
import com.setvect.bokslstock2.koreainvestment.trade.model.response.TokenResponse
import com.setvect.bokslstock2.koreainvestment.trade.service.StockClientService
import com.setvect.bokslstock2.koreainvestment.ws.model.Quotation
import com.setvect.bokslstock2.koreainvestment.ws.model.RealtimeExecution
import com.setvect.bokslstock2.koreainvestment.ws.model.WsResponse
import com.setvect.bokslstock2.util.DateUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VbsService(
    private val stockClientService: StockClientService,
    private val bokslStockProperties: BokslStockProperties
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private var token: TokenResponse = TokenResponse("", DateUtil.currentDateTime(DateUtil.yyyy_MM_dd_HH_mm_ss), "", 0L)
    private var currentDate: LocalDate = LocalDate.now().minusDays(1)

    /**
     * 체결
     */
    fun execution(response: WsResponse) {
        val quotation = Quotation.parsing(response.responseData)
        log.info("${response.trId} = $quotation")
    }

    /**
     * 호가
     */
    fun quotation(response: WsResponse) {
        val koreainvestment = bokslStockProperties.koreainvestment
        val realtimeExecution = RealtimeExecution.parsing(response.responseData)
        log.info("${response.trId} = $realtimeExecution")
        initDay(koreainvestment)
    }

    /**
     * 날짜가 변경될 경우 최초 로드
     */
    private fun initDay(koreainvestment: BokslStockProperties.Koreainvestment) {
        if (currentDate == LocalDate.now()) {
            return
        }
        loadToken()
        val requestBalance = stockClientService.requestBalance(BalanceRequest(koreainvestment.vbs.accountNo), token.accessToken)
        currentDate = LocalDate.now()
    }

    // TODO 동시성 성능 이슈가 있을까? 일딴 넘어가기
    @Synchronized
    private fun loadToken() {
        if (token.isExpired()) {
            token = stockClientService.requestToken()
        }
    }
}
