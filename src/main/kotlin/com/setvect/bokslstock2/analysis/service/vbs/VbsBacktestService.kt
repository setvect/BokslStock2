package com.setvect.bokslstock2.analysis.service.vbs

import com.setvect.bokslstock2.analysis.entity.vbs.VbsConditionEntity
import com.setvect.bokslstock2.analysis.repository.MabsConditionRepository
import com.setvect.bokslstock2.analysis.repository.MabsTradeRepository
import com.setvect.bokslstock2.analysis.service.MovingAverageService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 변동성 돌파 전략
 */
@Service
class VbsBacktestService(
    val mabsConditionRepository: MabsConditionRepository,
    val mabsTradeRepository: MabsTradeRepository,
    val movingAverageService: MovingAverageService,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun saveCondition(vbsConditionEntity: VbsConditionEntity) {

    }
}