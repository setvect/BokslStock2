package com.setvect.bokslstock2.analysis.rebalance.service

import com.setvect.bokslstock2.analysis.common.model.PreTrade
import com.setvect.bokslstock2.analysis.common.service.BacktestTradeService
import com.setvect.bokslstock2.analysis.rebalance.model.RebalanceBacktestCondition
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.DateUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime

/**
 * 리벨런싱 백테스트
 */
@Service
class RebalanceAnalysisService(
    private val stockRepository: StockRepository,
    private val backtestTradeService: BacktestTradeService,
    private val candleRepository: CandleRepository,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)


    fun makeSummaryReport(conditionList: List<RebalanceBacktestCondition>): File {
        conditionList.map { rebalanceBacktestCondition ->
            checkValidate(rebalanceBacktestCondition)
            processRebalance(rebalanceBacktestCondition)
        }
        return File("./")
    }


    private fun processRebalance(condition: RebalanceBacktestCondition) {
        val stockCodes = condition.listStock()
        // <종목코드, <날짜, 캔들>>
        val stockPriceIndex = backtestTradeService.getStockPriceIndex(stockCodes, condition.rebalanceFacter.periodType)

        // <종목코드, 종목정보>
        val codeByStock = stockCodes.associateWith { stockRepository.findByCode(it).get() }
        val tradeList = mutableListOf<PreTrade>()

        var current = startDate(condition)
        // TODO 여기서 부터 작업...

        stockPriceIndex.entries.forEach { stockPrice ->
            println("Key: ${stockPrice.key}")
            stockPrice.value.entries.forEach { sp -> println("\t${sp.key}: ${sp.value}") }
        }
    }

    /**
     * @return 백테스트 시작 날짜
     */
    private fun startDate(condition: RebalanceBacktestCondition): LocalDateTime {
        var current = condition.tradeCondition.range.from

        when (condition.rebalanceFacter.periodType) {
            PeriodType.PERIOD_WEEK -> current = DateUtil.convertDateOfMonday(current)
            PeriodType.PERIOD_MONTH -> current = current.withDayOfMonth(1)
            PeriodType.PERIOD_QUARTER, PeriodType.PERIOD_HALF, PeriodType.PERIOD_YEAR
            -> current = DateUtil.fitMonth(
                current.withDayOfMonth(1),
                condition.rebalanceFacter.periodType.getDeviceMonth()
            ).atTime(0, 0)
        }
        return current
    }

    private fun checkValidate(rebalanceBacktestCondition: RebalanceBacktestCondition) {
        val sumWeight = rebalanceBacktestCondition.stockCodes.sumOf { it.weight }
        if (sumWeight != 100) {
            throw RuntimeException("비중 합계는 100이여야 됩니다.")
        }
    }
}