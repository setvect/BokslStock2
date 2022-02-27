package com.setvect.bokslstock2.analysis.mabs.service

import com.setvect.bokslstock2.analysis.mabs.entity.MabsConditionEntity
import com.setvect.bokslstock2.analysis.mabs.entity.MabsTradeEntity
import com.setvect.bokslstock2.analysis.common.model.TradeType.BUY
import com.setvect.bokslstock2.analysis.common.model.TradeType.SELL
import com.setvect.bokslstock2.analysis.mabs.repository.MabsConditionRepository
import com.setvect.bokslstock2.analysis.mabs.repository.MabsTradeRepository
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 이동평균 돌파 매매 백테스트
 */
@Service
class MabsBacktestService(
    val mabsConditionRepository: MabsConditionRepository,
    val mabsTradeRepository: MabsTradeRepository,
    val movingAverageService: MovingAverageService,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 백테스트 조건 저장
     */
    fun saveCondition(mabsCondition: MabsConditionEntity) {
        mabsConditionRepository.save(mabsCondition)
    }

    /**
     * 모든 조건에 대한 백테스트 진행
     * 기존 백테스트 기록을 모두 삭제하고 다시 테스트 함
     */
    @Transactional
    fun runTestBatch() {
        val conditionList = mabsConditionRepository.findAll()
        var i = 0
        conditionList
            .filter { it.stock.code == "TQQQ" }
            .forEach {
            mabsTradeRepository.deleteByCondition(it)
            backtest(it)
            log.info("백테스트 진행 ${++i}/${conditionList.size}")
        }
    }

    @Transactional
    fun backtest(condition: MabsConditionEntity) {
        val movingAverageCandle = movingAverageService.getMovingAverage(
            condition.stock.code, condition.periodType, listOf(condition.shortPeriod, condition.longPeriod)
        )

        var lastStatus = SELL
        var highYield = 0.0
        var lowYield = 0.0
        var lastBuyInfo: MabsTradeEntity? = null

        for (idx in 2 until movingAverageCandle.size) {
            val currentCandle = movingAverageCandle[idx]
            // -1 영업일
            val yesterdayCandle = movingAverageCandle[idx - 1]
            // -2 영업일
            val beforeYesterdayCandle = movingAverageCandle[idx - 2]

            if (lastStatus == SELL) {
                // 매수 판단
                if (buyCheck(beforeYesterdayCandle, condition)) {
                    val shortFormat = String.format("%,d", yesterdayCandle.average[condition.shortPeriod])
                    val longFormat = String.format("%,d", yesterdayCandle.average[condition.longPeriod])
                    log.info("새롭게 이동평균을 돌파할 때만 매수합니다. ${yesterdayCandle.candleDateTimeStart}~${yesterdayCandle.candleDateTimeEnd} - 단기이평: $shortFormat, 장기이평: $longFormat")
                    continue
                }
                if (buyCheck(yesterdayCandle, condition)) {
                    lastBuyInfo = MabsTradeEntity(
                        mabsConditionEntity = condition,
                        tradeType = BUY,
                        highYield = 0.0,
                        lowYield = 0.0,
                        maShort = yesterdayCandle.average[condition.shortPeriod] ?: 0.0,
                        maLong = yesterdayCandle.average[condition.longPeriod] ?: 0.0,
                        yield = 0.0,
                        unitPrice = currentCandle.openPrice,
                        tradeDate = currentCandle.candleDateTimeStart
                    )
                    mabsTradeRepository.save(lastBuyInfo)
                    lastStatus = BUY
                    // 매도 판단
                    val currentCloseYield = ApplicationUtil.getYield(lastBuyInfo.unitPrice, currentCandle.closePrice)
                    highYield = 0.0.coerceAtLeast(currentCloseYield)
                    lowYield = 0.0.coerceAtMost(currentCloseYield)
                }
            } else {
                if (sellCheck(yesterdayCandle, condition)) {
                    val sellInfo = MabsTradeEntity(
                        mabsConditionEntity = condition,
                        tradeType = SELL,
                        highYield = highYield,
                        lowYield = lowYield,
                        maShort = yesterdayCandle.average[condition.shortPeriod] ?: 0.0,
                        maLong = yesterdayCandle.average[condition.longPeriod] ?: 0.0,
                        yield = ApplicationUtil.getYield(lastBuyInfo!!.unitPrice, currentCandle.openPrice),
                        unitPrice = currentCandle.openPrice,
                        tradeDate = currentCandle.candleDateTimeStart
                    )
                    mabsTradeRepository.save(sellInfo)
                    lastStatus = SELL
                    continue
                }
                val currentCloseYield = ApplicationUtil.getYield(lastBuyInfo!!.unitPrice, currentCandle.closePrice)
                highYield = highYield.coerceAtLeast(currentCloseYield)
                lowYield = lowYield.coerceAtMost(currentCloseYield)
            }
        }
    }

    /**
     * @return [candle]이 매수 조건이면 true
     */
    private fun buyCheck(candle: CandleDto, condition: MabsConditionEntity): Boolean {
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
    private fun sellCheck(candle: CandleDto, condition: MabsConditionEntity): Boolean {
        val shortValue = candle.average[condition.shortPeriod]
        val longValue = candle.average[condition.longPeriod]
        if (shortValue == null || longValue == null) {
            return false
        }
        val yieldValue = ApplicationUtil.getYield(longValue, shortValue)
        return (yieldValue * -1) > condition.downSellRate
    }
}