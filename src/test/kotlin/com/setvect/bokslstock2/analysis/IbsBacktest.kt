package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.common.model.StockCode
import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.NumberUtil
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.LocalDateTime


/**
 * IBS 전략 백테스트<br>
 * 결과가 좋지 않음.
 */
@SpringBootTest
@ActiveProfiles("local")
class IbsBacktest {
    @Autowired
    private lateinit var movingAverageService: MovingAverageService

    @Autowired
    private lateinit var candleRepository: CandleRepository
    val buyThreshold = 0.1

    // 매매 수수료
    val fee = 0.00015

    @Test
    @DisplayName("간단한 IBS 전략 테스트 ")
    fun test() {
        var money = 10_000_000

        val start = LocalDateTime.of(2015, 1, 1, 0, 0)
        val end = LocalDateTime.now()
        val candleEntityList = candleRepository.findByRange(
            StockCode.KODEX_2X_122630.code,
            PeriodType.PERIOD_DAY,
            start,
            end
        )

        val tradeHistory = mutableListOf<TradeStock>()
        for (candle in candleEntityList) {
            val sell = tradeHistory.isNotEmpty() && tradeHistory.last().tradeType == TradeType.BUY
            if (sell) {
                val buy = tradeHistory.last()
                tradeHistory.add(TradeStock(TradeType.SELL, candle))

            } else {
                // IBS = (종가-저가) / (고가-저가)
                val ibsValue = (candle.closePrice - candle.lowPrice) / (candle.highPrice - candle.lowPrice)
                if (buyThreshold >= ibsValue) {
                    tradeHistory.add(TradeStock(TradeType.BUY, candle))
                }
            }
        }

        var cumulativeReturn = 1.0
        val cumulativeReturnHistory = mutableListOf<Double>()
        for (i in 0 until tradeHistory.size step 2) {
            val buy = tradeHistory[i]
            val sell = tradeHistory[i + 1]
            if (buy.tradeType != TradeType.BUY) {
                throw RuntimeException("매도 내역이 아님 $buy")
            }

            if (sell.tradeType != TradeType.SELL) {
                throw RuntimeException("매수 내역이 아님 $sell")
            }

            val rateReturn = ApplicationUtil.getYield(buy.candle.closePrice, sell.candle.closePrice) - (fee * 2)
            cumulativeReturn *= (1 + rateReturn)
            cumulativeReturnHistory.add(cumulativeReturn)
            println(
                "[${sell.candle.candleDateTime}] 매수: ${buy.candle.closePrice}, 매도: ${sell.candle.closePrice}, 수익률: ${
                    NumberUtil.percent(rateReturn * 100)
                }"
            )
        }
        println("=== 전략 결과 ===")
        println("누적 수익률: ${NumberUtil.percent((cumulativeReturn - 1) * 100)}")
        val between = Duration.between(start, end)
        val cagr = ApplicationUtil.getCagr(1.0, cumulativeReturn, between.toDays().toInt())
        println("CAGR: ${NumberUtil.percent(cagr * 100)}")
        println("MDD: ${NumberUtil.percent(ApplicationUtil.getMdd(cumulativeReturnHistory) * 100)}")
        println("매수+매도 횟수: ${tradeHistory.size}")
        println("=================\n")
        println()
        println("=== Buy & Hold 결과 ===")
        val startPrice = candleEntityList.first().closePrice
        val endPrice = candleEntityList.last().closePrice
        val holdCagr = ApplicationUtil.getCagr(startPrice, endPrice, between.toDays().toInt())
        println("누적 수익률: ${NumberUtil.percent(ApplicationUtil.getYield(startPrice, endPrice) * 100)}")
        println("CAGR: ${NumberUtil.percent(holdCagr * 100)}")
        println("MDD: ${NumberUtil.percent(ApplicationUtil.getMdd(candleEntityList.map { it.closePrice }) * 100)}")
        println("=================\n")

    }

    /**
     *
     */
    data class TradeStock(val tradeType: TradeType, val candle: CandleEntity)

    enum class TradeType {
        BUY, SELL
    }
}