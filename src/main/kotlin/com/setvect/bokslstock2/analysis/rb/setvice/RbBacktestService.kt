package com.setvect.bokslstock2.analysis.rb.setvice

import com.setvect.bokslstock2.analysis.common.model.TradeType.BUY
import com.setvect.bokslstock2.analysis.common.model.TradeType.SELL
import com.setvect.bokslstock2.analysis.rb.entity.RbConditionEntity
import com.setvect.bokslstock2.analysis.rb.entity.RbTradeEntity
import com.setvect.bokslstock2.analysis.rb.repository.RbConditionRepository
import com.setvect.bokslstock2.analysis.rb.repository.RbTradeRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 리벨런싱 돌파 전략
 */
@Service
class RbBacktestService(
    val rbConditionRepository: RbConditionRepository,
    val rbTradeRepository: RbTradeRepository,
    val movingAverageService: MovingAverageService,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 백테스트 조건 저장
     */
    fun saveCondition(rbCondition: RbConditionEntity) {
        rbConditionRepository.save(rbCondition)
    }

    /**
     * 모든 조건에 대한 백테스트 진행
     * 기존 백테스트 기록을 모두 삭제하고 다시 테스트 함
     */
    @Transactional
    fun runTestBatch() {
        val conditionList = rbConditionRepository.findAll()
        var i = 0
        conditionList.forEach {
            rbTradeRepository.deleteByCondition(it)
            runTest(it)
            log.info("백테스트 진행 ${++i}/${conditionList.size}")
        }
    }

    @Transactional
    fun runTest(condition: RbConditionEntity) {
        val movingAverageCandle = movingAverageService.getMovingAverage(
            condition.stock.code, condition.periodType, listOf()
        )

        var lastBuyInfo: RbTradeEntity? = null

        for (idx in movingAverageCandle.indices) {
            val currentCandle = movingAverageCandle[idx]
            if (lastBuyInfo != null) {
                // 매도
                val sellInfo = RbTradeEntity(
                    rbConditionEntity = condition,
                    tradeType = SELL,
                    yield = ApplicationUtil.getYield(lastBuyInfo.unitPrice, currentCandle.openPrice),
                    unitPrice = currentCandle.openPrice,
                    tradeDate = currentCandle.candleDateTimeStart
                )
                rbTradeRepository.save(sellInfo)
            }

            // 매수
            lastBuyInfo = RbTradeEntity(
                rbConditionEntity = condition,
                tradeType = BUY,
                yield = 0.0,
                unitPrice = currentCandle.openPrice,
                tradeDate = currentCandle.candleDateTimeStart
            )
            rbTradeRepository.save(lastBuyInfo)
        }
    }
}