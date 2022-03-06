package com.setvect.bokslstock2.analysis.dm.serivce

import com.setvect.bokslstock2.analysis.common.model.TradeType
import com.setvect.bokslstock2.analysis.dm.model.DmAnalysisCondition
import com.setvect.bokslstock2.analysis.dm.model.DmTrade
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
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
        val tradeList = processDualMomentum(condition)
        var sumYield = 1.0
        tradeList.filter { it.tradeType == TradeType.SELL }.forEach {
            log.info("매매기록: $it")
            sumYield *= (it.yield + 1)
        }
        log.info("수익률: ${String.format("%.2f%%", (sumYield - 1) * 100)}")
    }

    private fun processDualMomentum(condition: DmAnalysisCondition): MutableList<DmTrade> {
        val stockCodes = getTradeStockCode(condition)

        // <종목코드, 종목정보>
        val codeByStock = stockCodes.associateWith { stockRepository.findByCode(it).get() }

        // <종목코드, <날짜, 캔들>>
        val stockPriceIndex = getStockPriceIndex(stockCodes, condition)

        var current =
            DateUtil.fitMonth(condition.basic.range.from.withDayOfMonth(1), condition.periodType.getDeviceMonth())

        var beforeBuyTrade: DmTrade? = null

        val tradeList = mutableListOf<DmTrade>()

        while (current.isBefore(condition.basic.range.to)) {
            val stockByRate = calculateRate(stockPriceIndex, current, condition, codeByStock)

            val existBeforeBuy = beforeBuyTrade != null
            val changeBuyStock = beforeBuyTrade != null && beforeBuyTrade.stockCode != condition.holdCode

            // 듀얼 모멘텀 매수 대상 종목이 없으면
            if (stockByRate.isEmpty()) {
                val existHoldCode = condition.holdCode != null
                if (existBeforeBuy && changeBuyStock) {
                    val stockPrice = stockPriceIndex[beforeBuyTrade!!.stockCode]!![current]!!
                    val sellTrade = DmTrade(
                        stockCode = beforeBuyTrade.stockCode,
                        tradeType = TradeType.SELL,
                        yield = ApplicationUtil.getYield(beforeBuyTrade.unitPrice, stockPrice.openPrice),
                        unitPrice = stockPrice.openPrice,
                        tradeDate = current
                    )
                    tradeList.add(sellTrade)
                    log.info("매도: ${getStockName(codeByStock, beforeBuyTrade.stockCode)}(${beforeBuyTrade.stockCode})")
                    beforeBuyTrade = null
                }
                if (existHoldCode && changeBuyStock) {
                    val stockPrice = stockPriceIndex[condition.holdCode]!![current]!!
                    val buyTrade = DmTrade(
                        stockCode = condition.holdCode!!,
                        tradeType = TradeType.BUY,
                        yield = 0.0,
                        unitPrice = stockPrice.openPrice,
                        tradeDate = current
                    )
                    tradeList.add(buyTrade)
                    log.info("매수: ${getStockName(codeByStock, condition.holdCode)}(${condition.holdCode})")
                    beforeBuyTrade = buyTrade
                } else if (existHoldCode) {
                    log.info("매수 유지: ${getStockName(codeByStock, condition.holdCode!!)}(${condition.holdCode})")
                }
            } else {
                val buyStockRate = stockByRate[0]
                val stockCode = buyStockRate.first
                val stockPrice = stockPriceIndex[stockCode]!![current]!!
                if (existBeforeBuy && changeBuyStock) {
                    val sellTrade = DmTrade(
                        stockCode = beforeBuyTrade!!.stockCode,
                        tradeType = TradeType.SELL,
                        yield = ApplicationUtil.getYield(beforeBuyTrade.unitPrice, stockPrice.openPrice),
                        unitPrice = stockPrice.openPrice,
                        tradeDate = current
                    )
                    tradeList.add(sellTrade)
                    log.info("매도: ${getStockName(codeByStock, beforeBuyTrade.stockCode)}(${beforeBuyTrade.stockCode})")
                }
                if (!existBeforeBuy || changeBuyStock) {
                    val buyTrade = DmTrade(
                        stockCode = stockCode,
                        tradeType = TradeType.BUY,
                        yield = 0.0,
                        unitPrice = stockPrice.openPrice,
                        tradeDate = current
                    )
                    tradeList.add(buyTrade)
                    log.info("매수: ${getStockName(codeByStock, stockCode)}(${stockCode})")
                    beforeBuyTrade = buyTrade
                } else {
                    log.info("매수 유지: ${getStockName(codeByStock, beforeBuyTrade!!.stockCode)}(${beforeBuyTrade.stockCode})")
                }
            }

            stockByRate.forEach {
                log.info("$current - ${getStockName(codeByStock, it.first)}(${it.first}) : ${it.second}")
            }

            if (stockByRate.isEmpty()) {
                log.info("$current - empty")
            }
            current = current.plusMonths(condition.periodType.getDeviceMonth().toLong())
        }
        return tradeList
    }

    /**
     * 듀얼 모멘터 대상 종목을 구함
     * [stockPriceIndex] <종목코드, <날짜, 캔들>>
     * @return <종목코드, 현재가격/모멘텀평균 가격>
     */
    private fun calculateRate(
        stockPriceIndex: Map<String, Map<LocalDateTime, CandleDto>>,
        current: LocalDateTime,
        condition: DmAnalysisCondition,
        codeByStock: Map<String, StockEntity>
    ): List<Pair<String, Double>> {
        val stockByRate = stockPriceIndex.entries.map { stockEntry ->
            val stock = codeByStock[stockEntry.key]!!

            val currentCandle = stockEntry.value[current]
                ?: throw RuntimeException("${stockEntry.key}에 대한 $current 시세가 없습니다.")

            log.info(
                "\t${current}: ${stock.name}(${stock.code}): ${currentCandle.candleDateTimeStart}~${currentCandle.candleDateTimeEnd} - " +
                        "O: ${currentCandle.openPrice}, H: ${currentCandle.highPrice}, L: ${currentCandle.lowPrice}, C:${currentCandle.closePrice}, ${currentCandle.periodType}"
            )

            val average = condition.timeWeight.entries.sumOf { timeWeight ->
                val delta = timeWeight.key
                val weight = timeWeight.value
                val deltaCandle = stockEntry.value[current.minusMonths(delta.toLong())]!!
                log.info("\t\t[${delta}] ${stock.code} - ${deltaCandle.candleDateTimeStart} - C: ${deltaCandle.closePrice}")


                log.info(
                    "\t\t$delta -   ${stock.name}(${stock.code}): ${deltaCandle.candleDateTimeStart}~${deltaCandle.candleDateTimeEnd} - " +
                            "O: ${deltaCandle.openPrice}, H: ${deltaCandle.highPrice}, L: ${deltaCandle.lowPrice}, C:${deltaCandle.closePrice}, ${deltaCandle.periodType}"
                )

                deltaCandle.closePrice * weight
            }

            val rate = currentCandle.openPrice / average
            //                log.info("\t${average}: \t${rate}")
            stockEntry.key to rate
        }
            .filter { it.second >= 1 && it.first != condition.holdCode }
            .sortedByDescending { it.second }
        return stockByRate
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