package com.setvect.bokslstock2.analysis.dm.serivce

import com.setvect.bokslstock2.analysis.dm.model.DmAnalysisCondition
import com.setvect.bokslstock2.analysis.dm.model.DmTrade
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.DateUtil
import java.time.LocalDateTime
import java.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 듀얼모멘텀 백테스트
 */
@Service
class DmAnalysisService(
    private val stockRepository: StockRepository,
    private val movingAverageService: MovingAverageService
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun runTest(condition: DmAnalysisCondition) {
        checkVaidate(condition)

        val stockCodes = getTradeStockCode(condition)

        // <종목코드, 종목정보>
        val codeByStock = stockCodes.associateWith { stockRepository.findByCode(it).get() }

        // <종목코드, <날짜, 캔들>>
        val stockPriceIndex = getStockPriceIndex(stockCodes, condition)

        var current =
            DateUtil.fitMonth(condition.basic.range.from.withDayOfMonth(1), condition.periodType.getDeviceMonth())

        var currentBuyTrade: DmTrade? = null

        while (current.isBefore(condition.basic.range.to)) {
            // <종목코드, 모멘텀평균 가격/현재가격>
            val stockByRate = stockPriceIndex.entries.map { stockEntry ->
                val currentCandle = stockEntry.value[current]
                    ?: throw RuntimeException("${stockEntry.key}에 대한 $current 시세가 없습니다.")

//                log.info(
//                    "\t${current}: ${stock.name}(${stock.code}): ${currentCandle.candleDateTimeStart}~${currentCandle.candleDateTimeEnd} - " +
//                            "O: ${currentCandle.openPrice}, H: ${currentCandle.highPrice}, L: ${currentCandle.lowPrice}, C:${currentCandle.closePrice}, ${currentCandle.periodType}"
//                )

                val average = condition.timeWeight.entries.map { timeWeight ->
                    val delta = timeWeight.key
                    val weight = timeWeight.value
                    val deltaCandle = stockEntry.value[current.minusMonths(delta.toLong())]!!
//                    log.info("\t\t[${delta}] ${stock.code} - ${deltaCandle.candleDateTimeStart} - C: ${deltaCandle.closePrice}")


//                    log.info("\t\t$delta -   ${stock.name}(${stock.code}): ${deltaCandle.candleDateTimeStart}~${deltaCandle.candleDateTimeEnd} - " +
//                            "O: ${deltaCandle.openPrice}, H: ${deltaCandle.highPrice}, L: ${deltaCandle.lowPrice}, C:${deltaCandle.closePrice}, ${deltaCandle.periodType}")

                    deltaCandle.closePrice * weight
                }.sum()

                val rate = average / currentCandle.openPrice
//                log.info("\t${average}: \t${rate}")
                stockEntry.key to rate
            }
                .filter { it.second <= 1 && it.first != condition.holdCode }
                .sortedBy { it.second }

            // 듀얼 모멘텀 매수 대상 종목이 없으면
            if (stockByRate.isEmpty()) {
                log.info("매수할 종목없음")

                if (condition.holdCode == null) {
                    if (currentBuyTrade != null) {
                        log.info("${getStockName(codeByStock, currentBuyTrade.stockCode)}(${currentBuyTrade.stockCode}) 매도")
                    }
                } else {
                    if (currentBuyTrade == null) {
                        log.info("${getStockName(codeByStock, condition.holdCode)}(${condition.holdCode}) 매수")
                    } else if (currentBuyTrade.stockCode != condition.holdCode) {
                        log.info("${getStockName(codeByStock, currentBuyTrade.stockCode)}(${currentBuyTrade.stockCode}) 매도")
                        log.info("${getStockName(codeByStock, condition.holdCode)}(${condition.holdCode}) 매수")
                    } else {
                        log.info("${getStockName(codeByStock, currentBuyTrade.stockCode)}(${currentBuyTrade.stockCode}) 매수 유지")
                    }
                }
            } else {

                val buyStock = stockByRate[0]

                if (currentBuyTrade == null) {
                    // 매수
                } else if (currentBuyTrade.stockCode != buyStock.first) {
                    // 매도후 매수
                }
            }

            stockByRate.forEach {
                log.info("$current - ${getStockName(codeByStock, it.first)}(${it.first}) : ${it.second}")
            }

            if (stockByRate.isEmpty()) {
                log.info("$current - empty")
            }

            println()

            current = current.plusMonths(condition.periodType.getDeviceMonth().toLong())
        }
    }

    private fun getStockName(codeByStock: Map<String, StockEntity>, code: String): String {
        return codeByStock[code]!!.name
    }

    /**
     * @return <종목코드, <날짜, 캔들>>
     */
    private fun getStockPriceIndex(
        stockCodes: MutableList<String>,
        dmCondition: DmAnalysisCondition
    ): Map<String, Map<LocalDateTime, CandleDto>> {
        val stockPriceIndex = stockCodes.associateWith { code ->
            movingAverageService.getMovingAverage(
                code,
                dmCondition.periodType,
                Collections.emptyList()
            )
                .associateBy { it.candleDateTimeStart.withDayOfMonth(1) }
        }
        return stockPriceIndex
    }

    private fun getTradeStockCode(dmCondition: DmAnalysisCondition): MutableList<String> {
        val stockCodes = dmCondition.stockCodes.toMutableList()
        if (dmCondition.holdCode != null) {
            stockCodes.add(dmCondition.holdCode)
        }
        return stockCodes
    }

    private fun checkVaidate(dmCondition: DmAnalysisCondition) {
        val sumWeight = dmCondition.timeWeight.entries.sumOf { it.value }
        if (sumWeight != 1.0) {
            throw RuntimeException("가중치의 합계가 100이여 합니다. 현재 가중치 합계: $sumWeight")
        }
    }
}