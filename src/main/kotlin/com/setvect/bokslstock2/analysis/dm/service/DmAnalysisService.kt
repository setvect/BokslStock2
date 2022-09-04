package com.setvect.bokslstock2.analysis.dm.service

import com.setvect.bokslstock2.analysis.common.model.*
import com.setvect.bokslstock2.analysis.common.service.BacktestTradeService
import com.setvect.bokslstock2.analysis.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.analysis.dm.model.DmBacktestCondition
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.entity.StockEntity
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.index.service.MovingAverageService
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import com.setvect.bokslstock2.util.DateUtil
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * 듀얼모멘텀 백테스트
 */
@Service
class DmAnalysisService(
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
        val preTrades: List<PreTrade>,
        /**
         * 기간별 모멘텀 스코어
         */
        val momentumScoreList: List<MomentumScore>
    )

    fun runTest(dmBacktestCondition: DmBacktestCondition) {
        checkValidate(dmBacktestCondition)
        val momentumResult = processDualMomentum(dmBacktestCondition)
        val tradeCondition = makeTradeDateCorrection(dmBacktestCondition, momentumResult.preTrades)
        val trades = backtestTradeService.trade(tradeCondition, momentumResult.preTrades)
        val result = backtestTradeService.analysis(trades, tradeCondition, dmBacktestCondition.stockCodes)
        val summary = getSummary(dmBacktestCondition, result.common)
        println(summary)
        makeReportFile(dmBacktestCondition, result, momentumResult.momentumScoreList)
    }

    fun makeSummaryReport(conditionList: List<DmBacktestCondition>): File {
        var i = 0
        val conditionResults = conditionList.map { dmBacktestCondition ->
            checkValidate(dmBacktestCondition)
            val momentumResult = processDualMomentum(dmBacktestCondition)
            val tradeCondition = makeTradeDateCorrection(dmBacktestCondition, momentumResult.preTrades)
            val trades = backtestTradeService.trade(tradeCondition, momentumResult.preTrades)
            val analysisResult = backtestTradeService.analysis(trades, tradeCondition, dmBacktestCondition.stockCodes)


            log.info("분석 진행 ${++i}/${conditionList.size}")
            Triple(dmBacktestCondition, analysisResult, momentumResult.momentumScoreList)
        }.toList()

        for (idx in conditionResults.indices) {
            val conditionResult = conditionResults[idx]
            makeReportFile(conditionResult.first, conditionResult.second, conditionResult.third)
            log.info("개별분석파일 생성 ${idx + 1}/${conditionList.size}")
        }

        // 결과 저장
        val reportFile =
            File("./backtest-result", "듀얼모멘텀_전략_백테스트_분석결과_" + Timestamp.valueOf(LocalDateTime.now()).time + ".xlsx")
        XSSFWorkbook().use { workbook ->
            val sheetBacktestSummary = createTotalSummary(workbook, conditionResults)
            workbook.setSheetName(workbook.getSheetIndex(sheetBacktestSummary), "1. 평가표")

            FileOutputStream(reportFile).use { ous ->
                workbook.write(ous)
            }
        }
        println("결과 파일:" + reportFile.name)
        return reportFile
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

        val basic = TradeCondition(
            range = realRange,
            investRatio = 0.999,
            cash = 10_000_000.0,
            feeBuy = 0.001,
            feeSell = 0.001,
            comment = "",
            benchmark = listOf()
        )

        val condition = DmBacktestCondition(
            tradeCondition = basic,
            stockCodes = stockCodes,
            holdCode = holdCode,
            periodType = PeriodType.PERIOD_MONTH,
            timeWeight = timeWeight,
            endSell = true
        )
        val stockPriceIndex = getStockPriceIndex(condition.listStock())
        val momentumScoreList = calcMomentumScores(condition, stockPriceIndex)
        return momentumScoreList.first { it.date == date }
    }


    /**
     * `나도` 이렇게 하기 싫다.
     * 비주얼포트폴리오 매매 전략과 동일하게 맞추기 위해서 직전 종가 기준으로 매매가 이루어져야 되기 때문에 백테스트 시작 시점 조정이 필요하다.
     */
    private fun makeTradeDateCorrection(
        dmBacktestCondition: DmBacktestCondition,
        preTrades: List<PreTrade>
    ): TradeCondition {
        val temp = dmBacktestCondition.tradeCondition
        val from =
            if (temp.range.from.isBefore(preTrades.first().tradeDate)) temp.range.from else preTrades.first().tradeDate
        val to = if (temp.range.to.isAfter(preTrades.last().tradeDate)) temp.range.to else preTrades.last().tradeDate
        return TradeCondition(
            DateRange(from, to),
            temp.investRatio,
            temp.cash,
            temp.feeBuy,
            temp.feeSell,
            temp.comment,
            temp.benchmark
        )
    }

    private fun processDualMomentum(condition: DmBacktestCondition): DualMomentumResult {
        val stockCodes = condition.listStock()
        // <종목코드, <날짜, 캔들>>
        val stockPriceIndex = getStockPriceIndex(stockCodes)
        val momentumScoreList = calcMomentumScores(condition, stockPriceIndex)

        // <종목코드, 종목정보>
        val codeByStock = stockCodes.associateWith { stockRepository.findByCode(it.code).get() }
        val tradeList = mutableListOf<PreTrade>()
        var beforeBuyTrade: PreTrade? = null
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
                val changeBuyStock = beforeBuyTrade != null && beforeBuyTrade.stockCode.code != condition.holdCode!!.code
                val existHoldCode = condition.holdCode != null

                if (changeBuyStock) {
                    val code = StockCode.findByCode(beforeBuyTrade!!.stockCode.code)
                    // 보유 종목 매도
                    val sellStock = stockPriceIndex[code]!![momentumScore.date]!!
                    val sellTrade = makeSellTrade(sellStock, beforeBuyTrade)
                    tradeList.add(sellTrade)
                    beforeBuyTrade = null
                }
                if (existHoldCode && (beforeBuyTrade == null || beforeBuyTrade.stockCode.code != condition.holdCode!!.code)) {
                    // hold 종목 매수
                    val buyStock = stockPriceIndex[condition.holdCode]!![momentumScore.date]!!
                    val stock = codeByStock[condition.holdCode]!!
                    val buyTrade = makeBuyTrade(buyStock, stock)
                    tradeList.add(buyTrade)
                    beforeBuyTrade = buyTrade
                } else if (existHoldCode) {
                    log.info(
                        "매수 유지: $momentumScore.date, ${condition.holdCode!!.desc}" +
                            "(${condition.holdCode})"
                    )
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
                    tradeList.add(sellTrade)
                }
                if (beforeBuyTrade == null || changeBuyStock) {
                    // 새운 종목 매수
                    val buyStock = stockPriceIndex[stockCode]!![momentumScore.date]!!
                    val stock = codeByStock[stockCode]!!
                    val buyTrade = makeBuyTrade(buyStock, stock)
                    tradeList.add(buyTrade)
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
            }
        }

        return DualMomentumResult(tradeList, momentumScoreList)
    }


    /**
     * @return 해당 날짜에 모든 종목에 대한 가격이 존재하면 true
     */
    private fun isExistStockIndex(stockPriceIndex: Map<StockCode, Map<LocalDate, CandleDto>>, date: LocalDate): Boolean {
        return stockPriceIndex.entries.all { it.value[date] != null }
    }

    /**
     * 모멘텀 스코어 계산
     */
    private fun calcMomentumScores(
        condition: DmBacktestCondition,
        stockPriceIndex: Map<StockCode, Map<LocalDate, CandleDto>>
    ): List<MomentumScore> {
        val range = backtestTradeService.fitBacktestRange(condition.stockCodes, condition.tradeCondition.range, condition.maxWeightMonth() + 1)
//        log.info("범위 조건 변경: ${condition.tradeCondition.range} -> $range")
//        condition.tradeCondition.range = range

        var current =
            DateUtil.fitMonth(
                condition.tradeCondition.range.from.withDayOfMonth(1),
                condition.periodType.getDeviceMonth()
            )
        val momentumScoreList = mutableListOf<MomentumScore>()
        while (current.isBefore(condition.tradeCondition.range.to.toLocalDate()) || current.isEqual(condition.tradeCondition.range.to.toLocalDate())) {
            // 현재 월의 이전 종가를 기준으로 계산해야 되기 때문에 직전월에 모멘텀 지수를 계산함
            val baseDate = current.minusMonths(1)
            val stockRate = calculateRate(stockPriceIndex, baseDate, condition)
            momentumScoreList.add(MomentumScore(current, stockRate.toMap()))
            current = current.plusMonths(condition.periodType.getDeviceMonth().toLong())
        }
        return momentumScoreList.toList()
    }


    private fun makeBuyTrade(
        targetStock: CandleDto,
        stock: StockEntity
    ): PreTrade {
        val buyTrade = PreTrade(
            stockCode = StockCode.findByCode(stock.code),
            tradeType = TradeType.BUY,
            yield = 0.0,
            unitPrice = targetStock.openPrice,
            tradeDate = targetStock.candleDateTimeStart,
        )
        log.info("매수: ${targetStock.candleDateTimeStart}(${buyTrade.tradeDate}), ${buyTrade.stockCode.name}(${buyTrade.stockCode.code})")
        return buyTrade
    }


    private fun makeSellTrade(
        targetStock: CandleDto,
        beforeBuyTrade: PreTrade
    ): PreTrade {
        val sellTrade = PreTrade(
            stockCode = beforeBuyTrade.stockCode,
            tradeType = TradeType.SELL,
            yield = ApplicationUtil.getYield(beforeBuyTrade.unitPrice, targetStock.openPrice),
            unitPrice = targetStock.openPrice,
            tradeDate = targetStock.candleDateTimeStart,
        )
        log.info("매도: ${targetStock.candleDateTimeStart}(${sellTrade.tradeDate}), ${sellTrade.stockCode.name}(${sellTrade.stockCode.code}), 수익: ${sellTrade.yield}")
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
     * 분석건에 대한 리포트 파일 만듦
     * @return 엑셀 파일
     */
    private fun makeReportFile(
        dmBacktestCondition: DmBacktestCondition,
        analysisResult: AnalysisResult,
        momentumScoreList: List<MomentumScore>
    ): File {
        val append = "_${dmBacktestCondition.timeWeight.entries.map { it.key }.joinToString(",")}"
        val reportFileSubPrefix =
            ReportMakerHelperService.getReportFileSuffix(
                dmBacktestCondition.tradeCondition,
                dmBacktestCondition.listStock(),
                append
            )
        val reportFile = File(
            "./backtest-result/dm-trade-report",
            "dm_trade_${reportFileSubPrefix}"
        )

        XSSFWorkbook().use { workbook ->
            var sheet = ReportMakerHelperService.createTradeReport(analysisResult, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "1. 매매이력")

            sheet =
                ReportMakerHelperService.createReportEvalAmount(analysisResult.common.evaluationAmountHistory, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "2. 일자별 자산비율 변화")

            sheet = ReportMakerHelperService.createReportRangeReturn(analysisResult.common.getMonthlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "3. 월별 수익률")

            sheet = ReportMakerHelperService.createReportRangeReturn(analysisResult.common.getYearlyYield(), workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "4. 년별 수익률")

            sheet = createReportSummary(dmBacktestCondition, analysisResult, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "5. 매매 요약결과 및 조건")

            sheet = createMomentumScore(dmBacktestCondition, momentumScoreList, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "6. 모멘텀 지수")

            sheet.createFreezePane(0, 1)

            FileOutputStream(reportFile).use { ous ->
                workbook.write(ous)
            }
        }
        println("결과 파일:" + reportFile.name)
        return reportFile
    }

    /**
     * 매매 결과 요약 및 조건 시트 만듦
     */
    private fun createReportSummary(
        dmBacktestCondition: DmBacktestCondition,
        analysisResult: AnalysisResult,
        workbook: XSSFWorkbook
    ): XSSFSheet {
        val sheet = workbook.createSheet()
        val summary = getSummary(dmBacktestCondition, analysisResult.common)
        ReportMakerHelperService.textToSheet(summary, sheet)
        sheet.defaultColumnWidth = 60
        return sheet
    }


    /**
     * 기간별 듀얼모멘텀 지수 엑셀 파일 만듦
     * [dateOfStockRate]: <날짜, <종목코드, 지수>>
     */
    private fun createMomentumScore(
        dmBacktestCondition: DmBacktestCondition,
        dateOfStockRate: List<MomentumScore>,
        workbook: XSSFWorkbook,
    ): XSSFSheet {
        val sheet = workbook.createSheet()
        val listStock = dmBacktestCondition.listStock()
        val headerColumns = listStock.map { it.code }.toMutableList()
        headerColumns.add(0, "날짜")
        ReportMakerHelperService.applyHeader(sheet, headerColumns)
        var rowIdx = 1

        val dateStyle = ReportMakerHelperService.ExcelStyle.createYearMonth(workbook)
        val commaDecimalStyle = ReportMakerHelperService.ExcelStyle.createCommaDecimal(workbook)
        dateOfStockRate.forEach { momentumScore ->
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
        ReportMakerHelperService.ExcelStyle.applyAllBorder(sheet)
        ReportMakerHelperService.ExcelStyle.applyDefaultFont(sheet)
        return sheet
    }


    /**
     * 분석 요약결과
     */
    private fun getSummary(
        dmBacktestCondition: DmBacktestCondition,
        commonAnalysisReportResult: CommonAnalysisReportResult
    ): String {
        val report = StringBuilder()

        report.append("----------- Buy&Hold 결과 -----------\n")
        val buyHoldText = ApplicationUtil.makeSummaryCompareStock(
            commonAnalysisReportResult.benchmarkTotalYield.buyHoldTotalYield,
            commonAnalysisReportResult.getBuyHoldSharpeRatio()
        )
        report.append(buyHoldText)

        report.append("----------- Benchmark 결과 -----------\n")
        val benchmarkText = ApplicationUtil.makeSummaryCompareStock(
            commonAnalysisReportResult.benchmarkTotalYield.benchmarkTotalYield,
            commonAnalysisReportResult.getBenchmarkSharpeRatio()
        )
        report.append(benchmarkText)

        val totalYield: CommonAnalysisReportResult.TotalYield = commonAnalysisReportResult.yieldTotal
        report.append("----------- 전략 결과 -----------\n")
        report.append(String.format("합산 실현 수익\t %,.2f%%", totalYield.yield * 100)).append("\n")
        report.append(String.format("합산 실현 MDD\t %,.2f%%", totalYield.mdd * 100)).append("\n")
        report.append(String.format("합산 매매회수\t %d", commonAnalysisReportResult.getWinningRateTotal().getTradeCount()))
            .append("\n")
        report.append(
            String.format(
                "합산 승률\t %,.2f%%",
                commonAnalysisReportResult.getWinningRateTotal().getWinRate() * 100
            )
        )
            .append("\n")
        report.append(String.format("합산 CAGR\t %,.2f%%", totalYield.getCagr() * 100)).append("\n")
        report.append(String.format("샤프지수\t %,.2f", commonAnalysisReportResult.getBacktestSharpeRatio())).append("\n")

        report.append("----------- 테스트 조건 -----------\n")
        val stockName = dmBacktestCondition.stockCodes.joinToString(", ") { "${it.code}[${it.name}]" }
        val holdStockName = Optional.ofNullable(dmBacktestCondition.holdCode).map { code ->
            stockRepository
                .findByCode(code.code)
                .map { "${it.code}[${it.name}]" }
                .orElse("")
        }.orElse("")
        val timeWeight = dmBacktestCondition.timeWeight.entries
            .sortedBy { it.key }
            .joinToString(", ") { "${it.key}월:${String.format("%.2f%%", it.value * 100)}" }

        val tradeCondition = dmBacktestCondition.tradeCondition
        report.append("모멘텀 대상종목\t${stockName}").append("\n")
        report.append("홀드 종목\t$holdStockName").append("\n")
        report.append("거래주기\t${dmBacktestCondition.periodType}").append("\n")
        report.append("기간별 가중치\t$timeWeight").append("\n")
        report.append("분석 대상 기간\t${tradeCondition.range}").append("\n")
        report.append("투자 비율\t${String.format("%,.2f%%", tradeCondition.investRatio * 100)}").append("\n")
        report.append("최초 투자금액\t${String.format("%,.0f", tradeCondition.cash)}").append("\n")
        report.append("매수 수수료\t${String.format("%,.2f%%", tradeCondition.feeBuy * 100)}").append("\n")
        report.append("매도 수수료\t${String.format("%,.2f%%", tradeCondition.feeSell * 100)}").append("\n")
        report.append("설명\t${tradeCondition.comment}").append("\n")

        return report.toString()
    }


    /**
     * @return 여러개 백테스트 결과 요약 시트
     */
    private fun createTotalSummary(
        workbook: XSSFWorkbook,
        conditionResults: List<Triple<DmBacktestCondition, AnalysisResult, List<MomentumScore>>>
    ): XSSFSheet {
        val sheet = workbook.createSheet()

        val header = "분석기간,거래종목,홀드종목,가중치기간 및 비율,투자비율,최초 투자금액,매수 수수료,매도 수수료," +
            "조건 설명," +
            "매수 후 보유 수익,매수 후 보유 MDD,매수 후 보유 CAGR,매수 후 샤프지수," +
            "밴치마크 보유 수익,밴치마크 보유 MDD,밴치마크 보유 CAGR,밴치마크 샤프지수," +
            "실현 수익,실현 MDD,실현 CAGR,샤프지수,매매 횟수,승률"
        ReportMakerHelperService.applyHeader(sheet, header)
        var rowIdx = 1

        val defaultStyle = ReportMakerHelperService.ExcelStyle.createDefault(workbook)
        val dateStyle = ReportMakerHelperService.ExcelStyle.createDate(workbook)
        val commaStyle = ReportMakerHelperService.ExcelStyle.createComma(workbook)
        val percentStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        val decimalStyle = ReportMakerHelperService.ExcelStyle.createDecimal(workbook)
        val percentImportantStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        percentImportantStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        percentImportantStyle.fillForegroundColor = IndexedColors.LEMON_CHIFFON.index

        conditionResults.forEach { conditionResult ->
            val multiCondition = conditionResult.first

            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0
            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.tradeCondition.range.toString())
            createCell.cellStyle = dateStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.stockCodes.joinToString(","))
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.holdCode!!.code)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            val timeWeight =
                multiCondition.timeWeight.entries.map { "${it.key}월: ${it.value * 100}%" }.joinToString(", ")
            createCell.setCellValue(timeWeight)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.tradeCondition.investRatio)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.tradeCondition.cash)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.tradeCondition.feeBuy)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.tradeCondition.feeSell)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.tradeCondition.comment)
            createCell.cellStyle = defaultStyle

            val result = conditionResult.second

            val buyHoldTotalYield: CommonAnalysisReportResult.TotalYield =
                result.common.benchmarkTotalYield.buyHoldTotalYield
            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(buyHoldTotalYield.yield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(buyHoldTotalYield.mdd)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(buyHoldTotalYield.getCagr())
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(result.common.getBuyHoldSharpeRatio())
            createCell.cellStyle = decimalStyle

            val benchmarkTotalYield: CommonAnalysisReportResult.TotalYield =
                result.common.benchmarkTotalYield.benchmarkTotalYield
            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(benchmarkTotalYield.yield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(benchmarkTotalYield.mdd)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(benchmarkTotalYield.getCagr())
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(result.common.getBenchmarkSharpeRatio())
            createCell.cellStyle = decimalStyle

            val totalYield: CommonAnalysisReportResult.TotalYield = result.common.yieldTotal

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(totalYield.yield)
            createCell.cellStyle = percentImportantStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(totalYield.mdd)
            createCell.cellStyle = percentImportantStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(totalYield.getCagr())
            createCell.cellStyle = percentImportantStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(result.common.getBacktestSharpeRatio())
            createCell.cellStyle = decimalStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(result.common.getWinningRateTotal().getTradeCount().toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx)
            createCell.setCellValue(result.common.getWinningRateTotal().getWinRate())
            createCell.cellStyle = percentStyle
        }
        sheet.createFreezePane(0, 1)
        sheet.defaultColumnWidth = 14
        sheet.setColumnWidth(0, 12000)
        sheet.setColumnWidth(1, 5000)
        sheet.setColumnWidth(2, 5000)

        ReportMakerHelperService.ExcelStyle.applyAllBorder(sheet)
        ReportMakerHelperService.ExcelStyle.applyDefaultFont(sheet)

        return sheet
    }

}