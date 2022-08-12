package com.setvect.bokslstock2.analysis.rebalance.service

import com.setvect.bokslstock2.analysis.common.model.PreTrade
import com.setvect.bokslstock2.analysis.common.service.BacktestTradeService
import com.setvect.bokslstock2.analysis.rebalance.model.RebalanceBacktestCondition
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDate

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
        val periodType = condition.rebalanceFacter.periodType
        val extractRange = DateRange(
            ApplicationUtil.fitStartDate(condition.rebalanceFacter.periodType, condition.tradeCondition.range.fromDate),
            ApplicationUtil.fitStartDate(condition.rebalanceFacter.periodType, condition.tradeCondition.range.toDate),
        )

        val stockPriceIndex = backtestTradeService.getStockPriceIndex(stockCodes, periodType, extractRange)

        // <종목코드, 종목정보>
        val codeByStock = stockCodes.associateWith { stockRepository.findByCode(it).get() }
        val tradeList = mutableListOf<PreTrade>()

        var current =
            ApplicationUtil.fitStartDate(condition.rebalanceFacter.periodType, condition.tradeCondition.range.fromDate)
        // TODO 여기서 부터 작업...
        while (current.isBefore(condition.tradeCondition.range.toDate) || current.isEqual(condition.tradeCondition.range.to.toLocalDate())) {
            log.info("current: $current")
            stockCodes.forEach { code ->
                val candleDto: CandleDto = stockPriceIndex[code]!![current]!!
                log.info("\t $code: OHLC: ${candleDto.openPrice},${candleDto.highPrice},${candleDto.lowPrice},${candleDto.closePrice}, ")
            }

            current = incrementDate(periodType, current)
        }
    }

    private fun incrementDate(
        periodType: PeriodType,
        current: LocalDate
    ): LocalDate {
        return when (periodType) {
            PeriodType.PERIOD_WEEK -> current.plusWeeks(1)
            PeriodType.PERIOD_MONTH -> current.plusMonths(1)
            PeriodType.PERIOD_QUARTER -> current.plusMonths(3)
            PeriodType.PERIOD_HALF -> current.plusMonths(6)
            PeriodType.PERIOD_YEAR -> current.plusYears(1)
            else -> current
        }
    }


    private fun checkValidate(rebalanceBacktestCondition: RebalanceBacktestCondition) {
        val sumWeight = rebalanceBacktestCondition.stockCodes.sumOf { it.weight }
        if (sumWeight != 100) {
            throw RuntimeException("비중 합계는 100이여야 됩니다.")
        }
    }
}