package com.setvect.bokslstock2.koreainvestment.trade.service

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.koreainvestment.trade.model.request.*
import com.setvect.bokslstock2.util.ApplicationUtil
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
    "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0b2tlbiIsImF1ZCI6IjYyNDNhNzM0LWVlYmItNGFhZC05ODBiLThkYzEyYzhhZjNkYyIsImlzcyI6InVub2d3IiwiZXhwIjoxNjczOTEwMjMxLCJpYXQiOjE2NzM4MjM4MzEsImp0aSI6IlBTbG1MVzEzNHhBSzRBUEdyaXRESE8wUjE1NE9sMmt2NU5DZyJ9.WOuJtu6fljCkPMW7E-aLVgWdFD1b3wHELyJwgZwkJhHGeBulbCaiVjzPxRXkN_CSJ16NOeIFsr_Flpml4roOvQ"

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
                LocalTime.of(9, 11, 0)
            ), AUTHORIZATION
        )
//        val json = JsonUtil.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(minutePrice)
//        log.info(json)

        // 과거 데이터를 먼저 처리 하기 위해 reversed() 적용
        val candleGroupMap = minutePrice.output2.reversed().groupByTo(TreeMap()) {
            return@groupByTo ApplicationUtil.fitStartDateTime(PeriodType.PERIOD_MINUTE_5, it.baseTime())
        }

        val candleGroupList = candleGroupMap.entries.map { Pair(it.key, it.value) }

        val groupingCandleList = mutableListOf<CandleDto>()
        for (i in candleGroupList.indices) {
            val beforeCandle = if (i == 0) {
                candleGroupList[i]
            } else {
                candleGroupList[i - 1]
            }
            val candleGroup = candleGroupList[i]
            val candle = CandleDto(
                stockCode = stockCode,
                candleDateTimeStart = candleGroup.second.first().baseTime(),
                candleDateTimeEnd = candleGroup.second.last().baseTime(),
                beforeCandleDateTimeEnd = beforeCandle.second.last().baseTime(),
                beforeClosePrice = beforeCandle.second.last().stckPrpr.toDouble(),
                openPrice = candleGroup.second.first().stckOprc.toDouble(),
                highPrice = candleGroup.second.maxOf { p -> p.stckHgpr.toDouble() },
                lowPrice = candleGroup.second.minOf { p -> p.stckLwpr.toDouble() },
                closePrice = candleGroup.second.last().stckPrpr.toDouble(),
                periodType = PeriodType.PERIOD_MINUTE_5
            )
            groupingCandleList.add(candle)
        }

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