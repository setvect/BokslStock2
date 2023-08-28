package com.setvect.bokslstock2.backtest.ivbs.service

import com.setvect.bokslstock2.backtest.common.model.TradeNeo
import com.setvect.bokslstock2.backtest.ivbs.model.IvbsBacktestCondition
import com.setvect.bokslstock2.backtest.ivbs.model.IvbsConditionItem
import com.setvect.bokslstock2.common.model.TradeType.BUY
import com.setvect.bokslstock2.common.model.TradeType.SELL
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import okhttp3.internal.toImmutableList
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 역 변동성 돌파전략
 * 특정조건 이하일 때 종가 매수
 * 다음날 시가 매도
 */
@Service
class IvbsBacktestService(
    val movingAverageService: MovingAverageService,
    val candleRepository: CandleRepository
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun runTest(condition: IvbsBacktestCondition): List<TradeNeo> {
        val conditionByMovingAverageCandle = getConditionByMovingAverageCandle(condition)

        val result = mutableListOf<TradeNeo>()

        // 매매기간을 하루씩 증가 시킴
        var currentDate = condition.range.fromDate
        // 해당 조건의 마지막 매수 정보 저장
        val lastBuyInfoByCondition = mutableMapOf<IvbsConditionItem, TradeNeo>()

        var currentCash = condition.cash

        while (currentDate.isBefore(condition.range.toDate) || currentDate == condition.range.toDate) {
            condition.conditionList.forEach { conditionItem ->
                val movingAverageByDate = conditionByMovingAverageCandle[conditionItem]

                val currentCandle = movingAverageByDate!![currentDate] ?: return@forEach

                // -1 영업일>
                val beforeCandle = movingAverageByDate.filterKeys { it.isBefore(currentDate) }
                    .maxByOrNull { it.key }?.value ?: return@forEach

                val lastBuyInfo = lastBuyInfoByCondition[conditionItem]
                // 매수 상태이면 매도 조건 체크
                if (lastBuyInfo != null) {
                    val targetPriceHigh = getTargetPriceHigh(beforeCandle, currentCandle, conditionItem)
                    log.info("현재 날짜: ${currentCandle.candleDateTimeStart}")

                    // 매도
                    var sellPrice = currentCandle.openPrice
                    // 5분 마다 상승/하락 체크
                    if (conditionItem.stayGapRise) { // 이 방식이 더 좋음
                        val cancelMinute5List = candleRepository.findByRange(
                            conditionItem.stockCode.code,
                            PeriodType.PERIOD_MINUTE_5,
                            currentCandle.candleDateTimeStart,
                            currentCandle.candleDateTimeEnd.withHour(23).withMinute(59)
                        )
                        log.info("5분 매도 체크: ${cancelMinute5List.size}")
                        var keepBuy = false
                        for (it in cancelMinute5List) {
                            sellPrice = it.closePrice
                            if (targetPriceHigh <= it.highPrice) {
                                log.info("[매수 유지] 목표가: $targetPriceHigh, 날짜: ${it.candleDateTime}, 고가: ${it.highPrice}")
                                keepBuy = true
                                break
                            }
                            // 분봉 종가가 시초가 대비 하락일 경우 여기서 끝냄
                            if (it.closePrice - it.openPrice < 0) {
                                break
                            }
                        }
                        if (keepBuy) {
                            return@forEach
                        }
                    }

                    val sellInfo = TradeNeo(
                        stockCode = currentCandle.stockCode,
                        tradeType = SELL,
                        price = sellPrice,
                        qty = lastBuyInfo.qty,
                        tradeDate = currentDate.atStartOfDay(),
                        backtestCondition = conditionItem.comment,
                    )

                    result.add(sellInfo)
                    lastBuyInfoByCondition.remove(conditionItem)
                    log.info("[하락 매도] 매도가: $sellPrice, 오늘시가:${currentCandle.openPrice}, 차이: ${sellPrice - currentCandle.openPrice}")
                    currentCash += sellPrice * lastBuyInfo.qty
                }

                val targetPriceLow = getTargetPriceLow(beforeCandle, currentCandle, conditionItem)
                val isTarget = targetPriceLow >= currentCandle.closePrice
                if (isTarget) {
                    // 현재 매수한 종목의 비중을 계산
                    val purchasedAllRatio = lastBuyInfoByCondition.entries.sumOf { it.key.investmentRatio }
                    // 매수에 사용할 현금을 계산
                    val buyCash = ApplicationUtil.getBuyCash(purchasedAllRatio, currentCash, conditionItem.investmentRatio, condition.investRatio)

                    val sellInfo = TradeNeo(
                        stockCode = currentCandle.stockCode,
                        tradeType = BUY,
                        price = currentCandle.closePrice,
                        qty = (buyCash / targetPriceLow).toInt(),
                        tradeDate = currentDate.atStartOfDay(),
                        backtestCondition = conditionItem.comment,
                        memo = "시가: ${currentCandle.openPrice}, 목표가: $targetPriceLow, 현재가: ${currentCandle.closePrice}"
                    )
                    result.add(sellInfo)
                    lastBuyInfoByCondition[conditionItem] = sellInfo
                    log.info("매수: ${sellInfo.stockCode}, 매수가: ${sellInfo.price}, 매수수량: ${sellInfo.qty}, 매수일: ${sellInfo.tradeDate}")

                    currentCash -= targetPriceLow * sellInfo.qty
                }
            }
            currentDate = currentDate.plusDays(1)
        }
        return result.toImmutableList()
    }

    /**
     * @return <조건, <날짜, 해당 날짜 캔들정보>>
     */
    private fun getConditionByMovingAverageCandle(condition: IvbsBacktestCondition): Map<IvbsConditionItem, Map<LocalDate, CandleDto>> {
        val conditionByMovingAverageCandle = condition.conditionList.associateWith { conditionItem ->
            val movingAverage = movingAverageService.getMovingAverage(
                conditionItem.stockCode,
                PeriodType.PERIOD_DAY,
                PeriodType.PERIOD_DAY,
                listOf(0),
                condition.range
            )
            movingAverage.associateBy { it.candleDateTimeStart.toLocalDate() }
        }
        return conditionByMovingAverageCandle
    }

    /**
     * @return 저가 매수 목표가 계산. (시가 - (전일 고가 - 전일 저가) * 변동율)
     */
    private fun getTargetPriceLow(
        beforeCandle: CandleDto,
        currentCandle: CandleDto,
        condition: IvbsConditionItem
    ): Double {
        var volatilityPrice = (beforeCandle.highPrice - beforeCandle.lowPrice) * condition.kRate
        // 호가단위 기준으로 절삭
        volatilityPrice -= (volatilityPrice % condition.unitAskPrice)
        return currentCandle.openPrice - volatilityPrice
    }

    /**
     * @return 목표가 계산
     */
    private fun getTargetPriceHigh(
        beforeCandle: CandleDto,
        currentCandle: CandleDto,
        condition: IvbsConditionItem
    ): Double {
        var volatilityPrice = (beforeCandle.highPrice - beforeCandle.lowPrice) * condition.kRate
        // 호가단위 기준으로 절삭
        volatilityPrice -= (volatilityPrice % condition.unitAskPrice)
        return currentCandle.openPrice + volatilityPrice
    }
}