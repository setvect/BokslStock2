package com.setvect.bokslstock2.backtest.dm.service

import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.backtest.common.model.TradeNeo
import com.setvect.bokslstock2.backtest.common.service.BacktestTradeService
import com.setvect.bokslstock2.backtest.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.backtest.common.service.SheetAppendMaker
import com.setvect.bokslstock2.backtest.dm.model.DmBacktestCondition
import com.setvect.bokslstock2.common.model.TradeType
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.DateUtil
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

/**
 * 듀얼모멘텀 백테스트
 */
@Service
class DmBacktestService(
    private val stockRepository: StockRepository,
    private val backtestTradeService: BacktestTradeService,
    private val candleRepository: CandleRepository,
    private val movingAverageService: MovingAverageService
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 날짜별 모멘터 스코어
     */
    data class MomentumScore(
        val date: LocalDate,
        /**
         * <종목코드, 모멘텀스코어>
         */
        val score: Map<StockCode, Double>,
    )

    /**
     * 모멘텀 결과
     */
    data class DualMomentumResult(
        /**
         * 거래 내역
         */
        val tradeList: List<TradeNeo>,
        /**
         * 기간별 모멘텀 스코어
         */
        val momentumScoreList: List<MomentumScore>
    )

    fun runTest(dmBacktestCondition: DmBacktestCondition): DualMomentumResult {
        checkValidate(dmBacktestCondition)
        val momentumScoreList = calcMomentumScores(dmBacktestCondition)
        val tradeList = processDualMomentum(dmBacktestCondition, momentumScoreList)
        return DualMomentumResult(tradeList, momentumScoreList)
    }

    /**
     * @return [date]에 대한 모멘텀 값
     */
    fun getMomentumScore(
        date: LocalDate,
        stockCodes: List<StockCode>,
        holdCode: StockCode,
        timeWeight: Map<Int, Double>
    ): MomentumScore {
        // TODO 모멘텀 스코어 계산을 위한 로직으로 변경. 현재는 필요 없는 연산이 많음
        val from = date.atTime(0, 0)
        val to = date.atTime(0, 0)
        val realRange = DateRange(from, to)

        val condition = DmBacktestCondition(
            range = realRange,
            investRatio = 0.999,
            cash = 10_000_000.0,
            stockCodes = stockCodes,
            holdCode = holdCode,
            periodType = PeriodType.PERIOD_MONTH,
            timeWeight = timeWeight,
            endSell = true
        )
        val stockPriceIndex = getStockPriceIndex(condition.listStock())
        val momentumScoreList = calcMomentumScores(condition)
        return momentumScoreList.first { it.date == date }
    }

    /**
     * @return 종목별 모멘텀 스코어
     */
    private fun processDualMomentum(condition: DmBacktestCondition, momentumScoreList: List<MomentumScore>): List<TradeNeo> {
        val stockCodes = condition.listStock()
        // <종목코드, <날짜, 캔들>>
        val stockPriceIndex = getStockPriceIndex(stockCodes)

        // <종목코드, 종목정보>
        val codeByStock = stockCodes.associateWith { stockRepository.findByCode(it.code).get() }
        val tradeList = mutableListOf<TradeNeo>()
        var beforeBuyTrade: TradeNeo? = null
        var currentCash = condition.cash

        for (momentumScore in momentumScoreList) {
            val stockRate = momentumScore.score
            val momentTargetRate = stockRate.entries
                .filter { it.key != condition.holdCode }
                .filter { it.value >= 1 }
                .sortedByDescending { it.value }

            if (!isExistStockIndex(stockPriceIndex, momentumScore.date)) {
                log.info("${momentumScore.date} 가격 정보 없음")
                break
            }

            // 듀얼 모멘텀 매수 대상 종목이 없으면, hold 종목 매수 또는 현금 보유
            if (momentTargetRate.isEmpty()) {
                // TODO condition.holdCode == null 이면 오류 남. 확인해 볼것
                val changeBuyStock = beforeBuyTrade != null && beforeBuyTrade.stockCode.code != condition.holdCode!!.code
                val existHoldCode = condition.holdCode != null

                if (changeBuyStock) {
                    val code = StockCode.findByCode(beforeBuyTrade!!.stockCode.code)
                    // 보유 종목 매도
                    val sellStock = stockPriceIndex[code]!![momentumScore.date]!!
                    val sellTrade = makeSellTrade(sellStock, beforeBuyTrade)

                    currentCash += sellTrade.getAmount()
                    tradeList.add(sellTrade)
                    beforeBuyTrade = null
                }
                if (existHoldCode && (beforeBuyTrade == null || beforeBuyTrade.stockCode.code != condition.holdCode!!.code)) {
                    // hold 종목 매수
                    val buyStock = stockPriceIndex[condition.holdCode]!![momentumScore.date]!!
                    val stock = codeByStock[condition.holdCode]!!
                    val buyTrade = makeBuyTrade(buyStock, stock, currentCash * condition.investRatio)
                    currentCash -= buyTrade.getAmount()
                    tradeList.add(buyTrade)
                    beforeBuyTrade = buyTrade
                } else if (existHoldCode) {
                    log.info("매수 유지: $momentumScore.date, ${condition.holdCode!!.desc}(${condition.holdCode})")
                }
            } else {
                val buyStockRate = momentTargetRate[0]
                val stockCode = buyStockRate.key
                val changeBuyStock = beforeBuyTrade != null && beforeBuyTrade.stockCode.code != stockCode.code

                if (changeBuyStock) {
                    val code = StockCode.findByCode(beforeBuyTrade!!.stockCode.code)
                    // 보유 종목 매도
                    val sellStock = stockPriceIndex[code]!![momentumScore.date]!!
                    val sellTrade = makeSellTrade(sellStock, beforeBuyTrade)
                    currentCash += sellTrade.getAmount()
                    tradeList.add(sellTrade)
                }
                if (beforeBuyTrade == null || changeBuyStock) {
                    // 새운 종목 매수
                    val buyStock = stockPriceIndex[stockCode]!![momentumScore.date]!!
                    val stock = codeByStock[stockCode]!!
                    val buyTrade = makeBuyTrade(buyStock, stock, currentCash * condition.investRatio)
                    tradeList.add(buyTrade)
                    currentCash -= buyTrade.getAmount()
                    beforeBuyTrade = buyTrade
                } else {
                    log.info("매수 유지: $momentumScore.date, ${beforeBuyTrade.stockCode.name}(${beforeBuyTrade.stockCode.code})")
                }
            }
        }

        // 마지막 보유 종목 매도
        if (condition.endSell && beforeBuyTrade != null) {
            val date = momentumScoreList.last().date.plusMonths(condition.periodType.getDeviceMonth().toLong())
            val code = StockCode.findByCode(beforeBuyTrade.stockCode.code)
            val sellStock = stockPriceIndex[code]!![date]
            if (sellStock != null) {
                val sellTrade = makeSellTrade(sellStock, beforeBuyTrade)
                tradeList.add(sellTrade)
            } else {
                // 마지막 시세로 매도
                val candleEntityList =
                    candleRepository.findByBeforeLastCandle(
                        beforeBuyTrade.stockCode.code,
                        date.atTime(0, 0),
                        PeriodType.PERIOD_DAY,
                        PageRequest.of(0, 1)
                    )
                val candleEntity = candleEntityList[0]
                val candleDto = CandleDto(
                    stockCode = StockCode.findByCode(candleEntity.stock.code),
                    candleDateTimeStart = candleEntity.candleDateTime,
                    candleDateTimeEnd = candleEntity.candleDateTime,
                    beforeCandleDateTimeEnd = candleEntity.candleDateTime,
                    beforeClosePrice = candleEntity.closePrice,
                    openPrice = candleEntity.openPrice,
                    highPrice = candleEntity.highPrice,
                    lowPrice = candleEntity.lowPrice,
                    closePrice = candleEntity.closePrice,
                    periodType = candleEntity.periodType
                )
                val sellTrade = makeSellTrade(candleDto, beforeBuyTrade)
                tradeList.add(sellTrade)
                currentCash += sellTrade.getAmount()
            }
        }

        return tradeList
    }


    /**
     * @return 해당 날짜에 모든 종목에 대한 가격이 존재하면 true
     */
    private fun isExistStockIndex(
        stockPriceIndex: Map<StockCode, Map<LocalDate, CandleDto>>,
        date: LocalDate
    ): Boolean {
        return stockPriceIndex.entries.all { it.value[date] != null }
    }

    /**
     * 모멘텀 스코어 계산
     */
    private fun calcMomentumScores(
        condition: DmBacktestCondition
    ): List<MomentumScore> {
        val stockCodes = condition.listStock()
        // <종목코드, <날짜, 캔들>>
        val stockPriceIndex = getStockPriceIndex(stockCodes)

        val range = backtestTradeService.fitBacktestRange(
            condition.stockCodes,
            condition.range,
            condition.maxWeightMonth() + 1
        )
//        log.info("범위 조건 변경: ${condition.tradeCondition.range} -> $range")
//        condition.tradeCondition.range = range

        var current =
            DateUtil.fitMonth(
                condition.range.from.withDayOfMonth(1),
                condition.periodType.getDeviceMonth()
            )
        val momentumScoreList = mutableListOf<MomentumScore>()
        while (current.isBefore(condition.range.to.toLocalDate()) || current.isEqual(condition.range.to.toLocalDate())) {
            // 현재 월의 이전 종가를 기준으로 계산해야 되기 때문에 직전월에 모멘텀 지수를 계산함
            val baseDate = current.minusMonths(1)
            val stockRate = calculateRate(stockPriceIndex, baseDate, condition)
            momentumScoreList.add(MomentumScore(current, stockRate.toMap()))
            current = current.plusMonths(condition.periodType.getDeviceMonth().toLong())
        }
        return momentumScoreList.toList()
    }

    private fun makeBuyTrade(targetStock: CandleDto, stock: StockEntity, buyCash: Double): TradeNeo {
        val buyTrade = TradeNeo(
            stockCode = StockCode.findByCode(stock.code),
            tradeType = TradeType.BUY,
            price = targetStock.openPrice,
            qty = (buyCash / targetStock.openPrice).toInt(),
            tradeDate = targetStock.candleDateTimeStart,
        )
        log.info(
            "매수: ${targetStock.candleDateTimeStart}(${buyTrade.tradeDate}), " +
                    "${buyTrade.stockCode.name}(${buyTrade.stockCode.code})," +
                    "매수가격: ${buyTrade.price}, " +
                    "수량: ${buyTrade.qty}, "
        )
        return buyTrade
    }


    private fun makeSellTrade(targetStock: CandleDto, beforeBuyTrade: TradeNeo): TradeNeo {
        val sellTrade = TradeNeo(
            stockCode = beforeBuyTrade.stockCode,
            tradeType = TradeType.SELL,
            price = targetStock.openPrice,
            qty = beforeBuyTrade.qty,
            tradeDate = targetStock.candleDateTimeStart,
        )
        log.info(
            "매도: ${targetStock.candleDateTimeStart}(${sellTrade.tradeDate}), " +
                    "${sellTrade.stockCode.name}(${sellTrade.stockCode.code}), " +
                    "매수/매도 가격: ${beforeBuyTrade.price}/${sellTrade.price}, " +
                    "수익율: ${ApplicationUtil.getYield(beforeBuyTrade.price, sellTrade.price)}," +
                    "수익금: ${ApplicationUtil.getYieldPrice(beforeBuyTrade.price, sellTrade.price, beforeBuyTrade.qty)}"
        )
        return sellTrade
    }

    /**
     * TODO 전체 내용을 반환 하도록 변경 <-- 2022년 8월 14일 다시 보니 무슨 의미 인지 모르겠다. ㅜㅜ
     * 듀얼 모멘터 대상 종목을 구함
     * [stockPriceIndex] <종목코드, <날짜, 캔들>>
     * @return <종목코드, 현재가격/모멘텀평균 가격>
     */
    private fun calculateRate(
        stockPriceIndex: Map<StockCode, Map<LocalDate, CandleDto>>,
        current: LocalDate,
        condition: DmBacktestCondition,
    ): List<Pair<StockCode, Double>> {
        val stockByRate = stockPriceIndex.entries.map { stockEntry ->
            val currentCandle = stockEntry.value[current]
                ?: throw RuntimeException("${stockEntry.key}에 대한 $current 시세가 없습니다.")

            log.info(
                "\t현재 날짜: ${current}: ${stockEntry.key}: ${currentCandle.candleDateTimeStart}~${currentCandle.candleDateTimeEnd} - " +
                        "O: ${currentCandle.openPrice}, H: ${currentCandle.highPrice}, L: ${currentCandle.lowPrice}, C:${currentCandle.closePrice}, ${currentCandle.periodType}"
            )

            val momentFormula = StringBuilder()
            // 모멘텀평균 가격(가중치 적용 종가 평균)
            val sumScore = condition.timeWeight.entries.sumOf { timeWeight ->
                val delta = timeWeight.key
                val weight = timeWeight.value
                val deltaCandle = stockEntry.value[current.minusMonths(delta.toLong())]!!
                log.info("\t\t비교 날짜: [${delta}] ${stockEntry.key} - ${deltaCandle.candleDateTimeStart} - C: ${deltaCandle.closePrice}")
                log.info(
                    "\t\t$delta -   ${stockEntry.key}: ${deltaCandle.candleDateTimeStart}~${deltaCandle.candleDateTimeEnd} - " +
                            "직전종가: ${deltaCandle.beforeClosePrice}, O: ${deltaCandle.openPrice}, H: ${deltaCandle.highPrice}, L: ${deltaCandle.lowPrice}, C:${deltaCandle.closePrice}, ${deltaCandle.periodType}, 수익률(현재 종가 / 직전 종가) : ${deltaCandle.getYield()}"
                )

                val score = deltaCandle.closePrice * weight
                momentFormula.append("  ${deltaCandle.closePrice} * $weight = ${score}\n")
                score
            }
            log.info("SUM(\n${momentFormula.toString()}) = $sumScore")
            val rate = currentCandle.closePrice / sumScore
            // 수익률 = 현재 날짜 시가 / 모멘텀평균 가격
            log.info(
                "모멘텀 스코어: ${
                    DateUtil.format(
                        current,
                        "yyyy-MM"
                    )
                }: ${stockEntry.key} = ${currentCandle.closePrice} / $sumScore = $rate"
            )
            log.info("--------------")

            stockEntry.key to rate
        }

        return stockByRate
    }

    /**
     * @return <종목코드, <날짜, 캔들>>
     */
    private fun getStockPriceIndex(
        stockCodes: List<StockCode>,
    ): Map<StockCode, Map<LocalDate, CandleDto>> {
        val stockPriceIndex = stockCodes.associateWith { stockCode ->
            movingAverageService.getMovingAverage(
                stockCode,
                PeriodType.PERIOD_DAY,
                PeriodType.PERIOD_MONTH,
                Collections.emptyList(),
            )
                .associateBy { it.candleDateTimeStart.toLocalDate().withDayOfMonth(1) }
        }
        return stockPriceIndex
    }

    private fun checkValidate(dmCondition: DmBacktestCondition) {
        // TODO 부동소수점으로 인한 합계 오차가 발생할 수 있음. timeWeight 값을 Int 변경
        val sumWeight = dmCondition.timeWeight.entries.sumOf { it.value }
        if (sumWeight != 1.0) {
            throw RuntimeException("가중치의 합계가 100이여 합니다. 현재 가중치 합계: $sumWeight")
        }
        if (dmCondition.periodType != PeriodType.PERIOD_MONTH) {
            throw RuntimeException("듀얼 모멘텀 백테스트는 분석주기가 MONTH 이여야 합니다. 현재 설정: ${dmCondition.periodType}")
        }
    }

    /**
     * 모멘텀 점수 시트 생성
     */
    class MomentumScoreSheetMaker(
        private val momentumScoreList: List<MomentumScore>,
        private val dmBacktestCondition: DmBacktestCondition
    ) : SheetAppendMaker {
        override fun appendSheet(workbook: XSSFWorkbook) {
            val sheet = workbook.createSheet()
            val listStock = dmBacktestCondition.listStock()
            val headerColumns = listStock.map { it.code }.toMutableList()
            headerColumns.add(0, "날짜")
            ReportMakerHelperService.applyHeader(sheet, headerColumns)
            var rowIdx = 1

            val dateStyle = ReportMakerHelperService.ExcelStyle.createYearMonth(workbook)
            val commaDecimalStyle = ReportMakerHelperService.ExcelStyle.createCommaDecimal(workbook)
            momentumScoreList.forEach { momentumScore ->
                val row = sheet.createRow(rowIdx++)
                var cellIdx = 0
                val dateCell = row.createCell(cellIdx++)
                dateCell.setCellValue(momentumScore.date)
                dateCell.cellStyle = dateStyle

                val rate = momentumScore.score

                listStock.forEach {
                    val rateCell = row.createCell(cellIdx++)
                    rateCell.setCellValue(Optional.ofNullable(rate[it]).orElse(0.0))
                    rateCell.cellStyle = commaDecimalStyle
                }
            }
            sheet.createFreezePane(0, 1)
            ReportMakerHelperService.ExcelStyle.applyAllBorder(sheet)
            ReportMakerHelperService.ExcelStyle.applyDefaultFont(sheet)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "6. 모멘텀 지수")
        }
    }
}