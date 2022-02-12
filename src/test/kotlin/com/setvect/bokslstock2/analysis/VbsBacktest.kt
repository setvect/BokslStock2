package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.entity.vbs.VbsConditionEntity
import com.setvect.bokslstock2.analysis.repository.vbs.VbsConditionRepository
import com.setvect.bokslstock2.analysis.service.MovingAverageService
import com.setvect.bokslstock2.analysis.service.vbs.VbsBacktestService
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_DAY
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateRange
import java.time.LocalDateTime
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("local")
class VbsBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var vbsAverageService: MovingAverageService

    @Autowired
    private lateinit var vbsConditionRepository: VbsConditionRepository

    @Autowired
    private lateinit var vbsBacktestService: VbsBacktestService

    @Test
    @Transactional
    @Rollback(false)
    fun 한번에_모두_백테스트() {
        // 0. 기존 백테스트 기록 모두 삭제

        // 1. 조건 만들기

        // 2. 모든 조건에 대해 백테스트
        vbsBacktestService.runTestBatch()

        // 3. 모든 조건에 대한 리포트 만들기
    }


    @Test
    fun 조건생성() {
        val stock = stockRepository.findByCode("233740").get()

        val condition = VbsConditionEntity(
            stock = stock,
            periodType = PERIOD_DAY,
            kRate = 0.5,
            maPeriod = 1,
            unitAskPrice = 5,
            comment = null
        )
        vbsBacktestService.saveCondition(condition)
    }

    @Test
    @Transactional
    // TODO
    fun 이동평균돌파전략_단건_리포트생성() {
        val range = DateRange(LocalDateTime.of(2016, 1, 1, 0, 0), LocalDateTime.now())
        val conditionList = vbsConditionRepository.listBySeq(listOf(1309720))
    }

}