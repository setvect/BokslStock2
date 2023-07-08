package com.setvect.bokslstock2.backtest.stoploss.service

import com.setvect.bokslstock2.backtest.common.model.TradeNeo
import com.setvect.bokslstock2.backtest.common.service.BacktestTradeService
import com.setvect.bokslstock2.backtest.stoploss.model.StopLoosBacktestCondition
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class StopLossBacktestService(
    private val backtestTradeService: BacktestTradeService,
    private val movingAverageService: MovingAverageService,
) {
    val log: Logger = LoggerFactory.getLogger(StopLossBacktestService::class.java)

    fun runTest(condition: StopLoosBacktestCondition): List<TradeNeo> {
        val movingAverageCandle = movingAverageService.getMovingAverage(
            condition.stockCode,
            PeriodType.PERIOD_DAY,
            PeriodType.PERIOD_MONTH,
        )
        adjustRange(condition)
        var currentDate = condition.getRange().from
        var currentCash = condition.cash
        val rtnValue: MutableList<TradeNeo> = mutableListOf()
        // 년도-월 기준으로 월봉 캔들 맵핑
        val cancelByYearMonth = movingAverageCandle.associateBy { it.candleDateTimeStart.withDayOfMonth(1) }
        var currentStatus = TradeType.SELL

        while (currentDate <= condition.getRange().to) {
            val currentCandle = cancelByYearMonth[currentDate]!!
            val buyCash = currentCash * condition.investRatio
            if (currentStatus == TradeType.SELL) {
                val tradeNeo = TradeNeo(
                    stockCode = condition.stockCode,
                    tradeType = TradeType.BUY,
                    price = currentCandle.openPrice,
                    qty = (buyCash / currentCandle.openPrice).toInt(),
                    tradeDate = currentCandle.candleDateTimeStart,
                )
                rtnValue.add(tradeNeo)
                currentStatus = TradeType.BUY
                currentCash -= tradeNeo.qty * tradeNeo.price
            }

            // 현재 날짜 averageMonthCount 만큼 매월 고가 - 저가 차이를 매월 구해 평균 계산
            val averageVolatility = (1..condition.averageMonthCount).map { month ->
                val date = currentDate.minusMonths(month.toLong()).withDayOfMonth(1)
                val candle = cancelByYearMonth[date]!!
                val diff = candle.highPrice - candle.closePrice
                log.info("날짜: $date, 고가: ${candle.highPrice}, 저가: ${candle.closePrice}, 차이: $diff")
                diff
            }.average()

            // 손절가 구함
            val stopLossPrice = currentCandle.openPrice - (averageVolatility * condition.stopLossRate)
            val averageVolatilityPrint = ApplicationUtil.truncateDecimal(averageVolatility, 2)
            val stopLossPricePrint = ApplicationUtil.truncateDecimal(stopLossPrice, 2)
            val message =
                "날짜: $currentDate, 시초가: ${currentCandle.openPrice}, 평균변동성: $averageVolatilityPrint, " +
                        "손절비율: ${condition.stopLossRate}, 손절가: $stopLossPricePrint"
            log.info(message)

            val candleMonth = movingAverageService.getMovingAverage(
                stockCode = condition.stockCode,
                selectPeriod = PeriodType.PERIOD_DAY,
                groupPeriod = PeriodType.PERIOD_DAY,
                dateRange = DateRange(currentDate.toLocalDate(), currentDate.plusMonths(1).minusDays(1).toLocalDate())
            )

            for (candle in candleMonth) {
                if (stopLossPrice > candle.lowPrice) {
                    val tradeNeo = TradeNeo(
                        stockCode = condition.stockCode,
                        tradeType = TradeType.SELL,
                        price = stopLossPrice,
                        qty = rtnValue.last().qty,
                        tradeDate = candle.candleDateTimeEnd,
                        memo = message,
                    )
                    rtnValue.add(tradeNeo)
                    currentStatus = TradeType.SELL
                    currentCash += tradeNeo.qty * tradeNeo.price
                    log.info("매도: it.candleDateTimeEnd")
                    break
                }
            }

            currentDate = currentDate.plusMonths(1)
        }
        return rtnValue
    }

    /**
     * @return 시세가 있는 범위로 조정
     */
    private fun adjustRange(condition: StopLoosBacktestCondition) {
        // 평균값 구하는 영역까지 포함해 계산
        val adjustBeforeRange = condition.getRange()
        var allRange = DateRange(adjustBeforeRange.from.minusMonths(condition.averageMonthCount.toLong()), condition.getRange().to)
        allRange = backtestTradeService.fitBacktestRange(mutableListOf(condition.stockCode), allRange)
        val temp = DateRange(allRange.from.plusMonths(condition.averageMonthCount.toLong()), allRange.to)

        var from = YearMonth.of(temp.from.year, temp.from.month)
        if (temp.from.dayOfMonth != 1) {
            from = from.plusMonths(1)
        }
        var to = YearMonth.of(temp.to.year, temp.to.month)
        if (temp.to.dayOfMonth != to.lengthOfMonth()) {
            to = to.minusMonths(1)
        }

        condition.from = from
        condition.to = to

        log.info("범위 조건 변경: $adjustBeforeRange -> ${condition.getRange()}")
    }
}
