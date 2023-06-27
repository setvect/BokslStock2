package com.setvect.bokslstock2.analysis.mabs.service

import com.setvect.bokslstock2.analysis.common.model.TradeNeo
import com.setvect.bokslstock2.analysis.mabs.model.MabsBacktestCondition
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
    fun runTest(condition: MabsBacktestCondition): List<TradeNeo> {
        val movingAverageCandle = movingAverageService.getMovingAverage(
            condition.stockCode,
            PeriodType.PERIOD_DAY,
            condition.periodType,
            listOf(condition.shortPeriod, condition.longPeriod)
        )

        var lastStatus = SELL
        var lastBuyInfo: TradeNeo? = null
        val rtnValue: MutableList<TradeNeo> = mutableListOf()

        var currentCash = condition.cash

        for (idx in 2 until movingAverageCandle.size) {
            val currentCandle = movingAverageCandle[idx]

            // 매매기간이 지나면 종료
            if (currentCandle.candleDateTimeStart > condition.range.to) {
                log.info("매매기간이 지나면 종료: ${currentCandle.candleDateTimeStart} > ${condition.range.to}")
                break
            }

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
                    val buyCash = currentCash * condition.investRatio
                    lastBuyInfo = TradeNeo(
                        stockCode = condition.stockCode,
                        tradeType = BUY,
                        price = currentCandle.openPrice,
                        qty = (buyCash / currentCandle.openPrice).toInt(),
                        tradeDate = currentCandle.candleDateTimeStart
                    )
                    currentCash -= lastBuyInfo.price * lastBuyInfo.qty
                    rtnValue.add(lastBuyInfo)
                    lastStatus = BUY
                }
            } else {
                // 매도 판단
                if (sellCheck(currentCandle, condition)) {
                    val sellInfo = TradeNeo(
                        stockCode = condition.stockCode,
                        tradeType = SELL,
                        price = currentCandle.openPrice,
                        qty = lastBuyInfo!!.qty,
                        tradeDate = currentCandle.candleDateTimeStart
                    )
                    currentCash += sellInfo.price * sellInfo.qty
                    rtnValue.add(sellInfo)
                    lastStatus = SELL
                    continue
                }
            }
        }
        return rtnValue.toImmutableList()
    }

    /**
     * @return [candle]이 매수 조건이면 true
     */
    private fun buyCheck(candle: CandleDto, condition: MabsBacktestCondition): Boolean {
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
    private fun sellCheck(candle: CandleDto, condition: MabsBacktestCondition): Boolean {
        val shortValue = candle.average[condition.shortPeriod]
        val longValue = candle.average[condition.longPeriod]
        if (shortValue == null || longValue == null) {
            return false
        }
        val yieldValue = ApplicationUtil.getYield(longValue, shortValue)
        return (yieldValue * -1) > condition.downSellRate
    }
}