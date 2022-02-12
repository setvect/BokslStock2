package com.setvect.bokslstock2.analysis.service.vbs

import com.setvect.bokslstock2.analysis.entity.MabsTradeEntity
import com.setvect.bokslstock2.analysis.entity.vbs.VbsConditionEntity
import com.setvect.bokslstock2.analysis.model.TradeType.SELL
import com.setvect.bokslstock2.analysis.repository.vbs.VbsConditionRepository
import com.setvect.bokslstock2.analysis.repository.vbs.VbsTradeRepository
import com.setvect.bokslstock2.analysis.service.MovingAverageService
import com.setvect.bokslstock2.index.dto.CandleDto
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
        conditionList.forEach {
            vbsTradeRepository.deleteByCondition(it)
            backtest(it)
            log.info("백테스트 진행 ${++i}/${conditionList.size}")
        }
    }

    private fun backtest(condition: VbsConditionEntity) {
        condition.stock.candleList
        val movingAverageCandle = movingAverageService.getMovingAverage(
            condition.stock.code, condition.periodType, listOf(condition.maPeriod)
        )

        var lastStatus = SELL
        var highYield = 0.0
        var lowYield = 0.0
        var lastBuyInfo: MabsTradeEntity? = null

        for (idx in 1 until movingAverageCandle.size) {
            val currentCandle = movingAverageCandle[idx]
            // -1 영업일
            val beforeCandle = movingAverageCandle[idx - 1]
// TODO
            if (lastStatus == SELL) {
                val targetPrice = getTargetPrice(beforeCandle, currentCandle, condition)
                // 매수 판단
                if (targetPrice <= currentCandle.highPrice) {
                }

            } else {
            }
        }
    }

    /**
     * @return 목표가 계산
     */
    private fun getTargetPrice(beforeCandle: CandleDto, currentCandle: CandleDto, condition: VbsConditionEntity): Int {
        val maPeriod = beforeCandle.average[condition.maPeriod] ?: return Integer.MAX_VALUE

        var volatilityPrice = (beforeCandle.highPrice - beforeCandle.lowPrice) * condition.kRate
        // 호가단위 기준으로 절삭
        volatilityPrice -= (volatilityPrice % condition.unitAskPrice)
        return (currentCandle.openPrice + volatilityPrice).toInt()

    }


}