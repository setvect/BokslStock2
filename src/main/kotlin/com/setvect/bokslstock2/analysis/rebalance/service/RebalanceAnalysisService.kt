package com.setvect.bokslstock2.analysis.rebalance.service

import com.setvect.bokslstock2.analysis.common.model.*
import com.setvect.bokslstock2.analysis.common.service.BacktestTradeService
import com.setvect.bokslstock2.analysis.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.analysis.rebalance.model.RebalanceBacktestCondition
import com.setvect.bokslstock2.index.dto.CandleDto
import com.setvect.bokslstock2.index.model.PeriodType
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.index.repository.StockRepository
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

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
        var i = 0

        val conditionResults = conditionList.map { backtestCondition ->
            checkValidate(backtestCondition)
            val trades = processRebalance(backtestCondition)
            val result = backtestTradeService.analysis(
                trades,
                backtestCondition.tradeCondition,
                backtestCondition.stockCodes.map { it.stockCode }
            )
            val summary = getSummary(backtestCondition, result.common)
            log.info(summary)

            log.info("분석 진행 ${++i}/${conditionList.size}")
            Pair(backtestCondition, result)
        }

        for (idx in conditionResults.indices) {
            val conditionResult = conditionResults[idx]
            makeReportFile(conditionResult.first, conditionResult.second)
            log.info("개별분석파일 생성 ${idx + 1}/${conditionList.size}")
        }

        // 결과 저장
        val reportFile =
            File("./backtest-result", "리벨런싱_전략_백테스트_분석결과_" + Timestamp.valueOf(LocalDateTime.now()).time + ".xlsx")
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
     * @return 여러개 백테스트 결과 요약 시트
     */
    private fun createTotalSummary(
        workbook: XSSFWorkbook,
        conditionResults: List<Pair<RebalanceBacktestCondition, AnalysisResult>>
    ): XSSFSheet {
        val sheet = workbook.createSheet()

        val header = "분석기간,거래종목,리벨런싱주기,리벨런싱입계치,종복별비율,투자비율,최초 투자금액,매수 수수료,매도 수수료," +
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
            createCell.setCellValue(multiCondition.rebalanceFacter.periodType.name)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.rebalanceFacter.threshold)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            val stockWeight = multiCondition.stockCodes
                .joinToString(",") { stock ->
                    "${stock.stockCode}:${stock.weight}"
                }
            createCell.setCellValue(stockWeight)
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

    /**
     * 분석건에 대한 리포트 파일 만듦
     * @return 엑셀 파일
     */
    private fun makeReportFile(
        backtestCondition: RebalanceBacktestCondition,
        analysisResult: AnalysisResult
    ): File {
        val rebalanceFacter = backtestCondition.rebalanceFacter
        val stockCodes = backtestCondition.stockCodes
        val append =
            "_${stockCodes.joinToString { it.stockCode }}_${rebalanceFacter.periodType},${rebalanceFacter.threshold}"

        val reportFileSubPrefix =
            ReportMakerHelperService.getReportFileSuffix(
                backtestCondition.tradeCondition,
                backtestCondition.listStock(),
                append
            )
        val reportFile = File(
            "./backtest-result/rebalance-trade-report",
            "rebalance_trade_${reportFileSubPrefix}"
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

            sheet = createReportSummary(backtestCondition, analysisResult, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "5. 매매 요약결과 및 조건")

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
        backtestCondition: RebalanceBacktestCondition,
        analysisResult: AnalysisResult,
        workbook: XSSFWorkbook
    ): XSSFSheet {
        val sheet = workbook.createSheet()
        val summary = getSummary(backtestCondition, analysisResult.common)
        ReportMakerHelperService.textToSheet(summary, sheet)
        sheet.defaultColumnWidth = 60
        return sheet
    }

    private fun getSummary(
        rebalanceBacktestCondition: RebalanceBacktestCondition,
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
        val stockName = rebalanceBacktestCondition
            .stockCodes.joinToString("\n") { stock ->
                stockRepository
                    .findByCode(stock.stockCode)
                    .map { "\t${it.code}[${it.name}]\t비율: ${stock.weight}%" }
                    .orElse("")
            }

        val tradeCondition = rebalanceBacktestCondition.tradeCondition
        report.append("대상종목\n${stockName}").append("\n")
        report.append("리벨런싱 주기\t${rebalanceBacktestCondition.rebalanceFacter.periodType}").append("\n")
        report.append("리벨런싱 입계치\t${rebalanceBacktestCondition.rebalanceFacter.threshold}").append("\n")
        report.append("분석 대상 기간\t${tradeCondition.range}").append("\n")
        report.append("투자 비율\t${String.format("%,.2f%%", tradeCondition.investRatio * 100)}").append("\n")
        report.append("최초 투자금액\t${String.format("%,.0f", tradeCondition.cash)}").append("\n")
        report.append("매수 수수료\t${String.format("%,.2f%%", tradeCondition.feeBuy * 100)}").append("\n")
        report.append("매도 수수료\t${String.format("%,.2f%%", tradeCondition.feeSell * 100)}").append("\n")
        report.append("설명\t${tradeCondition.comment}").append("\n")

        return report.toString()
    }


    private fun processRebalance(condition: RebalanceBacktestCondition): MutableList<Trade> {
        val stockCodes = condition.listStock()
        // <종목코드, <날짜, 캔들>>
        val periodType = condition.rebalanceFacter.periodType
        val extractRange = DateRange(
            ApplicationUtil.fitStartDate(condition.rebalanceFacter.periodType, condition.tradeCondition.range.fromDate),
            ApplicationUtil.fitStartDate(condition.rebalanceFacter.periodType, condition.tradeCondition.range.toDate),
        )

        val stockPriceIndex = backtestTradeService.getStockPriceIndex(stockCodes, periodType, extractRange)

        var current =
            ApplicationUtil.fitStartDate(condition.rebalanceFacter.periodType, condition.tradeCondition.range.fromDate)
        var beforeTrade = TradeInfo(date = current, buyStocks = listOf(), cash = condition.tradeCondition.cash, false)

        val rebalanceTradeHistory = mutableListOf<TradeInfo>()

        while (current.isBefore(condition.tradeCondition.range.toDate) || current.isEqual(condition.tradeCondition.range.to.toLocalDate())) {
            val changeDeviation = beforeTrade.deviation() >= condition.rebalanceFacter.threshold
            // 편차가 기준치 이상 리벨런싱
            if (beforeTrade.buyStocks.isEmpty() || changeDeviation) {
                // 원래는 부분적으로 샀다 팔아야 되는데 개발하기 귄찮아 전체 매도후 매수하는 방법으로 함
                val (buyStocks, afterCash) = rebalance(beforeTrade, condition, stockPriceIndex, current)

                rebalanceTradeHistory.add(TradeInfo(current, buyStocks, afterCash, true))
            }
            // 편차가 기준이 미만이면 종목 가격만 교체
            else {
                val buyStocks = beforeTrade.buyStocks.map {
                    val candleDto: CandleDto = stockPriceIndex[it.candle.code]!![current]!!
                    BuyStock(candleDto, it.qty, it.weight, it.unitPrice)
                }
                rebalanceTradeHistory.add(TradeInfo(current, buyStocks, beforeTrade.cash, false))
            }
            current = incrementDate(periodType, current)
            beforeTrade = rebalanceTradeHistory.last()
        }

        rebalanceTradeHistory.forEach { trade ->
            log.info(
                "날짜: ${trade.date}, " +
                        "종가 평가가격: ${trade.getEvalPriceClose()}, " +
                        "편차: ${String.format("%,.4f", trade.deviation())}, " +
                        "리벨런싱: ${trade.rebalance}"
            )
            trade.buyStocks.forEach { stock ->
                log.info(
                    "\t종목:${stock.candle.code}, " +
                            "수량: ${stock.qty}, " +
                            "종가: ${stock.candle.closePrice}, " +
                            "평가금액: ${stock.getEvalPriceClose()}, " +
                            "설정비중: ${stock.weight}%, " +
                            "현재비중: ${String.format("%,.3f%%", stock.realWeight(trade.getEvalPriceCloseWithoutCash()))}",
                )
            }
        }

        // -----------------------

        val tradeItemHistory = mutableListOf<Trade>()
        // <종목코드, 종목정보>
        val codeByStock = stockCodes.associateWith { stockRepository.findByCode(it).get() }
        // <종목코드, 직전 preTrade>
        val buyStock = HashMap<String, Trade>()

        var currentCash = 0.0

        rebalanceTradeHistory.forEach { rebalanceItem ->
            // 리벨런싱 됐을 때만 매매
            if (!rebalanceItem.rebalance) {
                return@forEach
            }
            if (buyStock.isNotEmpty()) {
                // ---------- 매도
                rebalanceItem.buyStocks.forEach { rebalStock ->
                    val candle: CandleDto = rebalStock.candle
                    val stock = codeByStock[candle.code]!!

                    val preTrade = PreTrade(
                        stock = Stock.of(stock),
                        tradeType = TradeType.SELL,
                        yield = ApplicationUtil.getYield(rebalStock.unitPrice, candle.closePrice),
                        unitPrice = candle.closePrice,
                        tradeDate = candle.candleDateTimeStart,
                    )

                    val buyTrade = buyStock[preTrade.stock.code]
                        ?: throw RuntimeException("${preTrade.stock.code} 매수 내역이 없습니다.")

                    buyStock.remove(preTrade.stock.code)
                    val sellPrice = buyTrade.getBuyAmount() * (1 + preTrade.yield)
                    val sellFee = sellPrice * condition.tradeCondition.feeSell
                    val gains = sellPrice - buyTrade.getBuyAmount()

                    // 매매후 현금
                    currentCash += sellPrice - sellFee
                    val stockEvalPrice =
                        buyStock.entries.map { it.value }.sumOf { it.preTrade.unitPrice * it.qty }
                    val tradeReportItem = Trade(
                        preTrade = preTrade,
                        qty = 0,
                        cash = currentCash,
                        feePrice = sellFee,
                        gains = gains,
                        stockEvalPrice = stockEvalPrice
                    )
                    tradeItemHistory.add(tradeReportItem)
                }
                currentCash += rebalanceItem.cash
            } else {
                currentCash = condition.tradeCondition.cash
            }


            // ---------- 매수
            rebalanceItem.buyStocks.forEach { rebalStock ->
                val candle: CandleDto = rebalStock.candle
                val stock = codeByStock[candle.code]!!

                val preTrade = PreTrade(
                    stock = Stock.of(stock),
                    tradeType = TradeType.BUY,
                    yield = 0.0,
                    unitPrice = candle.openPrice,
                    tradeDate = candle.candleDateTimeStart,
                )
                // 매수 금액
                val buyAmount = rebalStock.getEvalPriceOpen()
                val feePrice = condition.tradeCondition.feeBuy * buyAmount

                // 매수후 현금
                currentCash -= buyAmount + feePrice

                val stockEvalPrice = buyStock.entries.map { it.value }
                    .sumOf { it.preTrade.unitPrice * it.qty } + rebalStock.qty * preTrade.unitPrice

                val tradeReportItem = Trade(
                    preTrade = preTrade,
                    qty = rebalStock.qty,
                    cash = currentCash,
                    feePrice = feePrice,
                    gains = 0.0,
                    stockEvalPrice = stockEvalPrice
                )
                tradeItemHistory.add(tradeReportItem)
                buyStock[preTrade.stock.code] = tradeReportItem
            }
        }
        return tradeItemHistory
    }

    /**
     * 전체 종목을 일괄 매도후 비중에 맞게 다시 매수
     */
    private fun rebalance(
        beforeTrade: TradeInfo,
        condition: RebalanceBacktestCondition,
        stockPriceIndex: Map<String, Map<LocalDate, CandleDto>>,
        current: LocalDate
    ): Pair<List<BuyStock>, Double> {
        // 시작 지점 시가 기준 매도
        val sellAmount =
            beforeTrade.buyStocks.sumOf {
                val candleDto: CandleDto = stockPriceIndex[it.candle.code]!![current]!!
                it.qty * candleDto.openPrice
            }
        val currentCash = sellAmount + beforeTrade.cash

        val buyStocks = condition.stockCodes.map { stock ->
            val candleDto: CandleDto = stockPriceIndex[stock.stockCode]!![current]!!

            val buyPrice = currentCash * (stock.weight / 100.0)
            val quantify = (buyPrice / candleDto.openPrice).toInt()
            BuyStock(candleDto, quantify, stock.weight, candleDto.openPrice)
        }
        val afterCash = currentCash - buyStocks.sumOf { it.getEvalPriceOpen() }
        return Pair(buyStocks, afterCash)
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

    /**
     * 리빌런싱 단위별 매매 내역
     * @property date 리벨런싱 날짜
     * @property buyStocks 매수 종목
     * @property cash 매수 후 남은 현금
     * @property rebalance 해당 매매 주기에서 리벨런싱을 했는지 여부
     */
    data class TradeInfo(
        val date: LocalDate,
        val buyStocks: List<BuyStock>,
        val cash: Double,
        val rebalance: Boolean
    ) {
        /**
         * @return 시초가 기준 현금포함 평가금액
         */
        fun getEvalPriceOpen(): Double {
            return buyStocks.sumOf { it.getEvalPriceOpen() } + cash
        }

        /**
         * @return 종가 기준 현금포함 평가금액
         */
        fun getEvalPriceClose(): Double {
            return buyStocks.sumOf { it.getEvalPriceClose() } + cash
        }

        /**
         * @return 종가 기준 평가금액
         */
        fun getEvalPriceCloseWithoutCash(): Double {
            return buyStocks.sumOf { it.getEvalPriceClose() }
        }

        /**
         * 예를들어 4종목 비중이 모두 25%이면 분산 값은 0
         *
         * A: 0.2
         * B: 0.3
         * C: 0.24
         * D: 0.26
         * 이면 분산값은 0.12
         *
         * @return 종가기준 편차(현금 포함하지 않음)
         */
        fun deviation(): Double {
            val sum = buyStocks.sumOf { it.getEvalPriceClose() }

            return buyStocks.sumOf {
                val d = it.weight / 100.0
                abs(d - (it.getEvalPriceClose() / sum))
            }
        }
    }


    /**
     * @property candle 매수 종목 정보
     * @property qty 매수 수량
     * @property weight 해당 종목의 전체 투자대비 비중(%)
     * @property unitPrice 매수 가격
     */
    data class BuyStock(val candle: CandleDto, val qty: Int, val weight: Int, val unitPrice: Double) {

        fun getEvalPriceOpen(): Double {
            return candle.openPrice * qty
        }

        fun getEvalPriceClose(): Double {
            return candle.closePrice * qty
        }

        /**
         * 종가기준 전체 [totalPrice] 대비 비중(%)
         */
        fun realWeight(totalPrice: Double): Double {
            return getEvalPriceClose() / totalPrice * 100
        }
    }
}