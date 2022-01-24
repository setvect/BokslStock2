package com.setvect.bokslstock2.analysis.service

import com.setvect.bokslstock2.analysis.entity.MabsConditionEntity
import com.setvect.bokslstock2.analysis.entity.MabsTradeEntity
import com.setvect.bokslstock2.analysis.model.AnalysisMabsCondition
import com.setvect.bokslstock2.analysis.model.AnalysisReportResult
import com.setvect.bokslstock2.analysis.model.AnalysisReportResult.TotalYield
import com.setvect.bokslstock2.analysis.model.AnalysisReportResult.WinningRate
import com.setvect.bokslstock2.analysis.model.AnalysisReportResult.YieldMdd
import com.setvect.bokslstock2.analysis.model.TradeReportItem
import com.setvect.bokslstock2.analysis.model.TradeType.BUY
import com.setvect.bokslstock2.analysis.model.TradeType.SELL
import com.setvect.bokslstock2.analysis.repository.MabsConditionRepository
import com.setvect.bokslstock2.analysis.repository.MabsTradeRepository
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import javax.transaction.Transactional
import kotlin.streams.toList

/**
 * 이동평균 돌파 매매 백테스트
 */
@Service
class MabsBacktestService(
    val mabsConditionRepository: MabsConditionRepository,
    val mabsTradeRepository: MabsTradeRepository,
    val movingAverageService: MovingAverageService,
    val candleRepository: CandleRepository
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 백테스트 조건 저장
     */
    fun saveCondition(mabsCondition: MabsConditionEntity) {
        mabsConditionRepository.save(mabsCondition)
    }

    /**
     * 모든 조건에 대한 백테스트 진행
     * 기존 백테스트 기록을 모두 삭제하고 다시 테스트 함
     */
    @Transactional
    fun runTestBatch() {
        val conditionList = mabsConditionRepository.findAll()
        conditionList.forEach {
            mabsTradeRepository.deleteByCondition(it)
            backtest(it)
        }
    }

    private fun backtest(condition: MabsConditionEntity) {
        condition.stock.candleList
        val movingAverageCandle = movingAverageService.getMovingAverage(
            condition.stock.code, condition.periodType, listOf(condition.shortPeriod, condition.longPeriod)
        )

        var lastStatus = SELL
        var highYield = 0.0
        var lowYield = 0.0
        var lastBuyInfo: MabsTradeEntity? = null

        for (idx in 2 until movingAverageCandle.size) {
            val currentCandle = movingAverageCandle[idx]
            // -1 영업일
            val yesterdayCandle = movingAverageCandle[idx - 1]
            // -2 영업일
            val beforeYesterdayCandle = movingAverageCandle[idx - 2]

            if (lastStatus == SELL) {
                // 매수 판단
                if (buyCheck(beforeYesterdayCandle, condition)) {
                    val shortFormat = String.format("%,d", yesterdayCandle.average[condition.shortPeriod])
                    val longFormat = String.format("%,d", yesterdayCandle.average[condition.longPeriod])
                    log.info("새롭게 이동평균을 돌파할 때만 매수합니다. ${yesterdayCandle.candleDateTime} - 단기이평: $shortFormat, 장기이평: $longFormat")
                    continue
                }
                if (buyCheck(yesterdayCandle, condition)) {
                    lastBuyInfo = MabsTradeEntity(
                        mabsConditionEntity = condition,
                        tradeType = BUY,
                        highYield = 0.0,
                        lowYield = 0.0,
                        maShort = yesterdayCandle.average[condition.shortPeriod] ?: 0,
                        maLong = yesterdayCandle.average[condition.longPeriod] ?: 0,
                        yield = 0.0,
                        unitPrice = currentCandle.openPrice,
                        tradeDate = currentCandle.candleDateTime
                    )
                    mabsTradeRepository.save(lastBuyInfo)
                    lastStatus = BUY
                    // 매도 판단
                    val currentCloseYield = ApplicationUtil.getYield(lastBuyInfo.unitPrice, currentCandle.closePrice)
                    highYield = 0.0.coerceAtLeast(currentCloseYield)
                    lowYield = 0.0.coerceAtMost(currentCloseYield)
                }
            } else {
                if (sellCheck(yesterdayCandle, condition)) {
                    val sellInfo = MabsTradeEntity(
                        mabsConditionEntity = condition,
                        tradeType = SELL,
                        highYield = highYield,
                        lowYield = lowYield,
                        maShort = yesterdayCandle.average[condition.shortPeriod] ?: 0,
                        maLong = yesterdayCandle.average[condition.longPeriod] ?: 0,
                        yield = ApplicationUtil.getYield(lastBuyInfo!!.unitPrice, currentCandle.openPrice),
                        unitPrice = currentCandle.openPrice,
                        tradeDate = currentCandle.candleDateTime
                    )
                    mabsTradeRepository.save(sellInfo)
                    lastStatus = SELL
                    continue
                }
                val currentCloseYield = ApplicationUtil.getYield(lastBuyInfo!!.unitPrice, currentCandle.closePrice)
                highYield = highYield.coerceAtLeast(currentCloseYield)
                lowYield = lowYield.coerceAtMost(currentCloseYield)
            }
        }
    }

    /**
     * @return [candle]이 매수 조건이면 true
     */
    private fun buyCheck(candle: CandleDto, condition: MabsConditionEntity): Boolean {
        val shortValue = candle.average[condition.shortPeriod]
        val longValue = candle.average[condition.longPeriod]
        if (shortValue == null || longValue == null) {
            return false
        }
        val yieldValue = ApplicationUtil.getYield(longValue, shortValue)
        return yieldValue > condition.upBuyRate
    }

    /**
     * @return [candle]이 매도 조건이면 true
     */
    private fun sellCheck(candle: CandleDto, condition: MabsConditionEntity): Boolean {
        val shortValue = candle.average[condition.shortPeriod]
        val longValue = candle.average[condition.longPeriod]
        if (shortValue == null || longValue == null) {
            return false
        }
        val yieldValue = ApplicationUtil.getYield(longValue, shortValue)
        return (yieldValue * -1) > condition.downSellRate
    }

    /**
     *  분석 리포트
     */
    fun makeReport(condition: AnalysisMabsCondition) {
        val tradeItemHistory = trade(condition)
        val result = analysis(tradeItemHistory, condition)
        printSummary(result)
        makeReportFile(result)
    }


    /**
     * 매매 백테스트
     */
    private fun trade(condition: AnalysisMabsCondition): ArrayList<TradeReportItem> {
        val tradeList =
            condition.tradeCondition.tradeList.filter { condition.range.isBetween(it.tradeDate) }.toMutableList()
        if (tradeList.isEmpty()) {
            throw RuntimeException("매매 기록이 없습니다.")
        }
        // 첫 거래가 매도이면 삭제
        if (tradeList[0].tradeType == SELL) {
            tradeList.removeAt(0)
        }
        if (tradeList.size < 2) {
            throw RuntimeException("매수/매도 기록이 없습니다.")
        }

        var cash = condition.cash
        val tradeItemHistory = ArrayList<TradeReportItem>()
        tradeList.forEach {
            if (it.tradeType == BUY) {
                val buyQty: Int = ((cash * condition.investRatio) / it.unitPrice).toInt()
                val buyAmount: Int = buyQty * it.unitPrice
                cash -= buyAmount
                val tradeReportItem = TradeReportItem(
                    mabsTradeEntity = it,
                    qty = buyQty,
                    cash = cash,
                    feePrice = (condition.feeBuy * buyAmount).toInt(),
                    gains = 0
                )
                tradeItemHistory.add(tradeReportItem)
            } else if (it.tradeType == SELL) {
                // 투자수익금: (매수수량 * 현재가격) * 수익률 - 수수료
                val sellPrice = tradeItemHistory.last().qty * it.unitPrice
                val sellFee: Int = (sellPrice * condition.feeSell).toInt()
                val gains: Long = (sellPrice * it.yield - sellFee).toLong()

                cash += sellPrice + gains

                val tradeReportItem = TradeReportItem(
                    mabsTradeEntity = it, qty = 0, cash = cash, feePrice = sellFee, gains = gains
                )
                tradeItemHistory.add(tradeReportItem)
            }
        }
        return tradeItemHistory
    }

    /**
     * 매매 결과에 대한 통계적 분석을 함
     */
    private fun analysis(
        tradeItemHistory: ArrayList<TradeReportItem>, condition: AnalysisMabsCondition
    ): AnalysisReportResult {

        val buyAndHoldYieldMdd: YieldMdd = calculateHoldYield(condition.tradeCondition.stock, condition.range)

        val totalYield: TotalYield = calculateTotalYield(tradeItemHistory, condition)
        val winningRate: WinningRate = calculateCoinInvestment(tradeItemHistory)
        return AnalysisReportResult(
            analysisMabsCondition = condition,
            tradeHistory = tradeItemHistory,
            totalYield = totalYield,
            coinWinningRate = winningRate,
            stockHoldYield = buyAndHoldYieldMdd
        )
    }


    /**
     * @return 수익률 정보
     */
    private fun calculateTotalYield(
        tradeItemHistory: ArrayList<TradeReportItem>, condition: AnalysisMabsCondition
    ): TotalYield {
        if (tradeItemHistory.isEmpty()) {
            return TotalYield(
                yield = 0.0, mdd = 0.0, dayCount = condition.range.diffDays.toInt(), gainCount = 0, lossCount = 0
            )
        }

        val lastCash = tradeItemHistory.last().getFinalResult()

        val startCash = tradeItemHistory.first().getFinalResult()
        val realYield = ApplicationUtil.getYield(startCash, lastCash)

        var finalResultList = tradeItemHistory.stream().map(TradeReportItem::getFinalResult).toList()
        val realMdd = ApplicationUtil.getMddByLong(finalResultList)
        val totalYield = TotalYield(realYield, realMdd, condition.range.diffDays.toInt())
        tradeItemHistory.forEach {
            if (it.gains > 0) {
                totalYield.incrementGainCount()
            } else {
                totalYield.incrementLossCount()
            }
        }
        return totalYield
    }


    /**
     * @return 투자 종목 수익 정보
     */
    private fun calculateCoinInvestment(
        tradeItemHistory: ArrayList<TradeReportItem>
    ): WinningRate {
        val sellList = tradeItemHistory.filter { it.mabsTradeEntity.tradeType == SELL }.toList()
        val totalInvest = tradeItemHistory.sumOf { it.getInvestResult() }
        val gainCount = tradeItemHistory.count { it.gains > 0 }
        return WinningRate(gainCount, sellList.size - gainCount, totalInvest)
    }

    /**
     * @return 투자종목 Buy & Hold시 수익 정보
     */
    private fun calculateHoldYield(stock: StockEntity, range: DateRange): YieldMdd {
        val candleList = candleRepository.findByRange(stock, range.from, range.to)
        val priceList = candleList.map { it.closePrice }.toList()
        return YieldMdd(
            ApplicationUtil.getMdd(priceList.map { it.toDouble() }),
            ApplicationUtil.getYield(priceList.map { it.toDouble() })
        )
    }

    /**
     * 분석 요약결과
     */
    private fun printSummary(result: AnalysisReportResult) {
        val report = StringBuilder()
        val sumYield: YieldMdd = result.stockHoldYield
        val tradeCondition = result.analysisMabsCondition.tradeCondition
        report.append("매매번호: ${tradeCondition.mabsConditionSeq}, 종목: ${tradeCondition.stock.name}(${tradeCondition.stock.code}), 장기-단기: ${tradeCondition.longPeriod}-${tradeCondition.shortPeriod}\n")
        report.append(String.format("동일비중 수익\t %,.2f%%", sumYield.yield * 100)).append("\n")
        report.append(String.format("동일비중 MDD\t %,.2f%%", sumYield.mdd * 100)).append("\n")

        val totalYield: TotalYield = result.totalYield
        report.append(String.format("실현 수익\t %,.2f%%", totalYield.yield * 100)).append("\n")
        report.append(String.format("실현 MDD\t %,.2f%%", totalYield.mdd * 100)).append("\n")
        report.append(String.format("매매회수\t %d", totalYield.getTradeCount())).append("\n")
        report.append(String.format("승률\t %,.2f%%", totalYield.getWinRate() * 100)).append("\n")
        report.append(String.format("CAGR\t %,.2f%%", totalYield.getCagr() * 100)).append("\n")

        println(report)
    }

    /**
     * 매매 결과를 파일로 만듦
     */
    private fun makeReportFile(tradeItemHistory: AnalysisReportResult) {

    }
}