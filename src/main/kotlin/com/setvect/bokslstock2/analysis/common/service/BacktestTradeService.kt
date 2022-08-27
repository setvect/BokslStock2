package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.model.*
import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import kotlin.streams.toList

/**
 * 백테스팅 결과를 이용해 매매 분석
 *
 * 본 클래스는 매매 전략과 독립적으로 동작해야됨.
 * 즉 특정 매매전략에 의존적인 코드가 들어가면 안됨
 */
@Service
class BacktestTradeService(
    val candleRepository: CandleRepository,
    val stockRepository: StockRepository,
) {
    /**
     * @return 수수료등 각종 조건을 적용시킨 매매 내역
     */
    fun trade(condition: TradeCondition, preTrades: List<PreTrade>): List<Trade> {
        return tradeBundle(condition, listOf(preTrades))
    }

    fun tradeBundle(condition: TradeCondition, preTrades: List<List<PreTrade>>): List<Trade> {
        val bundleCount = preTrades.size

        val tradeAllList = filterPreTrade(preTrades, condition)

        if (tradeAllList.isEmpty()) {
            throw RuntimeException("매수/매도 기록이 없습니다.")
        }
        var cash = condition.cash
        val tradeItemHistory = mutableListOf<Trade>()
        // <종목코드, 직전 preTrade>
        val buyStock = HashMap<String, Trade>()

        tradeAllList.forEach { tradeItem ->
            if (tradeItem.tradeType == TradeType.BUY) {
                // 매수 처리
                val buyCash =
                    ReportMakerHelperService.getBuyCash(
                        buyStock.size,
                        cash,
                        bundleCount,
                        condition.investRatio
                    )

                val buyQty: Int = (buyCash / tradeItem.unitPrice).toInt()
                val buyAmount = buyQty * tradeItem.unitPrice
                val feePrice = condition.feeBuy * buyAmount
                cash -= buyAmount + feePrice
                val stockEvalPrice = buyStock.entries.map { it.value }
                    .sumOf { it.preTrade.unitPrice * it.qty } + buyQty * tradeItem.unitPrice

                val tradeReportItem = Trade(
                    preTrade = tradeItem,
                    qty = buyQty,
                    cash = cash,
                    feePrice = feePrice,
                    gains = 0.0,
                    stockEvalPrice = stockEvalPrice
                )
                tradeItemHistory.add(tradeReportItem)
                buyStock[tradeItem.stockCode.code] = tradeReportItem
            } else if (tradeItem.tradeType == TradeType.SELL) {
                // 매도 처리
                // 투자수익금 = 매수금액 * 수익률 - 수수료
                val buyTrade = buyStock[tradeItem.stockCode.code]
                    ?: throw RuntimeException("${tradeItem.stockCode.code} 매수 내역이 없습니다.")
                buyStock.remove(tradeItem.stockCode.code)
                val sellPrice = buyTrade.getBuyAmount() * (1 + tradeItem.yield)
                val sellFee = sellPrice * condition.feeSell
                val gains = sellPrice - buyTrade.getBuyAmount()

                // 매매후 현금
                cash += sellPrice - sellFee

                val stockEvalPrice =
                    buyStock.entries.map { it.value }.sumOf { it.preTrade.unitPrice * it.qty }

                val tradeReportItem = Trade(
                    preTrade = tradeItem,
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
     * @return 조건에 해당하는 매매 내역만 필터링
     */
    private fun filterPreTrade(
        preTrades: List<List<PreTrade>>,
        condition: TradeCondition
    ): List<PreTrade> {
        val matchPreTrades = preTrades
            .flatMap { p ->
                val rangeTrade = p.stream()
                    .filter { condition.range.isBetween(it.tradeDate) }
                    .toList()

                // 첫 거래가 매도이면 삭제
                val compactTrade = rangeTrade.stream()
                    .skip(if (rangeTrade[0].tradeType == TradeType.SELL) 1 else 0)
                    .toList()
                if (compactTrade.size > 1) compactTrade else emptyList()
            }
            .sortedWith(compareBy { it.tradeDate }).toList()
        return matchPreTrades
    }


    /**
     * 매매 결과에 대한 통계적 분석을 함
     */
    fun analysis(
        trades: List<Trade>, condition: TradeCondition, holdStockCodes: List<StockCode>
    ): AnalysisResult {
        // 대상 종목으로 날짜별로 Buy&Hold 및 투자전략 평가금액 얻기
        val evaluationAmountHistory = applyEvaluationAmount(trades, condition, holdStockCodes)

        val benchmarkTotalYield =
            ReportMakerHelperService.calculateTotalBenchmarkYield(evaluationAmountHistory, condition.range)
        val buyHoldYieldByCode = calculateBenchmarkYield(condition.range, holdStockCodes)
        val benchmarkYieldByCode = calculateBenchmarkYield(condition.range, condition.benchmark)

        val yieldTotal = ReportMakerHelperService.calculateTotalYield(evaluationAmountHistory, condition.range)
        val winningRate = calculateCoinInvestment(trades)

        val common = CommonAnalysisReportResult(
            evaluationAmountHistory = evaluationAmountHistory,
            yieldTotal = yieldTotal,
            winningRateTarget = winningRate,
            baseStockYieldCode = CompareYieldCode(buyHoldYieldByCode, benchmarkYieldByCode),
            benchmarkTotalYield = benchmarkTotalYield,
        )
        return AnalysisResult(condition, trades, common)
    }


    /**
     * @return 날짜별 평가금 계산
     */
    fun applyEvaluationAmount(
        trades: List<Trade>,
        condition: TradeCondition,
        holdStockCodes: List<StockCode>
    ): List<EvaluationRateItem> {

        val useStockCode = trades.map { it.preTrade.stockCode.code }.distinct()
        // <종목 코드, List(캔들)>
        val candleListMap = getConditionOfCandle(condition.range, useStockCode)

        // <종목 코드, Map<날짜, 종가>>
        val condClosePriceMap = getConditionByClosePriceMap(candleListMap)

        val allDateList =
            condClosePriceMap.entries
                .flatMap { it.value.entries }
                .map { it.key }.toSortedSet()

        var buyHoldLastRate = 1.0
        var benchmarkLastRate = 1.0
        var backtestLastRate = 1.0
        var backtestLastCash = condition.cash // 마지막 보유 현금

        // <거래날짜, 거래내용>
        val tradeByDate = trades.groupBy { it.preTrade.tradeDate }

        // 현재 가지고 있는 주식 수
        // <종목 코드, 주식수>
        val condByStockQty = trades
            .map { it.preTrade.stockCode.code }
            .distinct()
            .associateWith { 0 }
            .toMutableMap()

        val buyHoldRateMap: SortedMap<LocalDateTime, Double> = getBuyAndHoldEvalRate(condition.range, holdStockCodes)
        val benchmarkRateMap: SortedMap<LocalDateTime, Double> =
            getBuyAndHoldEvalRate(condition.range, condition.benchmark)

        val result = allDateList.map { date ->
            val buyHoldRate = buyHoldRateMap[date] ?: buyHoldLastRate
            val benchmarkRate = benchmarkRateMap[date] ?: benchmarkLastRate
            val currentTradeList = tradeByDate[date] ?: emptyList()
            for (trade in currentTradeList) {
                val stockCode = trade.preTrade.stockCode.code
                condByStockQty[stockCode] = trade.qty
                backtestLastCash = trade.cash
            }

            // 종가기준으로 보유 주식 평가금액 구하기
            val evalStockAmount =
                condByStockQty.entries.stream().filter { it.value > 0 }
                    .mapToDouble {

                        val closePrice: Double = getBeforeNearPrice(condClosePriceMap[it.key]!!, date, it.key)
                        closePrice * it.value
                    }.sum()


            val backtestRate = (backtestLastCash + evalStockAmount) / condition.cash
            val buyHoldYield = ApplicationUtil.getYield(buyHoldLastRate, buyHoldRate)
            val benchmarkYield = ApplicationUtil.getYield(benchmarkLastRate, benchmarkRate)
            val backtestYield = ApplicationUtil.getYield(backtestLastRate, backtestRate)

            buyHoldLastRate = buyHoldRate
            benchmarkLastRate = benchmarkRate
            backtestLastRate = backtestRate
            EvaluationRateItem(
                baseDate = date,
                buyHoldRate = buyHoldRate,
                benchmarkRate = benchmarkRate,
                backtestRate = backtestRate,
                buyHoldYield = buyHoldYield,
                benchmarkYield = benchmarkYield,
                backtestYield = backtestYield
            )
        }.toMutableList()
        // 최초 시작은 비율은 1.0
        result.add(
            0,
            EvaluationRateItem(
                baseDate = allDateList.first(),
                buyHoldRate = 1.0,
                benchmarkRate = 1.0,
                backtestRate = 1.0,
                buyHoldYield = 0.0,
                benchmarkYield = 0.0,
                backtestYield = 0.0,
            )
        )
        return result
    }

    /**
     * [date]날짜와 가장 가까운 가격을 반환함. 최대 7일 이전까지 가격을 찾고 없으면 예외 발생
     */
    private fun getBeforeNearPrice(condClosePriceMap: Map<LocalDateTime, Double>, date: LocalDateTime, code: String): Double {
        var closePrice: Double? = null
        for (i in 0L..7L) {
            closePrice = condClosePriceMap[date.minusDays(i)]
            if (closePrice != null) {
                break
            }
        }
        if (closePrice == null) {
            throw RuntimeException("${date}에 대한 조건아이디(${code})의 종가 정보가 없습니다.")
        }
        return closePrice
    }

    /**
     * Buy&Hold 종목을 동일 비중으로 매수 했을 경우 수익률 제공
     * @return 날짜별 Buy&Hold 수익률. <날짜, 수익비>
     */
    fun getBuyAndHoldEvalRate(range: DateRange, holdStockCodes: List<StockCode>): SortedMap<LocalDateTime, Double> {
        val combinedYield: SortedMap<LocalDateTime, Double> = calculateBuyAndHoldProfitRatio(range, holdStockCodes)
        val initial = TreeMap<LocalDateTime, Double>()
        initial[range.from] = 1.0
        return combinedYield.entries.fold(initial) { acc: SortedMap<LocalDateTime, Double>, item ->
            // 누적수익 = 직전 누적수익 * (수익률 + 1)
            acc[item.key] = acc.entries.last().value * (item.value + 1)
            acc
        }
    }


    /**
     * 수익비는 1에서 시작함
     * @return <날짜, 수익비>
     */
    fun calculateBuyAndHoldProfitRatio(
        range: DateRange,
        holdStockCodes: List<StockCode>
    ): SortedMap<LocalDateTime, Double> {

        // <종목코드, List(캔들)>
        val mapOfCandleList = getConditionOfCandle(range, holdStockCodes.map { it.code })

        // <종목코드, Map<날짜, 종가>>
        val mapOfCondClosePrice = getConditionByClosePriceMap(mapOfCandleList)

        // <종목코드, 직전 가격>
        val mapOfBeforePrice = getConditionOfFirstOpenPrice(mapOfCandleList)
        var currentDate = range.from

        // <날짜, Map<종목코드, 상대 수익률>>
        val mapOfDayRelativeRate = mutableMapOf<LocalDateTime, Map<String, Double>>()
        while (currentDate.isBefore(range.to) || (currentDate == range.to)) {
            // Map<종목코드, 상대 수익률>
            val mapCondRelativeRate: Map<String, Double> = mapOfCondClosePrice.entries
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
            .associate { dayOfItem ->
                dayOfItem.key to dayOfItem.value.values.toList().average()
            }
            .toSortedMap()
    }


    /**
     * @return <종목 아이디, 투자 종목에 대한 Buy & Hold시 수익 정보>
     */
    fun calculateBenchmarkYield(
        range: DateRange,
        stockCodes: List<StockCode>,
    ): Map<StockCode, CommonAnalysisReportResult.YieldMdd> {
        val mapOfCandleList = getConditionOfCandle(range, stockCodes.map { it.code })

        // <조건아이디, 직전 가격>
        val mapOfBeforePrice = getConditionOfFirstOpenPrice(mapOfCandleList)

        return mapOfCandleList.entries.associate { entry ->
            val priceHistory = entry.value.stream().map { it.closePrice }.toList().toMutableList()
            // 해당 캔들의 시초가를 맨 앞에 넣기
            priceHistory.add(0, mapOfBeforePrice[entry.key])
            val yieldMdd = CommonAnalysisReportResult.YieldMdd(
                ApplicationUtil.getYield(priceHistory),
                ApplicationUtil.getMdd(priceHistory)
            )
            StockCode.findByCode(entry.key) to yieldMdd
        }
    }

    /**
     *@return <종목코드, List(캔들)>
     */
    fun getConditionOfCandle(range: DateRange, stockCodes: List<String>): Map<String, List<CandleEntity>> {
        return stockCodes.associateWith { stockCode ->
            candleRepository.findByRange(
                stockRepository.findByCode(stockCode).get(),
                range.from,
                range.to
            )
        }
    }

    /**
     * @return <종목코드, 투자 종목 수익 정보>
     */
    fun calculateCoinInvestment(
        tradeItemHistory: List<Trade>
    ): Map<String, CommonAnalysisReportResult.WinningRate> {
        val sellList = tradeItemHistory.filter { it.preTrade.tradeType == TradeType.SELL }.toList()
        val groupBy = sellList.groupBy { it.preTrade.stockCode.code }

        return groupBy.entries.associate { entity ->
            val totalInvest = entity.value.sumOf { it.gains }
            val gainCount = entity.value.count { it.gains > 0 }
            entity.key to CommonAnalysisReportResult.WinningRate(
                gainCount,
                entity.value.size - gainCount,
                totalInvest
            )
        }.toMap()
    }


    /**
     * @return <종목코드, Map<날짜, 종가>>
     */
    companion object {
        fun getConditionByClosePriceMap(
            candleListMap: Map<String, List<CandleEntity>>
        ): Map<String, Map<LocalDateTime, Double>> {
            return candleListMap.entries.associate { code ->
                code.key to code.value.associate { it.candleDateTime to it.closePrice }
            }
        }

        /**
         * @return <종목코드, 최초 가격>
         */
        fun getConditionOfFirstOpenPrice(
            mapOfCandleList: Map<String, List<CandleEntity>>
        ): MutableMap<String, Double> {
            return mapOfCandleList.entries.associate {
                it.key to it.value[0].openPrice
            }.toMutableMap()
        }
    }
}