package com.setvect.bokslstock2.analysis.service

import com.setvect.bokslstock2.analysis.entity.MabsConditionEntity
import com.setvect.bokslstock2.analysis.entity.MabsTradeEntity
import com.setvect.bokslstock2.analysis.model.AnalysisMabsCondition
import com.setvect.bokslstock2.analysis.model.AnalysisReportResult
import com.setvect.bokslstock2.analysis.model.AnalysisReportResult.TotalYield
import com.setvect.bokslstock2.analysis.model.AnalysisReportResult.WinningRate
import com.setvect.bokslstock2.analysis.model.AnalysisReportResult.YieldMdd
import com.setvect.bokslstock2.analysis.model.EvaluationAmountItem
import com.setvect.bokslstock2.analysis.model.TradeReportItem
import com.setvect.bokslstock2.analysis.model.TradeType.BUY
import com.setvect.bokslstock2.analysis.model.TradeType.SELL
import com.setvect.bokslstock2.analysis.repository.MabsConditionRepository
import com.setvect.bokslstock2.analysis.repository.MabsTradeRepository
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.DateUtil
import java.io.File
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.streams.toList

/**
 * 이동평균 돌파 매매 백테스트
 */
@Service
class MabsBacktestService(
    val mabsConditionRepository: MabsConditionRepository,
    val mabsTradeRepository: MabsTradeRepository,
    val movingAverageService: MovingAverageService,
    val candleRepository: CandleRepository,
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
        var i = 0
        conditionList.forEach {
            mabsTradeRepository.deleteByCondition(it)
            backtest(it)
            log.info("백테스트 진행 ${++i}/${conditionList.size}")
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
                    log.info("새롭게 이동평균을 돌파할 때만 매수합니다. ${yesterdayCandle.candleDateTimeStart}~${yesterdayCandle.candleDateTimeEnd} - 단기이평: $shortFormat, 장기이평: $longFormat")
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
                        tradeDate = currentCandle.candleDateTimeStart
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
                        tradeDate = currentCandle.candleDateTimeStart
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
        val summary = getSummary(result)
        println(summary)
        makeReport(result)
        makeReportEvalAmount(result)
    }

    /**
     *  복수개의 조건에 대한 분석 요약 리포트를 만듦
     */
    fun makeSummaryReport(conditionList: List<AnalysisMabsCondition>) {
        var i = 0
        val resultList = conditionList.map { condition ->
            val tradeItemHistory = trade(condition)
            val analysis = analysis(tradeItemHistory, condition)
            log.info("분석 진행 ${++i}/${conditionList.size}")
            analysis
        }.toList()

        val header = "분석기간,분석 아이디,종목,투자비율,최초 투자금액,매수 수수료,매도 수수료," +
                "조건 설명," +
                "매수 후 보유 수익,매수 후 보유 MDD,매수 후 보유 CAGR," +
                "실현 수익,실현 MDD,실현 CAGR,매매 횟수,승률,"
        val report = StringBuilder(header.replace(",", "\t") + "\n")

        resultList.forEach { result ->
            // 개별 매매 리포트 만듦
            makeReport(result)

            val multiCondition = result.analysisMabsCondition
            val tradeConditionList = multiCondition.tradeConditionList

            val reportRow = StringBuilder()
            reportRow.append(String.format("%s\t", multiCondition.range))
            reportRow.append(
                String.format(
                    "%s\t",
                    tradeConditionList.joinToString("|") { it.mabsConditionSeq.toString() }
                )
            )
            reportRow.append(String.format("%s\t", tradeConditionList.joinToString(",") { it.stock.name }))
            reportRow.append(String.format("%,.2f%%\t", multiCondition.investRatio * 100))
            reportRow.append(String.format("%,d\t", multiCondition.cash))
            reportRow.append(String.format("%,.2f%%\t", multiCondition.feeBuy * 100))
            reportRow.append(String.format("%,.2f%%\t", multiCondition.feeSell * 100))

            reportRow.append(String.format("%s\t", multiCondition.comment))

            val sumYield: TotalYield = result.buyAndHoldYieldTotal
            reportRow.append(String.format("%,.2f%%\t", sumYield.yield * 100))
            reportRow.append(String.format("%,.2f%%\t", sumYield.mdd * 100))
            reportRow.append(String.format("%,.2f%%\t", sumYield.getCagr() * 100))

            val totalYield: TotalYield = result.yieldTotal
            reportRow.append(String.format("%,.2f%%\t", totalYield.yield * 100))
            reportRow.append(String.format("%,.2f%%\t", totalYield.mdd * 100))
            reportRow.append(String.format("%,.2f%%\t", totalYield.getCagr() * 100))
            reportRow.append(String.format("%d\t", result.getWinningRateTotal().getTradeCount()))
            reportRow.append(String.format("%,.2f%%\t", result.getWinningRateTotal().getWinRate() * 100))

            report.append(reportRow).append("\n")

            log.info("리포트생성 진행 ${++i}/${conditionList.size}")
        }

        report.append("\n\n\n========================== 매매 조건 정보 ==========================\n")

        val conditionHeader = "분석 아이디,종목이름,종목코드,매매주기,단기 이동평균 기간,장기 이동평균 기간,하락매도률,상승매도률"
        report.append(conditionHeader.replace(",", "\t") + "\n")

        val mabsConditionList: List<MabsConditionEntity> = conditionList
            .flatMap { it.tradeConditionList }
            .distinct()
            .toList()

        for (condition in mabsConditionList) {
            val reportRow = StringBuilder()
            reportRow.append(String.format("%s\t", condition.mabsConditionSeq))
            reportRow.append(String.format("%s\t", condition.stock.name))
            reportRow.append(String.format("%s\t", condition.stock.code))
            reportRow.append(String.format("%s\t", condition.periodType))
            reportRow.append(String.format("%d\t", condition.shortPeriod))
            reportRow.append(String.format("%d\t", condition.longPeriod))
            reportRow.append(String.format("%,.2f%%\t", condition.downSellRate * 100))
            reportRow.append(String.format("%,.2f%%\t", condition.upBuyRate * 100))
            report.append(reportRow).append("\n")
        }

        // 결과 저장
        val reportFile =
            File("./backtest-result", "이평선돌파_전략_백테스트_분석결과_" + Timestamp.valueOf(LocalDateTime.now()).time + ".txt")
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr")
        println("결과 파일:" + reportFile.name)
    }


    /**
     * 매매 백테스트
     */
    private fun trade(condition: AnalysisMabsCondition): ArrayList<TradeReportItem> {
        val rangeInList: List<List<MabsTradeEntity>> =
            condition.tradeConditionList.map { mainList ->
                mainList.tradeList.filter { condition.range.isBetween(it.tradeDate) }
            }
                .toList()

        val tradeAllList = rangeInList.flatMap { tradeList ->
            val subList = tradeList.stream()
                // 첫 거래가 매도이면 삭제
                .skip(if (tradeList[0].tradeType == SELL) 1 else 0)
                .toList()
            if (subList.size > 1) subList else emptyList()
        }.sortedWith(compareBy { it.tradeDate }).toList()

        if (tradeAllList.size < 2) {
            throw RuntimeException("매수/매도 기록이 없습니다.")
        }

        var cash = condition.cash
        val tradeItemHistory = ArrayList<TradeReportItem>()
        val buyStock = HashMap<String, TradeReportItem>()
        tradeAllList.forEach { tradeItem ->
            if (tradeItem.tradeType == BUY) {
                val buyCash = getBuyCash(buyStock.size, cash, condition.tradeConditionList.size, condition.investRatio)

                val buyQty: Int = (buyCash / tradeItem.unitPrice).toInt()
                val buyAmount: Int = buyQty * tradeItem.unitPrice
                val feePrice = (condition.feeBuy * buyAmount).toInt()
                cash -= buyAmount + feePrice
                val stockEvalPrice = buyStock.entries.map { it.value }
                    .sumOf { it.mabsTradeEntity.unitPrice.toLong() * it.qty } + buyQty * tradeItem.unitPrice
                val tradeReportItem = TradeReportItem(
                    mabsTradeEntity = tradeItem,
                    qty = buyQty,
                    cash = cash,
                    feePrice = feePrice,
                    gains = 0,
                    stockEvalPrice = stockEvalPrice
                )
                tradeItemHistory.add(tradeReportItem)
                buyStock[tradeItem.mabsConditionEntity.stock.code] = tradeReportItem
            } else if (tradeItem.tradeType == SELL) {
                // 투자수익금 = 매수금액 * 수익률 - 수수료
                val buyTrade = buyStock[tradeItem.mabsConditionEntity.stock.code]
                    ?: throw RuntimeException("${tradeItem.mabsConditionEntity.stock.code} 매수 내역이 없습니다.")
                buyStock.remove(tradeItem.mabsConditionEntity.stock.code)
                val sellPrice = (buyTrade.getBuyAmount() * (1 + tradeItem.yield)).toLong()
                val sellFee = (sellPrice * condition.feeSell).toInt()
                val gains = (sellPrice - buyTrade.getBuyAmount())

                // 매매후 현금
                cash += sellPrice - sellFee

                val stockEvalPrice =
                    buyStock.entries.map { it.value }.sumOf { it.mabsTradeEntity.unitPrice.toLong() * it.qty }
                val tradeReportItem = TradeReportItem(
                    mabsTradeEntity = tradeItem,
                    qty = 0,
                    cash = cash,
                    feePrice = sellFee,
                    gains = gains,
                    stockEvalPrice = stockEvalPrice
                )
                tradeItemHistory.add(tradeReportItem)
            }
        }
        return tradeItemHistory
    }

    /**
     * [currentBuyStockCount] 현재 매수중인 종목 수
     * [cash] 현재 보유 현금
     * [stockBuyTotalCount] 매매 대상 종목수
     * [investRatio] 전체 현금 대비 투자 비율. 1: 모든 현금을 투자, 0.5 현금의 50%만 매수에 사용
     *
     * @return 매수에 사용될 금액 반환
     */
    private fun getBuyCash(
        currentBuyStockCount: Int,
        cash: Long,
        stockBuyTotalCount: Int,
        investRatio: Double
    ): Double {
        // 매수에 사용할 현금
        // 현재현금 * 직전 매수 종목 수 / 매매 대상 종목수 * 사용비율 * 매매 대상 종목수  / 사용비율 / (매매 대상 종목수 / 사용비율 - 직전 매수 종목 수) + 현재현금
        val startCash =
            cash * currentBuyStockCount / stockBuyTotalCount * investRatio * stockBuyTotalCount / investRatio / (stockBuyTotalCount / investRatio - currentBuyStockCount) + cash
        // 매수에 사용할 현금 = 시작현금 역산 * 사용비율 * (1/매매종목수)
        return startCash * investRatio * (1 / stockBuyTotalCount.toDouble())
    }

    /**
     * 매매 결과에 대한 통계적 분석을 함
     */
    private fun analysis(
        tradeItemHistory: ArrayList<TradeReportItem>, condition: AnalysisMabsCondition
    ): AnalysisReportResult {
        // 날짜별로 Buy&Hold 및 투자전략 평가금액 얻기
        val evaluationAmountHistory = applyEvaluationAmount(tradeItemHistory, condition)

        val buyAndHoldYieldMdd: TotalYield = calculateTotalBuyAndHoldYield(evaluationAmountHistory, condition.range)
        val buyAndHoldYieldCondition: Map<Int, YieldMdd> =
            calculateBuyAndHoldYield(condition)

        val yieldTotal: TotalYield = calculateTotalYield(evaluationAmountHistory, condition.range)
        val winningRate: Map<Int, WinningRate> = calculateCoinInvestment(tradeItemHistory)

        return AnalysisReportResult(
            analysisMabsCondition = condition,
            tradeHistory = tradeItemHistory,
            evaluationAmountHistory = evaluationAmountHistory,
            yieldTotal = yieldTotal,
            winningRateCondition = winningRate,
            buyAndHoldYieldCondition = buyAndHoldYieldCondition,
            buyAndHoldYieldTotal = buyAndHoldYieldMdd,
        )
    }

    /**
     * @return 날짜별 평가금 계산
     */
    private fun applyEvaluationAmount(
        tradeItemHistory: ArrayList<TradeReportItem>,
        condition: AnalysisMabsCondition
    ): List<EvaluationAmountItem> {
        val buyHoldMap: SortedMap<LocalDateTime, Long> = getBuyAndHoldEvalAmount(condition)
        // <조건아아디, List(캔들)>
        val candleListMap = getConditionOfCandle(condition)

        // <조건아이디, Map<날짜, 종가>>
        val condClosePriceMap: Map<Int, Map<LocalDateTime, Int>> =
            getConditionByClosePriceMap(condition.tradeConditionList, candleListMap)

        val allDateList =
            condClosePriceMap.entries.flatMap { it.value.entries }.map { it.key }.toSortedSet()

        var buyHoldLastAmount = condition.cash
        var backtestLastCash = condition.cash // 마지막 보유 현금

        // <거래날짜, 거래내용>
        val tradeByDate: Map<LocalDateTime, List<TradeReportItem>> =
            tradeItemHistory.groupBy { it.mabsTradeEntity.tradeDate }

        // 현재 가지고 있는 주식 수
        // <조건아이디, 주식수>
        val condByStockQty = condition.tradeConditionList.associate { it.mabsConditionSeq to 0 }.toMutableMap()

        return allDateList.map { date ->
            val buyHoldAmount = buyHoldMap[date] ?: buyHoldLastAmount
            val currentTradeList = tradeByDate[date] ?: emptyList()
            for (trade in currentTradeList) {
                val mabsConditionSeq = trade.mabsTradeEntity.mabsConditionEntity.mabsConditionSeq
                condByStockQty[mabsConditionSeq] = trade.qty
                backtestLastCash = trade.cash
            }

            // 종가기준으로 보유 주식 평가금액 구하기
            val evalStockAmount =
                condByStockQty.entries.stream().filter { it.value > 0 }
                    .mapToLong {
                        val closePrice = condClosePriceMap[it.key]!![date]
                            ?: throw RuntimeException("${date}에 대한 조건아이디(${it.key})의 종가 정보가 없습니다.")
                        closePrice * it.value.toLong()
                    }.sum()


            val backtestAmount = backtestLastCash + evalStockAmount
            buyHoldLastAmount = buyHoldAmount
            EvaluationAmountItem(baseDate = date, buyHoldAmount = buyHoldAmount, backtestAmount = backtestAmount)
        }.toList()

    }

    /**
     * @return 수익률 정보
     */
    private fun calculateTotalYield(
        evaluationAmountList: List<EvaluationAmountItem>, range: DateRange
    ): TotalYield {
        if (evaluationAmountList.isEmpty()) {
            return TotalYield(
                yield = 0.0, mdd = 0.0, dayCount = range.diffDays.toInt()
            )
        }

        val lastCash = evaluationAmountList.last().backtestAmount
        val startCash = evaluationAmountList.first().backtestAmount
        val realYield = ApplicationUtil.getYield(startCash, lastCash)

        val finalResultList = evaluationAmountList.stream().map(EvaluationAmountItem::backtestAmount).toList()
        val realMdd = ApplicationUtil.getMddByLong(finalResultList)
        return TotalYield(realYield, realMdd, range.diffDays.toInt())
    }

    /**
     * @return <조건아이디, 투자 종목 수익 정보>
     */
    private fun calculateCoinInvestment(
        tradeItemHistory: ArrayList<TradeReportItem>
    ): Map<Int, WinningRate> {
        val sellList = tradeItemHistory.filter { it.mabsTradeEntity.tradeType == SELL }.toList()
        val groupBy: Map<Int, List<TradeReportItem>> =
            sellList.groupBy { it.mabsTradeEntity.mabsConditionEntity.mabsConditionSeq }

        return groupBy.entries.associate { entity ->
            val totalInvest = entity.value.sumOf { it.gains }
            val gainCount = entity.value.count { it.gains > 0 }
            entity.key to WinningRate(gainCount, entity.value.size - gainCount, totalInvest)
        }.toMap()
    }


    /**
     * @return <조건아이디, 투자 종목에 대한 Buy & Hold시 수익 정보>
     */
    private fun calculateBuyAndHoldYield(
        condition: AnalysisMabsCondition,
    ): Map<Int, YieldMdd> {
        val mapOfCandleList = getConditionOfCandle(condition)

        // <조건아이디, 직전 가격>
        val mapOfBeforePrice = getConditionOfFirstOpenPrice(condition.tradeConditionList, mapOfCandleList)

        return mapOfCandleList.entries.associate { entry ->
            val priceHistory = entry.value.stream().map { it.closePrice }.toList().toMutableList()
            // 해당 캔들의 시초가를 맨 앞에 넣기
            priceHistory.add(0, mapOfBeforePrice[entry.key])

            entry.key to YieldMdd(
                ApplicationUtil.getYieldByInt(priceHistory),
                ApplicationUtil.getMddByInt(priceHistory)
            )
        }
    }


    /**
     * @return 전체 투자 종목에 대한 Buy & Hold시 수익 정보
     */
    private fun calculateTotalBuyAndHoldYield(
        evaluationAmountList: List<EvaluationAmountItem>,
        range: DateRange
    ): TotalYield {
        val prices = evaluationAmountList.map { it.buyHoldAmount }.toList()
        return TotalYield(
            ApplicationUtil.getYieldByLong(prices),
            ApplicationUtil.getMddByLong(prices),
            range.diffDays.toInt()
        )
    }

    /**
     * Buy & Hold 투자금액 대비 날짜별 평가금액
     * @return <날짜, 평가금액>
     */
    private fun getBuyAndHoldEvalAmount(condition: AnalysisMabsCondition): SortedMap<LocalDateTime, Long> {
        val combinedYield: SortedMap<LocalDateTime, Double> = calculateBuyAndHoldProfitRatio(condition)
        val initial = TreeMap<LocalDateTime, Long>()
        initial[condition.range.from] = condition.cash
        return combinedYield.entries.fold(initial) { acc: SortedMap<LocalDateTime, Long>, item ->
            // 누적수익 = 직전 누적수익 * (수익률 + 1)
            acc[item.key] = (acc.entries.last().value * (item.value + 1)).toLong()
            acc
        }
    }

    /**
     * 수익비는 1에서 시작함
     * @return <날짜, 수익비>
     */
    private fun calculateBuyAndHoldProfitRatio(condition: AnalysisMabsCondition): SortedMap<LocalDateTime, Double> {
        val range = condition.range

        val tradeConditionList = condition.tradeConditionList
        // <조건아아디, List(캔들)>
        val mapOfCandleList = getConditionOfCandle(condition)

        // <조건아이디, Map<날짜, 종가>>
        val mapOfCondClosePrice: Map<Int, Map<LocalDateTime, Int>> =
            getConditionByClosePriceMap(tradeConditionList, mapOfCandleList)

        // <조건아이디, 직전 가격>
        val mapOfBeforePrice = getConditionOfFirstOpenPrice(tradeConditionList, mapOfCandleList)
        var currentDate = range.from
        // <날짜, Map<조건아이디, 상대 수익률>>
        val mapOfDayRelativeRate = mutableMapOf<LocalDateTime, Map<Int, Double>>()
        while (currentDate.isBefore(range.to) || (currentDate == range.to)) {
            // Map<조건아이디, 상대 수익률>
            val mapCondRelativeRate: Map<Int, Double> = mapOfCondClosePrice.entries
                .filter { it.value[currentDate] != null }
                .associate {
                    val beforePrice = mapOfBeforePrice[it.key]
                    val closePrice = it.value[currentDate]!!
                    val relativeYield = closePrice / beforePrice!!.toDouble() - 1
                    mapOfBeforePrice[it.key] = closePrice
                    it.key to relativeYield
                }

            if (mapCondRelativeRate.isNotEmpty()) {
                mapOfDayRelativeRate[currentDate] = mapCondRelativeRate
            }
            currentDate = currentDate.plusDays(1)
        }

        // <날짜, 합산수익률>
        return mapOfDayRelativeRate.entries
            .associate { dayOfItem -> dayOfItem.key to dayOfItem.value.values.toList().average() }
            .toSortedMap()
    }

    /**
     * @return <조건아이디, Map<날짜, 종가>>
     */
    private fun getConditionByClosePriceMap(
        tradeConditionList: List<MabsConditionEntity>,
        candleListMpa: Map<Int, List<CandleEntity>>
    ): Map<Int, Map<LocalDateTime, Int>> {
        return tradeConditionList.associate { tradeCondition ->
            tradeCondition.mabsConditionSeq to (candleListMpa[tradeCondition.mabsConditionSeq]
                ?.map { it.candleDateTime to it.closePrice })!!.toMap()
        }
    }

    /**
     * @return <조건아이디, 최초 가격>
     */
    private fun getConditionOfFirstOpenPrice(
        conditionList: List<MabsConditionEntity>,
        mapOfCandleList: Map<Int, List<CandleEntity>>
    ): MutableMap<Int, Int?> {
        return conditionList.associate {
            it.mabsConditionSeq to mapOfCandleList[it.mabsConditionSeq]?.get(0)?.openPrice
        }
            .toMutableMap()
    }

    /**
     *@return <조건아아디, List(캔들)>
     */
    private fun getConditionOfCandle(condition: AnalysisMabsCondition): Map<Int, List<CandleEntity>> {
        return condition.tradeConditionList.associate { tradeCondition ->
            tradeCondition.mabsConditionSeq to candleRepository.findByRange(
                tradeCondition.stock,
                condition.range.from,
                condition.range.to
            )
        }
    }

    /**
     * 분석 요약결과
     */
    private fun getSummary(result: AnalysisReportResult): String {
        val report = StringBuilder()
        val tradeConditionList = result.analysisMabsCondition.tradeConditionList

        report.append("----------- Buy&Hold 결과 -----------\n")
        report.append(String.format("합산 동일비중 수익\t %,.2f%%", result.buyAndHoldYieldTotal.yield * 100)).append("\n")
        report.append(String.format("합산 동일비중 MDD\t %,.2f%%", result.buyAndHoldYieldTotal.mdd * 100)).append("\n")
        report.append(String.format("합산 동일비중 CAGR\t %,.2f%%", result.buyAndHoldYieldTotal.getCagr() * 100)).append("\n")

        for (i in 1..tradeConditionList.size) {
            val tradeCondition = tradeConditionList[i - 1]
            report.append("${i}. 조건번호: ${tradeCondition.mabsConditionSeq}, 종목: ${tradeCondition.stock.name}(${tradeCondition.stock.code}), 단기-장기(${tradeCondition.periodType}): ${tradeCondition.shortPeriod}-${tradeCondition.longPeriod}\n")
            val sumYield = result.buyAndHoldYieldCondition[tradeCondition.mabsConditionSeq]
            if (sumYield == null) {
                log.warn("조건에 해당하는 결과가 없습니다. mabsConditionSeq: ${tradeCondition.mabsConditionSeq}")
                break
            }
            report.append(String.format("${i}. 동일비중 수익\t %,.2f%%", sumYield.yield * 100)).append("\n")
            report.append(String.format("${i}. 동일비중 MDD\t %,.2f%%", sumYield.mdd * 100)).append("\n")
        }


        val totalYield: TotalYield = result.yieldTotal
        report.append("----------- 전략 결과 -----------\n")
        report.append(String.format("합산 실현 수익\t %,.2f%%", totalYield.yield * 100)).append("\n")
        report.append(String.format("합산 실현 MDD\t %,.2f%%", totalYield.mdd * 100)).append("\n")
        report.append(String.format("합산 매매회수\t %d", result.getWinningRateTotal().getTradeCount())).append("\n")
        report.append(String.format("합산 승률\t %,.2f%%", result.getWinningRateTotal().getWinRate() * 100)).append("\n")
        report.append(String.format("합산 CAGR\t %,.2f%%", totalYield.getCagr() * 100)).append("\n")

        for (i in 1..tradeConditionList.size) {
            val tradeCondition = tradeConditionList[i - 1]
            report.append("${i}. 조건번호: ${tradeCondition.mabsConditionSeq}, 종목: ${tradeCondition.stock.name}(${tradeCondition.stock.code}), 단기-장기(${tradeCondition.periodType}): ${tradeCondition.shortPeriod}-${tradeCondition.longPeriod}\n")

            val winningRate = result.winningRateCondition[tradeCondition.mabsConditionSeq]
            if (winningRate == null) {
                log.warn("조건에 해당하는 결과가 없습니다. mabsConditionSeq: ${tradeCondition.mabsConditionSeq}")
                break
            }
            report.append(String.format("${i}. 실현 수익\t %,d", winningRate.invest)).append("\n")
            report.append(String.format("${i}. 매매회수\t %d", winningRate.getTradeCount())).append("\n")
            report.append(String.format("${i}. 승률\t %,.2f%%", winningRate.getWinRate() * 100)).append("\n")
        }
        return report.toString()
    }

    /**
     * 매매 결과를 파일로 만듦
     */
    private fun makeReport(result: AnalysisReportResult) {
        val header =
            "날짜,종목,매매 구분,단기 이동평균,장기 이동평균,매수 수량,매매 금액,체결 가격,최고수익률,최저수익률,실현 수익률,수수료,투자 수익(수수료포함),보유 주식 평가금,매매후 보유 현금,평가금(주식+현금),수익비"
        val report = StringBuilder(header.replace(",", "\t")).append("\n")

        result.tradeHistory.forEach { row: TradeReportItem ->
            val mabsTradeEntity: MabsTradeEntity = row.mabsTradeEntity
            val mabsConditionEntity: MabsConditionEntity = mabsTradeEntity.mabsConditionEntity
            val tradeDate: LocalDateTime = mabsTradeEntity.tradeDate
            val dateStr: String = DateUtil.formatDateTime(tradeDate)
            report.append(String.format("%s\t", dateStr))
            report.append(String.format("%s\t", mabsConditionEntity.stock.getNameCode()))
            report.append(String.format("%s\t", mabsTradeEntity.tradeType))
            report.append(String.format("%,d\t", mabsTradeEntity.maShort))
            report.append(String.format("%,d\t", mabsTradeEntity.maLong))
            report.append(String.format("%,d\t", row.qty))
            report.append(String.format("%,d\t", row.getBuyAmount()))
            report.append(String.format("%,d\t", mabsTradeEntity.unitPrice))
            report.append(String.format("%,.2f%%\t", mabsTradeEntity.highYield * 100))
            report.append(String.format("%,.2f%%\t", mabsTradeEntity.lowYield * 100))
            report.append(String.format("%,.2f%%\t", mabsTradeEntity.yield * 100))
            report.append(String.format("%,d\t", row.feePrice))
            report.append(String.format("%,d\t", row.gains))
            report.append(String.format("%,d\t", row.stockEvalPrice))
            report.append(String.format("%,d\t", row.cash))
            report.append(String.format("%,d\t", row.getEvalPrice()))
            report.append(
                String.format(
                    "%,.2f\n",
                    row.getEvalPrice() / result.analysisMabsCondition.cash.toDouble()
                )
            )
        }

        report.append("\n\n")
        report.append(getSummary(result))
        report.append("\n\n")
        report.append(getConditionSummary(result))

        val reportFileSubPrefix = getReportFileSuffix(result.analysisMabsCondition.tradeConditionList, result)
        val reportFile = File("./backtest-result/trade-report", "trade_" + reportFileSubPrefix)
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr")
        println("결과 파일:" + reportFile.name)
    }

    /**
     * 날짜에 따른 평가금액(Buy&Hold, 벡테스트) 변화 리포트
     */
    private fun makeReportEvalAmount(result: AnalysisReportResult) {
        val header =
            "날짜,Buy&Hold 평가금,백테스트 평가금"
        val report = StringBuilder(header.replace(",", "\t")).append("\n")

        result.evaluationAmountHistory.forEach { row: EvaluationAmountItem ->
            val dateStr: String = DateUtil.formatDateTime(row.baseDate)
            report.append(String.format("%s\t", dateStr))
            report.append(String.format("%,d\t", row.buyHoldAmount))
            report.append(String.format("%,d\n", row.backtestAmount))
        }

        report.append("\n\n")
        report.append(getSummary(result))
        report.append("\n\n")
        report.append(getConditionSummary(result))

        val reportFileSubPrefix = getReportFileSuffix(result.analysisMabsCondition.tradeConditionList, result)
        val reportFile = File("./backtest-result/trade-report", "trade_eval_" + reportFileSubPrefix)
        FileUtils.writeStringToFile(reportFile, report.toString(), "euc-kr")
        println("결과 파일:" + reportFile.name)
    }

    /**
     * 백테스트 조건 요약 정보
     */
    private fun getConditionSummary(
        result: AnalysisReportResult
    ): String {
        val range: DateRange = result.analysisMabsCondition.range
        val condition = result.analysisMabsCondition

        val report = StringBuilder()

        report.append("----------- 백테스트 조건 -----------\n")
        report.append(String.format("분석기간\t %s", range)).append("\n")
        report.append(String.format("투자비율\t %,.2f%%", condition.investRatio * 100)).append("\n")
        report.append(String.format("최초 투자금액\t %,d", condition.cash)).append("\n")
        report.append(String.format("매수 수수료\t %,.2f%%", condition.feeBuy * 100)).append("\n")
        report.append(String.format("매도 수수료\t %,.2f%%", condition.feeSell * 100)).append("\n")

        val tradeConditionList: List<MabsConditionEntity> = result.analysisMabsCondition.tradeConditionList

        for (i in 1..tradeConditionList.size) {
            val tradeCondition = tradeConditionList[i - 1]
            report.append(String.format("${i}. 조건아이디\t %s", tradeCondition.mabsConditionSeq)).append("\n")
            report.append(String.format("${i}. 분석주기\t %s", tradeCondition.periodType)).append("\n")
            report.append(String.format("${i}. 대상 코인\t %s", tradeCondition.stock.getNameCode())).append("\n")
            report.append(String.format("${i}. 상승 매수률\t %,.2f%%", tradeCondition.upBuyRate * 100)).append("\n")
            report.append(String.format("${i}. 하락 매도률\t %,.2f%%", tradeCondition.downSellRate * 100)).append("\n")
            report.append(String.format("${i}. 단기 이동평균 기간\t %d", tradeCondition.shortPeriod)).append("\n")
            report.append(String.format("${i}. 장기 이동평균 기간\t %d", tradeCondition.longPeriod)).append("\n")
        }
        return report.toString()

    }

    /**
     * @return 조건 정보가 담긴 리포트 파일명 subfix
     */
    private fun getReportFileSuffix(
        tradeConditionList: List<MabsConditionEntity>,
        result: AnalysisReportResult
    ): String {
        return String.format(
            "%s_%s~%s_%s.txt",
            tradeConditionList.joinToString(",") { it.mabsConditionSeq.toString() },
            result.analysisMabsCondition.range.fromDateFormat,
            result.analysisMabsCondition.range.toDateFormat,
            tradeConditionList.joinToString(",") { it.stock.code }
        )
    }
}