package com.setvect.bokslstock2.analysis.dm.serivce

import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.analysis.common.model.Trade
import com.setvect.bokslstock2.analysis.common.model.TradeType
import com.setvect.bokslstock2.analysis.common.service.BacktestTradeService
import com.setvect.bokslstock2.analysis.dm.model.DmAnalysisCondition
import com.setvect.bokslstock2.analysis.dm.model.DmAnalysisReportResult
import com.setvect.bokslstock2.analysis.dm.model.DmBacktestCondition
import com.setvect.bokslstock2.analysis.dm.model.Stock
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
    private val movingAverageService: MovingAverageService,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)
    val tradeService = BacktestTradeService()


    fun runTest(condition: DmBacktestCondition) {
        checkValidate(condition)
        val tradeList = processDualMomentum(condition)
        var sumYield = 1.0
        tradeList.forEach {
            log.info("${it.tradeType}\t${it.tradeDate}\t${it.stock.name}(${it.stock.code})\t${it.yield}")
            sumYield *= (it.yield + 1)
        }
        log.info("수익률: ${String.format("%.2f%%", (sumYield - 1) * 100)}")

        val analysisCondition = DmAnalysisCondition(
            tradeConditionList = listOf(),
            basic = condition.basic
        )
//        val trades = tradeService.trade(analysisCondition)
//        println(trades.size)
//
//        val result = tradeService.analysis(trades, analysisCondition)
//        makeReportFile(result)
    }

    private fun processDualMomentum(condition: DmBacktestCondition): List<Trade> {
        val stockCodes = condition.listStock()
        // <종목코드, 종목정보>
        val codeByStock = stockCodes.associateWith { stockRepository.findByCode(it).get() }

        // <종목코드, <날짜, 캔들>>
        val stockPriceIndex = getStockPriceIndex(stockCodes, condition)

        var current =
            DateUtil.fitMonth(condition.basic.range.from.withDayOfMonth(1), condition.periodType.getDeviceMonth())

        var beforeBuyTrade: Trade? = null

        val tradeList = mutableListOf<Trade>()
        var tradeSeq = 0L
        while (current.isBefore(condition.basic.range.to)) {
            val stockByRate = calculateRate(stockPriceIndex, current, condition, codeByStock)

            val existBeforeBuy = beforeBuyTrade != null

            // 듀얼 모멘텀 매수 대상 종목이 없으면
            if (stockByRate.isEmpty()) {
                val changeBuyStock = beforeBuyTrade != null && beforeBuyTrade.stock.code != condition.holdCode
                val existHoldCode = condition.holdCode != null

                if (existBeforeBuy && changeBuyStock) {
                    val stockPrice = stockPriceIndex[beforeBuyTrade!!.stock.code]!![current]!!
                    val sellTrade = makeSellTrade(stockPrice, current, ++tradeSeq, beforeBuyTrade)
                    tradeList.add(sellTrade)
                    beforeBuyTrade = null
                }
                if (existHoldCode && (beforeBuyTrade == null || beforeBuyTrade.stock.code != condition.holdCode)) {
                    val stockPrice = stockPriceIndex[condition.holdCode]!![current]!!
                    val stock = codeByStock[condition.holdCode]!!
                    val buyTrade = makeBuyTrade(stockPrice, current, ++tradeSeq, stock)
                    tradeList.add(buyTrade)
                    beforeBuyTrade = buyTrade
                } else if (existHoldCode) {
                    log.info("매수 유지: $current, ${getStockName(codeByStock, condition.holdCode!!)}(${condition.holdCode})")
                }
            } else {
                val buyStockRate = stockByRate[0]
                val stockCode = buyStockRate.first
                val changeBuyStock = beforeBuyTrade != null && beforeBuyTrade.stock.code != stockCode

                if (existBeforeBuy && changeBuyStock) {
                    val stockPrice = stockPriceIndex[beforeBuyTrade!!.stock.code]!![current]!!
                    val sellTrade = makeSellTrade(stockPrice, current, ++tradeSeq, beforeBuyTrade)
                    tradeList.add(sellTrade)
                }
                if (!existBeforeBuy || changeBuyStock) {
                    val stockPrice = stockPriceIndex[stockCode]!![current]!!
                    val stock = codeByStock[stockCode]!!
                    val buyTrade = makeBuyTrade(stockPrice, current, ++tradeSeq, stock)
                    tradeList.add(buyTrade)
                    beforeBuyTrade = buyTrade
                } else {
                    log.info("매수 유지: $current, ${beforeBuyTrade!!.stock.name}(${beforeBuyTrade.stock.code})")
                }
            }

//            stockByRate.forEach {
//                log.info("$current - ${getStockName(codeByStock, it.first)}(${it.first}) : ${it.second}")
//            }

//            if (stockByRate.isEmpty()) {
//                log.info("$current - empty")
//            }
            current = current.plusMonths(condition.periodType.getDeviceMonth().toLong())
        }


        return tradeList
    }

    private fun makeBuyTrade(
        stockPrice: CandleDto,
        current: LocalDateTime,
        seq: Long,
        stock: StockEntity
    ): Trade {
        val buyTrade = Trade(
            stock = Stock.of(stock),
            tradeType = TradeType.BUY,
            yield = 0.0,
            unitPrice = stockPrice.openPrice,
            tradeDate = current,
            seqNo = seq,
        )
        log.info("매수: ${buyTrade.tradeDate}, ${buyTrade.stock.name}(${buyTrade.stock.code})")
        return buyTrade
    }


    private fun makeSellTrade(
        stockPrice: CandleDto,
        current: LocalDateTime,
        seq: Long,
        beforeBuyTrade: Trade
    ): Trade {
        val sellTrade = Trade(
            stock = beforeBuyTrade.stock,
            tradeType = TradeType.SELL,
            yield = ApplicationUtil.getYield(beforeBuyTrade.unitPrice, stockPrice.openPrice),
            unitPrice = stockPrice.openPrice,
            tradeDate = current,
            seqNo = seq,
        )
        log.info("매도: ${sellTrade.tradeDate}, ${sellTrade.stock.name}(${sellTrade.stock.code}), 수익: ${sellTrade.yield}")
        return sellTrade
    }

    /**
     * 듀얼 모멘터 대상 종목을 구함
     * [stockPriceIndex] <종목코드, <날짜, 캔들>>
     * @return <종목코드, 현재가격/모멘텀평균 가격>
     */
    private fun calculateRate(
        stockPriceIndex: Map<String, Map<LocalDateTime, CandleDto>>,
        current: LocalDateTime,
        condition: DmBacktestCondition,
        codeByStock: Map<String, StockEntity>
    ): List<Pair<String, Double>> {
        val stockByRate = stockPriceIndex.entries.map { stockEntry ->
            val stock = codeByStock[stockEntry.key]!!

            val currentCandle = stockEntry.value[current]
                ?: throw RuntimeException("${stockEntry.key}에 대한 $current 시세가 없습니다.")

//            log.info(
//                "\t${current}: ${stock.name}(${stock.code}): ${currentCandle.candleDateTimeStart}~${currentCandle.candleDateTimeEnd} - " +
//                        "O: ${currentCandle.openPrice}, H: ${currentCandle.highPrice}, L: ${currentCandle.lowPrice}, C:${currentCandle.closePrice}, ${currentCandle.periodType}"
//            )

            val average = condition.timeWeight.entries.sumOf { timeWeight ->
                val delta = timeWeight.key
                val weight = timeWeight.value
                val deltaCandle = stockEntry.value[current.minusMonths(delta.toLong())]!!
//                log.info("\t\t[${delta}] ${stock.code} - ${deltaCandle.candleDateTimeStart} - C: ${deltaCandle.closePrice}")
//                log.info(
//                    "\t\t$delta -   ${stock.name}(${stock.code}): ${deltaCandle.candleDateTimeStart}~${deltaCandle.candleDateTimeEnd} - " +
//                            "O: ${deltaCandle.openPrice}, H: ${deltaCandle.highPrice}, L: ${deltaCandle.lowPrice}, C:${deltaCandle.closePrice}, ${deltaCandle.periodType}"
//                )

                deltaCandle.closePrice * weight
            }

            val rate = currentCandle.openPrice / average
//            log.info("\t${average}: \t${rate}")
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
        stockCodes: List<String>,
        dmCondition: DmBacktestCondition
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

    private fun checkValidate(dmCondition: DmBacktestCondition) {
        val sumWeight = dmCondition.timeWeight.entries.sumOf { it.value }
        if (sumWeight != 1.0) {
            throw RuntimeException("가중치의 합계가 100이여 합니다. 현재 가중치 합계: $sumWeight")
        }
    }


    /**
     * 분석 요약결과
     */
    private fun getSummary(result: DmAnalysisReportResult): String {
        val report = StringBuilder()
        val tradeConditionList = result.dmAnalysisCondition.tradeConditionList

        report.append("----------- Buy&Hold 결과 -----------\n")
        report.append(String.format("합산 동일비중 수익\t %,.2f%%", result.common.buyHoldYieldTotal.yield * 100))
            .append("\n")
        report.append(String.format("합산 동일비중 MDD\t %,.2f%%", result.common.buyHoldYieldTotal.mdd * 100)).append("\n")
        report.append(String.format("합산 동일비중 CAGR\t %,.2f%%", result.common.buyHoldYieldTotal.getCagr() * 100))
            .append("\n")
        report.append(String.format("샤프지수\t %,.2f", result.common.getBuyHoldSharpeRatio())).append("\n")

//        for (i in 1..tradeConditionList.size) {
//            val tradeCondition = tradeConditionList[i - 1]
//            report.append(
//                "${i}. 조건번호: ${tradeCondition.conditionSeq}, 종목: ${tradeCondition.stock.name}(${tradeCondition.stock.code}), " +
//                        "매매주기: ${tradeCondition.periodType}, 변동성 비율: ${tradeCondition.kRate}, 이동평균 단위:${tradeCondition.maPeriod}, " +
//                        "갭상승 통과: ${tradeCondition.gapRisenSkip}, 하루에 한번 거래: ${tradeCondition.onlyOneDayTrade}\n"
//            )
//            val sumYield = result.common.buyHoldYieldCondition[tradeCondition.conditionSeq]
//            if (sumYield == null) {
//                log.warn("조건에 해당하는 결과가 없습니다. vbsConditionSeq: ${tradeCondition.conditionSeq}")
//                break
//            }
//            report.append(String.format("${i}. 동일비중 수익\t %,.2f%%", sumYield.yield * 100)).append("\n")
//            report.append(String.format("${i}. 동일비중 MDD\t %,.2f%%", sumYield.mdd * 100)).append("\n")
//        }

        val totalYield: CommonAnalysisReportResult.TotalYield = result.common.yieldTotal
        report.append("----------- 전략 결과 -----------\n")
        report.append(String.format("합산 실현 수익\t %,.2f%%", totalYield.yield * 100)).append("\n")
        report.append(String.format("합산 실현 MDD\t %,.2f%%", totalYield.mdd * 100)).append("\n")
        report.append(String.format("합산 매매회수\t %d", result.common.getWinningRateTotal().getTradeCount())).append("\n")
        report.append(String.format("합산 승률\t %,.2f%%", result.common.getWinningRateTotal().getWinRate() * 100))
            .append("\n")
        report.append(String.format("합산 CAGR\t %,.2f%%", totalYield.getCagr() * 100)).append("\n")
        report.append(String.format("샤프지수\t %,.2f", result.common.getBacktestSharpeRatio())).append("\n")

        for (i in 1..tradeConditionList.size) {
            val tradeCondition = tradeConditionList[i - 1]
//            report.append(
//                "${i}. 조건번호: ${tradeCondition.conditionSeq}, 종목: ${tradeCondition.stock.name}(${tradeCondition.stock.code}), " +
//                        "매매주기: ${tradeCondition.periodType}, 변동성 비율: ${tradeCondition.kRate}, 이동평균 단위:${tradeCondition.maPeriod}, " +
//                        "갭상승 통과: ${tradeCondition.gapRisenSkip}, 하루에 한번 거래: ${tradeCondition.onlyOneDayTrade}\n"
//            )
//
//            val winningRate = result.common.winningRateCondition[tradeCondition.conditionSeq]
//            if (winningRate == null) {
//                log.warn("조건에 해당하는 결과가 없습니다. vbsConditionSeq: ${tradeCondition.conditionSeq}")
//                break
//            }
//            report.append(String.format("${i}. 실현 수익\t %,f", winningRate.invest)).append("\n")
//            report.append(String.format("${i}. 매매회수\t %d", winningRate.getTradeCount())).append("\n")
//            report.append(String.format("${i}. 승률\t %,.2f%%", winningRate.getWinRate() * 100)).append("\n")
        }
        return report.toString()
    }
}