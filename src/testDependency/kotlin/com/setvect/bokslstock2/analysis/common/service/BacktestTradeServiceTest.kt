package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.model.PreTrade
import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.analysis.common.model.Trade
import com.setvect.bokslstock2.analysis.common.model.TradeCondition
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.DateUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class BacktestTradeServiceTest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var backtestTradeService: BacktestTradeService

    @Test
    fun analysis() {
        val preTrade1 =
            PreTrade(
                conditionName = "KODEX_200 매매",
                stockCode = StockCode.KODEX_200_069500,
                tradeType = TradeType.BUY,
                yield = 0.0,
                unitPrice = 29_000.0,
                tradeDate = DateUtil.getLocalDate("2023-01-02", "yyyy-MM-dd").atStartOfDay()
            )
        val trade1 = Trade(preTrade = preTrade1, qty = 10, cash = 500_000.0, feePrice = 100.0, gains = 0.0, stockEvalPrice = 290_000.0)

        val preTrade2 =
            PreTrade(
                conditionName = "KODEX_200 매매",
                stockCode = StockCode.KODEX_200_069500,
                tradeType = TradeType.SELL,
                yield = 0.1,
                unitPrice = 30_000.0,
                tradeDate = DateUtil.getLocalDate("2023-01-04", "yyyy-MM-dd").atStartOfDay()
            )
        val trade2 = Trade(preTrade = preTrade2, qty = 10, cash = 800_000.0, feePrice = 100.0, gains = 10_000.0, stockEvalPrice = 0.0)
        val trades = listOf(trade1, trade2)
        val condition = TradeCondition(
            range = DateRange("2023-01-01T00:00:00", "2023-06-01T00:00:00"),
            investRatio = 0.999,
            cash = 800_000.0,
            feeBuy = 0.001,
            feeSell = 0.001,
            comment = "",
            benchmark = listOf(StockCode.KODEX_200_069500)
        )
        val holdStockCodes = listOf(StockCode.KODEX_200_069500)
        val analysisResult = backtestTradeService.analysis(trades, condition, holdStockCodes)

        val result = ReportMakerHelperService.createSummary(
            analysisResult.common,
            listOf("KODEX_200 매매"),
            condition,
            ""
        )
        println(result)
    }
}