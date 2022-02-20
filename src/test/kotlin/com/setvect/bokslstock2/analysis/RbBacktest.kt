package com.setvect.bokslstock2.analysis

import com.setvect.bokslstock2.analysis.rb.entity.RbConditionEntity
import com.setvect.bokslstock2.analysis.rb.repository.RbConditionRepository
import com.setvect.bokslstock2.analysis.rb.repository.RbTradeRepository
import com.setvect.bokslstock2.analysis.rb.setvice.RbAnalysisService
import com.setvect.bokslstock2.analysis.rb.setvice.RbBacktestService
import com.setvect.bokslstock2.index.model.PeriodType.PERIOD_MONTH
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Rollback
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@SpringBootTest
@ActiveProfiles("local")
class RbBacktest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var stockRepository: StockRepository

    @Autowired
    private lateinit var rbTradeRepository: RbTradeRepository

    @Autowired
    private lateinit var rbConditionRepository: RbConditionRepository

    @Autowired
    private lateinit var rbBacktestService: RbBacktestService

    @Autowired
    private lateinit var analysisService: RbAnalysisService

    @Autowired
    private lateinit var candleRepository: CandleRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    /**
     * 백테스트 건수에 따라 Heap 사이즈 조절
     */
    @Test
    @Transactional
    @Rollback(false)
    fun 모든매매조건_단건기준_리포트생성() {
        // 0. 기존 백테스트 기록 모두 삭제
        deleteBacktestData()

        // 1. 조건 만들기
        조건생성()

        // 2. 모든 조건에 대해 백테스트
        rbBacktestService.runTestBatch()
    }

    @Test
    fun 조건생성() {
        val stockList = stockRepository.findAll()

        stockList.forEach { stock ->
            val condition = RbConditionEntity(
                stock = stock,
                periodType = PERIOD_MONTH,
                comment = null
            )
            rbBacktestService.saveCondition(condition)
        }
    }



    private fun deleteBacktestData() {
        rbTradeRepository.deleteAll()
        rbConditionRepository.deleteAll()
    }


}