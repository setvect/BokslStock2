package com.setvect.bokslstock2.analysis.mabs.service

import com.setvect.bokslstock2.analysis.mabs.model.MabsCondition
import com.setvect.bokslstock2.analysis.mabs.model.MabsTrade
import com.setvect.bokslstock2.common.model.TradeType.BUY
import com.setvect.bokslstock2.common.model.TradeType.SELL
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import okhttp3.internal.toImmutableList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 이동평균 돌파 매매 백테스트
 */
@Service
class MabsBacktestService(
    val movingAverageService: MovingAverageService,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun runTest(condition: MabsCondition): List<MabsTrade> {
        val movingAverageCandle = movingAverageService.getMovingAverage(
            condition.stock.convertStockCode(),
            PeriodType.PERIOD_DAY,
            condition.periodType,
            listOf(condition.shortPeriod, condition.longPeriod)
        )

        var lastStatus = SELL
        var highYield = 0.0
        var lowYield = 0.0
        var lastBuyInfo: MabsTrade? = null
        val rtnValue: MutableList<MabsTrade> = mutableListOf()

        for (idx in 2 until movingAverageCandle.size) {
            val currentCandle = movingAverageCandle[idx]
            // -1 영업일
            val yesterdayCandle = movingAverageCandle[idx - 1]

            if (lastStatus == SELL) {
                // 매수 판단
                if (buyCheck(yesterdayCandle, condition)) {
                    val shortFormat = String.format("%,d", currentCandle.average[condition.shortPeriod])
                    val longFormat = String.format("%,d", currentCandle.average[condition.longPeriod])
                    log.info("새롭게 이동평균을 돌파할 때만 매수합니다. ${currentCandle.candleDateTimeStart}~${currentCandle.candleDateTimeEnd} - 단기이평: $shortFormat, 장기이평: $longFormat")
                    continue
                }
                if (buyCheck(currentCandle, condition)) {
                    lastBuyInfo = MabsTrade(
                        mabsCondition = condition,
                        tradeType = BUY,
                        highYield = 0.0,
                        lowYield = 0.0,
                        maShort = currentCandle.average[condition.shortPeriod] ?: 0.0,
                        maLong = currentCandle.average[condition.longPeriod] ?: 0.0,
                        yield = 0.0,
                        unitPrice = currentCandle.openPrice,
                        tradeDate = currentCandle.candleDateTimeStart
                    )
                    rtnValue.add(lastBuyInfo)
                    lastStatus = BUY
                    // 매도 판단
                    val currentCloseYield = ApplicationUtil.getYield(lastBuyInfo.unitPrice, currentCandle.closePrice)
                    highYield = 0.0.coerceAtLeast(currentCloseYield)
                    lowYield = 0.0.coerceAtMost(currentCloseYield)
                }
            } else {
                if (sellCheck(currentCandle, condition)) {
                    val sellInfo = MabsTrade(
                        mabsCondition = condition,
                        tradeType = SELL,
                        highYield = highYield,
                        lowYield = lowYield,
                        maShort = currentCandle.average[condition.shortPeriod] ?: 0.0,
                        maLong = currentCandle.average[condition.longPeriod] ?: 0.0,
                        yield = ApplicationUtil.getYield(lastBuyInfo!!.unitPrice, currentCandle.openPrice),
                        unitPrice = currentCandle.openPrice,
                        tradeDate = currentCandle.candleDateTimeStart
                    )
                    rtnValue.add(sellInfo)
                    lastStatus = SELL
                    continue
                }
                val currentCloseYield = ApplicationUtil.getYield(lastBuyInfo!!.unitPrice, currentCandle.closePrice)
                highYield = highYield.coerceAtLeast(currentCloseYield)
                lowYield = lowYield.coerceAtMost(currentCloseYield)
            }
        }
        return rtnValue.toImmutableList()
    }

    /**
     * @return [candle]이 매수 조건이면 true
     */
    private fun buyCheck(candle: CandleDto, condition: MabsCondition): Boolean {
        val shortValue = candle.average[condition.shortPeriod]
        val longValue = candle.average[condition.longPeriod]
        if (shortValue == null || longValue == null) {
            return false
        }
        val yieldValue = ApplicationUtil.getYield(longValue, shortValue)
        return yieldValue > condition.upBuyRate
    }

    /**
     * @return [candle]이 매도 조건이면 true
     */
    private fun sellCheck(candle: CandleDto, condition: MabsCondition): Boolean {
        val shortValue = candle.average[condition.shortPeriod]
        val longValue = candle.average[condition.longPeriod]
        if (shortValue == null || longValue == null) {
            return false
        }
        val yieldValue = ApplicationUtil.getYield(longValue, shortValue)
        return (yieldValue * -1) > condition.downSellRate
    }
}