package com.setvect.bokslstock2.analysis.vbs.service

import com.setvect.bokslstock2.analysis.common.model.TradeNeo
import com.setvect.bokslstock2.analysis.vbs.model.VbsCondition
import com.setvect.bokslstock2.analysis.vbs.model.VbsConditionItem
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
 * 변동성 돌파 전략
 */
@Service
class VbsBacktestService(
    val movingAverageService: MovingAverageService,
    val candleRepository: CandleRepository
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun runTest(condition: VbsCondition): List<TradeNeo> {
        val conditionByMovingAverageCandle = getConditionByMovingAverageCandle(condition)

        val result = mutableListOf<TradeNeo>()

        // 매매기간을 하루씩 증가 시킴
        var currentDate = condition.range.fromDate
        // 해당 조건의 마지막 매수 정보 저장
        val lastBuyInfoByCondition = mutableMapOf<VbsConditionItem, TradeNeo>()

        var currentCash = condition.cash

        while (currentDate.isBefore(condition.range.toDate) || currentDate == condition.range.toDate) {
            condition.conditionList.forEach { conditionItem ->
                val movingAverageByDate = conditionByMovingAverageCandle[conditionItem]

                val currentCandle = movingAverageByDate!![currentDate] ?: return@forEach

                // -1 영업일
                val beforeCandle = movingAverageByDate.filterKeys { it.isBefore(currentDate) }
                    .maxByOrNull { it.key }?.value ?: return@forEach
                val targetPrice = getTargetPrice(beforeCandle, currentCandle, conditionItem)

                val lastBuyInfo = lastBuyInfoByCondition[conditionItem]
                // 매수 상태이면 매도 조건 체크
                if (lastBuyInfo != null) {
                    log.info("현제 날짜: ${currentCandle.candleDateTimeStart}")

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
                        var keepBuy = false
                        for (it in cancelMinute5List) {
                            sellPrice = it.closePrice
                            if (targetPrice <= it.highPrice) {
                                log.info("[매수 유지] 목표가: $targetPrice, 날짜: ${it.candleDateTime}, 고가: ${it.highPrice}")
                                keepBuy = true
                                break
                            }
                            // 분봉 종가가 시초가 대비 하락일 경우 여기서 끝냄
                            // TODO 실제 운영에서는 closePrice와 openPrice 값이 같은 경우도 매도를 하고 있음. 변경 해야됨.
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
                        backtestCondition = conditionItem.comment
                    )

                    result.add(sellInfo)
                    lastBuyInfoByCondition.remove(conditionItem)
                    log.info("[하락 매도] 매도가: $sellPrice, 오늘시가:${currentCandle.openPrice}, 차이: ${sellPrice - currentCandle.openPrice}")
                    currentCash += sellPrice * lastBuyInfo.qty
                }

                // 매수 판단
                val maPrice = currentCandle.average[conditionItem.maPeriod] ?: 0.0

                val isTarget = targetPrice <= currentCandle.highPrice
                val isMa = maPrice <= targetPrice || maPrice == 0.0
                if (isTarget && isMa) {
                    // 현재 매수한 종목의 비중을 계산
                    val purchasedAllRatio = lastBuyInfoByCondition.entries.sumOf { it.key.investmentRatio }
                    // 매수에 사용할 현금을 계산
                    val buyCash = ApplicationUtil.getBuyCash(purchasedAllRatio, currentCash, conditionItem.investmentRatio, condition.investRatio)

                    val sellInfo = TradeNeo(
                        stockCode = currentCandle.stockCode,
                        tradeType = BUY,
                        price = targetPrice,
                        qty = (buyCash / targetPrice).toInt(),
                        tradeDate = currentDate.atStartOfDay(),
                        backtestCondition = conditionItem.comment
                    )
                    result.add(sellInfo)
                    lastBuyInfoByCondition[conditionItem] = sellInfo
                    log.info("매수: ${sellInfo.stockCode}, 매수가: ${sellInfo.price}, 매수수량: ${sellInfo.qty}, 매수일: ${sellInfo.tradeDate}")

                    currentCash -= targetPrice * sellInfo.qty
                }
            }
            currentDate = currentDate.plusDays(1)
        }
        return result.toImmutableList()
    }

    /**
     * @return <조건, <날짜, 해당 날짜 캔들정보>>
     */
    private fun getConditionByMovingAverageCandle(condition: VbsCondition): Map<VbsConditionItem, Map<LocalDate, CandleDto>> {
        val conditionByMovingAverageCandle = condition.conditionList.associateWith { conditionItem ->
            //  conditionItem.stayGapRise == true인 경우 5분봉 데이터를 기준으로 계산함, 데이터의 일관성을 맞추기 위함
            if (conditionItem.stayGapRise) {
                val movingAverage = movingAverageService.getMovingAverage(
                    conditionItem.stockCode,
                    PeriodType.PERIOD_MINUTE_5,
                    PeriodType.PERIOD_DAY,
                    listOf(conditionItem.maPeriod),
                    condition.range
                )
                movingAverage.associateBy { it.candleDateTimeStart.toLocalDate() }
            } else {
                val movingAverage = movingAverageService.getMovingAverage(
                    conditionItem.stockCode,
                    PeriodType.PERIOD_DAY,
                    PeriodType.PERIOD_DAY,
                    listOf(conditionItem.maPeriod),
                    condition.range
                )
                movingAverage.associateBy { it.candleDateTimeStart.toLocalDate() }
            }
        }
        return conditionByMovingAverageCandle
    }

    /**
     * @return 목표가 계산
     */
    private fun getTargetPrice(
        beforeCandle: CandleDto,
        currentCandle: CandleDto,
        condition: VbsConditionItem
    ): Double {
        var volatilityPrice = (beforeCandle.highPrice - beforeCandle.lowPrice) * condition.kRate
        // 호가단위 기준으로 절삭
        volatilityPrice -= (volatilityPrice % condition.unitAskPrice)
        return currentCandle.openPrice + volatilityPrice
    }
}