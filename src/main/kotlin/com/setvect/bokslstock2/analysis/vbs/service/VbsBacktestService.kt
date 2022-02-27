package com.setvect.bokslstock2.analysis.vbs.service

import com.setvect.bokslstock2.analysis.common.model.TradeType.BUY
import com.setvect.bokslstock2.analysis.common.model.TradeType.SELL
import com.setvect.bokslstock2.analysis.vbs.entity.VbsConditionEntity
import com.setvect.bokslstock2.analysis.vbs.entity.VbsTradeEntity
import com.setvect.bokslstock2.analysis.vbs.repository.VbsConditionRepository
import com.setvect.bokslstock2.analysis.vbs.repository.VbsTradeRepository
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 변동성 돌파 전략
 */
@Service
class VbsBacktestService(
    val vbsConditionRepository: VbsConditionRepository,
    val vbsTradeRepository: VbsTradeRepository,
    val movingAverageService: MovingAverageService,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 백테스트 조건 저장
     */
    fun saveCondition(vbsCondition: VbsConditionEntity) {
        vbsConditionRepository.save(vbsCondition)
    }

    /**
     * 모든 조건에 대한 백테스트 진행
     * 기존 백테스트 기록을 모두 삭제하고 다시 테스트 함
     */
    @Transactional
    fun runTestBatch() {
        val conditionList = vbsConditionRepository.findAll()
        var i = 0
        conditionList
            .forEach {
                vbsTradeRepository.deleteByCondition(it)
                backtest(it)
                log.info("백테스트 진행 ${++i}/${conditionList.size}")
            }
    }

    @Transactional
    fun backtest(condition: VbsConditionEntity) {
        val movingAverageCandle = movingAverageService.getMovingAverage(
            condition.stock.code, condition.periodType, listOf(condition.maPeriod)
        )

        var lastBuyInfo: VbsTradeEntity? = null

        for (idx in 1 until movingAverageCandle.size) {
            val currentCandle = movingAverageCandle[idx]
            // -1 영업일
            val beforeCandle = movingAverageCandle[idx - 1]
            var sell = false
            if (lastBuyInfo != null) {
                // 갭 상승 시 매도 통과 조건이면, 전일 고가 보다 오늘 시초가가 높은 경우 오늘 매도 하지 않음
                if (condition.gapRisenSkip && beforeCandle.highPrice < currentCandle.openPrice) {
                    continue
                }

                // 매도
                val sellInfo = VbsTradeEntity(
                    vbsConditionEntity = condition,
                    tradeType = SELL,
                    maPrice = currentCandle.average[condition.maPeriod] ?: 0.0,
                    yield = ApplicationUtil.getYield(lastBuyInfo.unitPrice, currentCandle.openPrice),
                    unitPrice = currentCandle.openPrice,
                    tradeDate = currentCandle.candleDateTimeStart
                )
                vbsTradeRepository.save(sellInfo)
                lastBuyInfo = null
                sell = true
            }

            // 하루에 한번만 매매를 한다면, 매도 했으면 그날 매수 안함
            if (sell && condition.onlyOneDayTrade) {
                continue
            }

            val targetPrice = getTargetPrice(beforeCandle, currentCandle, condition)
            val maPrice = currentCandle.average[condition.maPeriod] ?: 0.0

            // 매수 판단
            val isTarget = targetPrice <= currentCandle.highPrice
            val isMa = maPrice <= targetPrice || maPrice == 0.0
            if (isTarget && isMa) {
                lastBuyInfo = VbsTradeEntity(
                    vbsConditionEntity = condition,
                    tradeType = BUY,
                    maPrice = maPrice,
                    yield = 0.0,
                    unitPrice = targetPrice,
                    tradeDate = currentCandle.candleDateTimeStart
                )
                vbsTradeRepository.save(lastBuyInfo)
            }
        }
    }

    /**
     * @return 목표가 계산
     */
    private fun getTargetPrice(
        beforeCandle: CandleDto,
        currentCandle: CandleDto,
        condition: VbsConditionEntity
    ): Double {
        var volatilityPrice = (beforeCandle.highPrice - beforeCandle.lowPrice) * condition.kRate
        // 호가단위 기준으로 절삭
        volatilityPrice -= (volatilityPrice % condition.unitAskPrice)
        return currentCandle.openPrice + volatilityPrice
    }
}