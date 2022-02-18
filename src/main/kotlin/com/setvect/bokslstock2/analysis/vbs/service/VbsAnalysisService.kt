package com.setvect.bokslstock2.analysis.vbs.service

import com.setvect.bokslstock2.analysis.common.model.EvaluationRateItem
import com.setvect.bokslstock2.analysis.common.model.TradeType.BUY
import com.setvect.bokslstock2.analysis.common.model.TradeType.SELL
import com.setvect.bokslstock2.analysis.vbs.entity.VbsConditionEntity
import com.setvect.bokslstock2.analysis.vbs.entity.VbsTradeEntity
import com.setvect.bokslstock2.analysis.vbs.model.VbsAnalysisCondition
import com.setvect.bokslstock2.analysis.vbs.model.VbsAnalysisReportResult
import com.setvect.bokslstock2.analysis.vbs.model.VbsAnalysisReportResult.TotalYield
import com.setvect.bokslstock2.analysis.vbs.model.VbsAnalysisReportResult.WinningRate
import com.setvect.bokslstock2.analysis.vbs.model.VbsAnalysisReportResult.YieldMdd
import com.setvect.bokslstock2.analysis.vbs.model.VbsTradeReportItem
import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.ss.usermodel.DataFormat
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.streams.toList


/**
 * 변동성 돌파 매매 분석
 */
@Service
class VbsAnalysisService(
    val candleRepository: CandleRepository,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     *  분석 리포트
     */
    fun makeReport(condition: VbsAnalysisCondition) {
        val tradeItemHistory = trade(condition)
        val result = analysis(tradeItemHistory, condition)
        val summary = getSummary(result)
        println(summary)
        makeReportFile(result)
    }

    /**
     * 분석건에 대한 리포트 파일 만듦
     * @return 엑셀 파일
     */
    private fun makeReportFile(result: VbsAnalysisReportResult): File {
        val reportFileSubPrefix = getReportFileSuffix(result)
        val reportFile = File("./backtest-result/vbs-trade-report", "vbs_trade_$reportFileSubPrefix")

        XSSFWorkbook().use { workbook ->
            var sheet = createTradeReport(result, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "1. 매매이력")

            sheet = createReportEvalAmount(result, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "2. 일짜별 자산비율 변화")

            sheet = createReportSummary(result, workbook)
            workbook.setSheetName(workbook.getSheetIndex(sheet), "3. 매매 요약결과 및 조건")

            FileOutputStream(reportFile).use { ous ->
                workbook.write(ous)
            }
        }
        println("결과 파일:" + reportFile.name)
        return reportFile
    }

    /**
     *  복수개의 조건에 대한 분석 요약 리포트를 만듦
     */
    fun makeSummaryReport(conditionList: List<VbsAnalysisCondition>): File {
        var i = 0
        val resultList = conditionList.map { condition ->
            val tradeItemHistory = trade(condition)
            val analysis = analysis(tradeItemHistory, condition)
            log.info("분석 진행 ${++i}/${conditionList.size}")
            analysis
        }.toList()


        var rowIdx = 1
        resultList.forEach { result ->
            makeReportFile(result)
            log.info("개별분석파일 생성 ${rowIdx++}/${resultList.size}")
        }

        // 결과 저장
        val reportFile =
            File("./backtest-result", "변동성돌파_전략_백테스트_분석결과_" + Timestamp.valueOf(LocalDateTime.now()).time + ".xlsx")
        XSSFWorkbook().use { workbook ->
            val sheetBacktestSummary = createTotalSummary(workbook, resultList)
            workbook.setSheetName(workbook.getSheetIndex(sheetBacktestSummary), "1. 평가표")

            val sheetCondition = createMultiCondition(workbook, conditionList)
            workbook.setSheetName(workbook.getSheetIndex(sheetCondition), "2. 테스트 조건")
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
        resultList: List<VbsAnalysisReportResult>
    ): XSSFSheet {
        val sheet = workbook.createSheet()

        val header = "분석기간,분석 아이디,종목,투자비율,최초 투자금액,매수 수수료,매도 수수료," +
                "매매주기,변동성비율,이동평균단위,갭 상승 매도 넘김,하루에 한번 거래,호가단위," +
                "조건 설명," +
                "매수 후 보유 수익,매수 후 보유 MDD,매수 후 보유 CAGR," +
                "실현 수익,실현 MDD,실현 CAGR,매매 횟수,승률"
        applyHeader(sheet, header)
        var rowIdx = 1

        val defaultStyle = ExcelStyle.createDefault(workbook)
        val dateStyle = ExcelStyle.createDate(workbook)
        val commaStyle = ExcelStyle.createComma(workbook)
        val percentStyle = ExcelStyle.createPercent(workbook)
        val percentImportantStyle = ExcelStyle.createPercent(workbook)
        percentImportantStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        percentImportantStyle.fillForegroundColor = IndexedColors.LEMON_CHIFFON.index

        resultList.forEach { result ->

            val multiCondition = result.vbsAnalysisCondition
            val tradeConditionList = multiCondition.tradeConditionList

            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0
            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.range.toString())
            createCell.cellStyle = dateStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString("|") { it.vbsConditionSeq.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.stock.name })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.investRatio)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.cash.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.feeBuy)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.feeSell)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.periodType.name })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.kRate.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.maPeriod.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.gapRisenSkip.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.onlyOneDayTrade.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.unitAskPrice.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.comment)
            createCell.cellStyle = defaultStyle

            val sumYield: TotalYield = result.buyAndHoldYieldTotal
            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(sumYield.yield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(sumYield.mdd)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(sumYield.getCagr())
            createCell.cellStyle = percentStyle

            val totalYield: TotalYield = result.yieldTotal

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
            createCell.setCellValue(result.getWinningRateTotal().getTradeCount().toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx)
            createCell.setCellValue(result.getWinningRateTotal().getWinRate())
            createCell.cellStyle = percentStyle
        }
        sheet.createFreezePane(0, 1)
        sheet.defaultColumnWidth = 14
        sheet.setColumnWidth(0, 12000)
        sheet.setColumnWidth(1, 5000)
        sheet.setColumnWidth(2, 5000)

        ExcelStyle.applyAllBorder(sheet)
        ExcelStyle.applyDefaultFont(sheet)

        return sheet
    }

    /**
     * @return 백테스트 조건 정보를 가지고 있는 시트
     */
    private fun createMultiCondition(
        workbook: XSSFWorkbook,
        conditionList: List<VbsAnalysisCondition>
    ): XSSFSheet {
        val sheet = workbook.createSheet()
        val conditionHeader = "분석 아이디,종목이름,종목코드,매매주기,변동성비율,이동평균단위,갭 상승 매도 넘김,하루에 한번 거래,호가단위,조건설명"
        applyHeader(sheet, conditionHeader)

        val vbsConditionList: List<VbsConditionEntity> = conditionList
            .flatMap { it.tradeConditionList }
            .distinct()
            .toList()

        var rowIdx = 1
        val defaultStyle = ExcelStyle.createDate(workbook)
        val percentStyle = ExcelStyle.createPercent(workbook)
        val decimalStyle = ExcelStyle.createDecimal(workbook)

        for (condition in vbsConditionList) {
            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0

            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.vbsConditionSeq.toString())
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.stock.name)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.stock.code)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.periodType.name)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.kRate)
            createCell.cellStyle = decimalStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.maPeriod.toDouble())
            createCell.cellStyle = decimalStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.gapRisenSkip)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.onlyOneDayTrade)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(condition.unitAskPrice.toDouble())
            createCell.cellStyle = decimalStyle

            createCell = row.createCell(cellIdx)
            createCell.setCellValue(condition.comment)
            createCell.cellStyle = percentStyle
        }

        sheet.createFreezePane(0, 1)
        sheet.defaultColumnWidth = 15

        ExcelStyle.applyAllBorder(sheet)
        ExcelStyle.applyDefaultFont(sheet)
        return sheet
    }

    /**
     * 매매 백테스트
     */
    private fun trade(condition: VbsAnalysisCondition): ArrayList<VbsTradeReportItem> {
        val rangeInList: List<List<VbsTradeEntity>> =
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
        val tradeItemHistory = ArrayList<VbsTradeReportItem>()
        val buyStock = HashMap<String, VbsTradeReportItem>()
        tradeAllList.forEach { tradeItem ->
            if (tradeItem.tradeType == BUY) {
                // 매도 처리
                val buyCash = getBuyCash(buyStock.size, cash, condition.tradeConditionList.size, condition.investRatio)

                val buyQty: Int = (buyCash / tradeItem.unitPrice).toInt()
                val buyAmount: Long = buyQty * tradeItem.unitPrice.toLong()
                val feePrice = (condition.feeBuy * buyAmount).toInt()
                cash -= buyAmount + feePrice
                val stockEvalPrice = buyStock.entries.map { it.value }
                    .sumOf { it.vbsTradeEntity.unitPrice.toLong() * it.qty } + buyQty * tradeItem.unitPrice.toLong()
                val vbsTradeReportItem = VbsTradeReportItem(
                    vbsTradeEntity = tradeItem,
                    qty = buyQty,
                    cash = cash,
                    feePrice = feePrice,
                    gains = 0,
                    stockEvalPrice = stockEvalPrice
                )
                tradeItemHistory.add(vbsTradeReportItem)
                buyStock[tradeItem.vbsConditionEntity.stock.code] = vbsTradeReportItem
            } else if (tradeItem.tradeType == SELL) {
                // 매수 처리
                // 투자수익금 = 매수금액 * 수익률 - 수수료
                val buyTrade = buyStock[tradeItem.vbsConditionEntity.stock.code]
                    ?: throw RuntimeException("${tradeItem.vbsConditionEntity.stock.code} 매수 내역이 없습니다.")
                buyStock.remove(tradeItem.vbsConditionEntity.stock.code)
                val sellPrice = (buyTrade.getBuyAmount() * (1 + tradeItem.yield)).toLong()
                val sellFee = (sellPrice * condition.feeSell).toInt()
                val gains = (sellPrice - buyTrade.getBuyAmount())

                // 매매후 현금
                cash += sellPrice - sellFee

                val stockEvalPrice =
                    buyStock.entries.map { it.value }.sumOf { it.vbsTradeEntity.unitPrice.toLong() * it.qty }
                val vbsTradeReportItem = VbsTradeReportItem(
                    vbsTradeEntity = tradeItem,
                    qty = 0,
                    cash = cash,
                    feePrice = sellFee,
                    gains = gains,
                    stockEvalPrice = stockEvalPrice
                )
                tradeItemHistory.add(vbsTradeReportItem)
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
        tradeItemHistory: ArrayList<VbsTradeReportItem>, condition: VbsAnalysisCondition
    ): VbsAnalysisReportResult {
        // 날짜별로 Buy&Hold 및 투자전략 평가금액 얻기
        val evaluationAmountHistory = applyEvaluationAmount(tradeItemHistory, condition)

        val buyAndHoldYieldMdd: TotalYield = calculateTotalBuyAndHoldYield(evaluationAmountHistory, condition.range)
        val buyAndHoldYieldCondition: Map<Int, YieldMdd> =
            calculateBuyAndHoldYield(condition)

        val yieldTotal: TotalYield = calculateTotalYield(evaluationAmountHistory, condition.range)
        val winningRate: Map<Int, WinningRate> = calculateCoinInvestment(tradeItemHistory)

        return VbsAnalysisReportResult(
            vbsAnalysisCondition = condition,
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
        tradeItemHistory: ArrayList<VbsTradeReportItem>,
        condition: VbsAnalysisCondition
    ): List<EvaluationRateItem> {
        val buyHoldRateMap: SortedMap<LocalDateTime, Double> = getBuyAndHoldEvalRate(condition)
        // <조건아아디, List(캔들)>
        val candleListMap = getConditionOfCandle(condition)

        // <조건아이디, Map<날짜, 종가>>
        val condClosePriceMap: Map<Int, Map<LocalDateTime, Int>> =
            getConditionByClosePriceMap(condition.tradeConditionList, candleListMap)

        val allDateList =
            condClosePriceMap.entries.flatMap { it.value.entries }.map { it.key }.toSortedSet()

        var buyHoldLastAmount = 1.0
        var backtestLastCash = condition.cash // 마지막 보유 현금

        // <거래날짜, 거래내용>
        val tradeByDate: Map<LocalDateTime, List<VbsTradeReportItem>> =
            tradeItemHistory.groupBy { it.vbsTradeEntity.tradeDate }

        // 현재 가지고 있는 주식 수
        // <조건아이디, 주식수>
        val condByStockQty = condition.tradeConditionList.associate { it.vbsConditionSeq to 0 }.toMutableMap()

        val result = allDateList.map { date ->
            val buyHoldRate = buyHoldRateMap[date] ?: buyHoldLastAmount
            val currentTradeList = tradeByDate[date] ?: emptyList()
            for (trade in currentTradeList) {
                val vbsConditionSeq = trade.vbsTradeEntity.vbsConditionEntity.vbsConditionSeq
                condByStockQty[vbsConditionSeq] = trade.qty
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


            val backtestRate = (backtestLastCash + evalStockAmount) / condition.cash.toDouble()
            buyHoldLastAmount = buyHoldRate
            EvaluationRateItem(baseDate = date, buyHoldRate = buyHoldRate, backtestRate = backtestRate)
        }.toMutableList()
        // 최초 시작은 비율은 1.0
        result.add(
            0,
            EvaluationRateItem(
                baseDate = allDateList.first(),
                buyHoldRate = 1.0,
                backtestRate = 1.0
            )
        )
        return result

    }

    /**
     * @return 수익률 정보
     */
    private fun calculateTotalYield(
        evaluationAmountList: List<EvaluationRateItem>, range: DateRange
    ): TotalYield {
        if (evaluationAmountList.isEmpty()) {
            return TotalYield(
                yield = 0.0, mdd = 0.0, dayCount = range.diffDays.toInt()
            )
        }

        val lastCash = evaluationAmountList.last().backtestRate
        val startCash = evaluationAmountList.first().backtestRate
        val realYield = ApplicationUtil.getYield(startCash, lastCash)

        val finalResultList = evaluationAmountList.stream().map(EvaluationRateItem::backtestRate).toList()
        val realMdd = ApplicationUtil.getMdd(finalResultList)
        return TotalYield(realYield, realMdd, range.diffDays.toInt())
    }

    /**
     * @return <조건아이디, 투자 종목 수익 정보>
     */
    private fun calculateCoinInvestment(
        tradeItemHistory: ArrayList<VbsTradeReportItem>
    ): Map<Int, WinningRate> {
        val sellList = tradeItemHistory.filter { it.vbsTradeEntity.tradeType == SELL }.toList()
        val groupBy: Map<Int, List<VbsTradeReportItem>> =
            sellList.groupBy { it.vbsTradeEntity.vbsConditionEntity.vbsConditionSeq }

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
        condition: VbsAnalysisCondition,
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
        evaluationRateList: List<EvaluationRateItem>,
        range: DateRange
    ): TotalYield {
        val rateList = evaluationRateList.map { it.buyHoldRate }.toList()
        return TotalYield(
            ApplicationUtil.getYield(rateList),
            ApplicationUtil.getMdd(rateList),
            range.diffDays.toInt()
        )
    }

    /**
     * Buy & Hold 투자금액 대비 날짜별 평가율
     * @return <날짜, 평가율>
     */
    private fun getBuyAndHoldEvalRate(condition: VbsAnalysisCondition): SortedMap<LocalDateTime, Double> {
        val combinedYield: SortedMap<LocalDateTime, Double> = calculateBuyAndHoldProfitRatio(condition)
        val initial = TreeMap<LocalDateTime, Double>()
        initial[condition.range.from] = 1.0
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
    private fun calculateBuyAndHoldProfitRatio(condition: VbsAnalysisCondition): SortedMap<LocalDateTime, Double> {
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
        tradeConditionList: List<VbsConditionEntity>,
        candleListMpa: Map<Int, List<CandleEntity>>
    ): Map<Int, Map<LocalDateTime, Int>> {
        return tradeConditionList.associate { tradeCondition ->
            tradeCondition.vbsConditionSeq to (candleListMpa[tradeCondition.vbsConditionSeq]
                ?.map { it.candleDateTime to it.closePrice })!!.toMap()
        }
    }

    /**
     * @return <조건아이디, 최초 가격>
     */
    private fun getConditionOfFirstOpenPrice(
        conditionList: List<VbsConditionEntity>,
        mapOfCandleList: Map<Int, List<CandleEntity>>
    ): MutableMap<Int, Int?> {
        return conditionList.associate {
            it.vbsConditionSeq to mapOfCandleList[it.vbsConditionSeq]?.get(0)?.openPrice
        }
            .toMutableMap()
    }

    /**
     *@return <조건아아디, List(캔들)>
     */
    private fun getConditionOfCandle(condition: VbsAnalysisCondition): Map<Int, List<CandleEntity>> {
        return condition.tradeConditionList.associate { tradeCondition ->
            tradeCondition.vbsConditionSeq to candleRepository.findByRange(
                tradeCondition.stock,
                condition.range.from,
                condition.range.to
            )
        }
    }

    /**
     * 분석 요약결과
     */
    private fun getSummary(result: VbsAnalysisReportResult): String {
        val report = StringBuilder()
        val tradeConditionList = result.vbsAnalysisCondition.tradeConditionList

        report.append("----------- Buy&Hold 결과 -----------\n")
        report.append(String.format("합산 동일비중 수익\t %,.2f%%", result.buyAndHoldYieldTotal.yield * 100)).append("\n")
        report.append(String.format("합산 동일비중 MDD\t %,.2f%%", result.buyAndHoldYieldTotal.mdd * 100)).append("\n")
        report.append(String.format("합산 동일비중 CAGR\t %,.2f%%", result.buyAndHoldYieldTotal.getCagr() * 100)).append("\n")

        for (i in 1..tradeConditionList.size) {
            val tradeCondition = tradeConditionList[i - 1]
            report.append(
                "${i}. 조건번호: ${tradeCondition.vbsConditionSeq}, 종목: ${tradeCondition.stock.name}(${tradeCondition.stock.code}), " +
                        "매매주기: ${tradeCondition.periodType}, 변동성 비율: ${tradeCondition.kRate}, 이동평균 단위:${tradeCondition.maPeriod}, " +
                        "갭상승 통과: ${tradeCondition.gapRisenSkip}, 하루에 한번 거래: ${tradeCondition.onlyOneDayTrade}\n"
            )
            val sumYield = result.buyAndHoldYieldCondition[tradeCondition.vbsConditionSeq]
            if (sumYield == null) {
                log.warn("조건에 해당하는 결과가 없습니다. vbsConditionSeq: ${tradeCondition.vbsConditionSeq}")
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
            report.append(
                "${i}. 조건번호: ${tradeCondition.vbsConditionSeq}, 종목: ${tradeCondition.stock.name}(${tradeCondition.stock.code}), " +
                        "매매주기: ${tradeCondition.periodType}, 변동성 비율: ${tradeCondition.kRate}, 이동평균 단위:${tradeCondition.maPeriod}, " +
                        "갭상승 통과: ${tradeCondition.gapRisenSkip}, 하루에 한번 거래: ${tradeCondition.onlyOneDayTrade}\n"
            )

            val winningRate = result.winningRateCondition[tradeCondition.vbsConditionSeq]
            if (winningRate == null) {
                log.warn("조건에 해당하는 결과가 없습니다. vbsConditionSeq: ${tradeCondition.vbsConditionSeq}")
                break
            }
            report.append(String.format("${i}. 실현 수익\t %,d", winningRate.invest)).append("\n")
            report.append(String.format("${i}. 매매회수\t %d", winningRate.getTradeCount())).append("\n")
            report.append(String.format("${i}. 승률\t %,.2f%%", winningRate.getWinRate() * 100)).append("\n")
        }
        return report.toString()
    }

    /**
     * 매매 내역을 시트로 만듦
     */
    private fun createTradeReport(result: VbsAnalysisReportResult, workbook: XSSFWorkbook): XSSFSheet {
        val sheet = workbook.createSheet()
        val header =
            "날짜,종목,매매 구분,이동평균,매수 수량,매매 금액,체결 가격,실현 수익률,수수료,투자 수익(수수료포함),보유 주식 평가금,매매후 보유 현금,평가금(주식+현금),수익비"
        applyHeader(sheet, header)
        var rowIdx = 1

        val defaultStyle = ExcelStyle.createDate(workbook)
        val commaStyle = ExcelStyle.createComma(workbook)
        val percentStyle = ExcelStyle.createPercent(workbook)
        val decimalStyle = ExcelStyle.createDecimal(workbook)

        result.tradeHistory.forEach { tradeItem: VbsTradeReportItem ->
            val vbsTradeEntity: VbsTradeEntity = tradeItem.vbsTradeEntity
            val vbsConditionEntity: VbsConditionEntity = vbsTradeEntity.vbsConditionEntity
            val tradeDate: LocalDateTime = vbsTradeEntity.tradeDate

            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0
            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeDate)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(vbsConditionEntity.stock.getNameCode())
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(vbsTradeEntity.tradeType.name)
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(vbsTradeEntity.maPrice.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.qty.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.getBuyAmount().toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(vbsTradeEntity.unitPrice.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(vbsTradeEntity.yield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.feePrice.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.gains.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.stockEvalPrice.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.cash.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.getEvalPrice().toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx)
            createCell.setCellValue(tradeItem.getEvalPrice() / result.vbsAnalysisCondition.cash.toDouble())
            createCell.cellStyle = decimalStyle
        }

        sheet.createFreezePane(0, 1)
        sheet.defaultColumnWidth = 12
        sheet.setColumnWidth(0, 4000)
        sheet.setColumnWidth(1, 4000)
        sheet.setColumnWidth(12, 4000)
        sheet.setColumnWidth(13, 4000)
        sheet.setColumnWidth(14, 4000)
        sheet.setColumnWidth(15, 4000)

        ExcelStyle.applyAllBorder(sheet)
        ExcelStyle.applyDefaultFont(sheet)
        return sheet
    }

    /**
     * 날짜에 따른 평가금액(Buy&Hold, 벡테스트) 변화 시트 만듦
     */
    private fun createReportEvalAmount(result: VbsAnalysisReportResult, workbook: XSSFWorkbook): XSSFSheet {
        val sheet = workbook.createSheet()
        val header = "날짜,Buy&Hold 평가금,백테스트 평가금"
        applyHeader(sheet, header)
        var rowIdx = 1

        val dateStyle = ExcelStyle.createDate(workbook)
        val commaStyle = ExcelStyle.createDecimal(workbook)

        result.evaluationAmountHistory.forEach { evalItem: EvaluationRateItem ->
            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0
            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(evalItem.baseDate)
            createCell.cellStyle = dateStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(evalItem.buyHoldRate)
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx)
            createCell.setCellValue(evalItem.backtestRate)
            createCell.cellStyle = commaStyle
        }
        sheet.createFreezePane(0, 1)
        sheet.defaultColumnWidth = 20
        ExcelStyle.applyAllBorder(sheet)
        ExcelStyle.applyDefaultFont(sheet)
        return sheet
    }

    /**
     * 매매 결과 요약 및 조건 시트 만듦
     */
    private fun createReportSummary(result: VbsAnalysisReportResult, workbook: XSSFWorkbook): XSSFSheet {
        val sheet = workbook.createSheet()
        val summary = getSummary(result)
        textToSheet(summary, sheet)
        val conditionSummary = getConditionSummary(result)
        textToSheet(conditionSummary, sheet)

        sheet.defaultColumnWidth = 60
        return sheet
    }

    private fun applyHeader(
        sheet: XSSFSheet,
        header: String,
    ) {
        val rowHeader = sheet.createRow(0)
        val headerTxt = header.split(",")
        for (cellIdx in headerTxt.indices) {
            val cell = rowHeader.createCell(cellIdx)
            cell.setCellValue(headerTxt[cellIdx])
            cell.cellStyle = ExcelStyle.createHeaderRow(sheet.workbook)
        }
    }

    private fun textToSheet(summary: String, sheet: XSSFSheet) {
        val lines = summary.split("\n")
        sheet.createRow(sheet.physicalNumberOfRows)

        for (rowIdx in lines.indices) {
            val row = sheet.createRow(sheet.physicalNumberOfRows)
            val columns = lines[rowIdx].split("\t")

            for (colIdx in columns.indices) {
                val colVal = columns[colIdx]
                val cell = row.createCell(colIdx)
                cell.setCellValue(colVal)
                cell.cellStyle = ExcelStyle.createDefault(sheet.workbook)
            }
        }
    }


    /**
     * 백테스트 조건 요약 정보
     */
    private fun getConditionSummary(
        result: VbsAnalysisReportResult
    ): String {
        val range: DateRange = result.vbsAnalysisCondition.range
        val condition = result.vbsAnalysisCondition

        val report = StringBuilder()

        report.append("----------- 백테스트 조건 -----------\n")
        report.append(String.format("분석기간\t %s", range)).append("\n")
        report.append(String.format("투자비율\t %,.2f%%", condition.investRatio * 100)).append("\n")
        report.append(String.format("최초 투자금액\t %,d", condition.cash)).append("\n")
        report.append(String.format("매수 수수료\t %,.2f%%", condition.feeBuy * 100)).append("\n")
        report.append(String.format("매도 수수료\t %,.2f%%", condition.feeSell * 100)).append("\n")

        val tradeConditionList: List<VbsConditionEntity> = result.vbsAnalysisCondition.tradeConditionList

        for (i in 1..tradeConditionList.size) {
            val tradeCondition = tradeConditionList[i - 1]
            report.append(String.format("${i}. 조건아이디\t %s", tradeCondition.vbsConditionSeq)).append("\n")
            report.append(String.format("${i}. 분석주기\t %s", tradeCondition.periodType)).append("\n")
            report.append(String.format("${i}. 대상 종목\t %s", tradeCondition.stock.getNameCode())).append("\n")
            report.append(String.format("${i}. 변동성 비율\t %,.2f", tradeCondition.kRate)).append("\n")
            report.append(String.format("${i}. 이동평균 단위\t %d", tradeCondition.maPeriod)).append("\n")
            report.append(String.format("${i}. 갭 상승 시 매도 넘김\t %s", tradeCondition.gapRisenSkip)).append("\n")
            report.append(String.format("${i}. 하루에 한번 거래\t %s", tradeCondition.onlyOneDayTrade)).append("\n")
            report.append(String.format("${i}. 호가단위\t %s", tradeCondition.unitAskPrice)).append("\n")
            report.append(String.format("${i}. 조건 설명\t %s", tradeCondition.comment)).append("\n")
        }
        return report.toString()
    }

    /**
     * @return 조건 정보가 담긴 리포트 파일명 subfix
     */
    private fun getReportFileSuffix(
        result: VbsAnalysisReportResult
    ): String {
        val tradeConditionList = result.vbsAnalysisCondition.tradeConditionList
        return String.format(
            "%s_%s~%s_%s.xlsx",
            tradeConditionList.joinToString(",") { it.vbsConditionSeq.toString() },
            result.vbsAnalysisCondition.range.fromDateFormat,
            result.vbsAnalysisCondition.range.toDateFormat,
            tradeConditionList.joinToString(",") { it.stock.code }
        )
    }

    /**
     * 엑셀 리포트에 사용될 셀 스타일 모음
     */
    object ExcelStyle {
        fun createDefault(workbook: XSSFWorkbook): XSSFCellStyle? {
            return workbook.createCellStyle()
        }

        fun createDate(workbook: XSSFWorkbook): XSSFCellStyle? {
            val cellStyle = workbook.createCellStyle()
            val createHelper: CreationHelper = workbook.creationHelper
            cellStyle.dataFormat = createHelper.createDataFormat().getFormat("yyyy/MM/dd hh:mm")
            return cellStyle
        }

        fun createComma(workbook: XSSFWorkbook): XSSFCellStyle? {
            val cellStyle = workbook.createCellStyle()
            val format: DataFormat = workbook.createDataFormat()
            cellStyle.dataFormat = format.getFormat("###,###")
            return cellStyle
        }

        /**
         * 소수점 표시
         */
        fun createDecimal(workbook: XSSFWorkbook): XSSFCellStyle? {
            val cellStyle = workbook.createCellStyle()
            val format: DataFormat = workbook.createDataFormat()
            cellStyle.dataFormat = format.getFormat("0.00")
            return cellStyle
        }

        fun createPercent(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            val format: DataFormat = workbook.createDataFormat()
            cellStyle.dataFormat = format.getFormat("###,###.00%")
            return cellStyle
        }

        fun createHeaderRow(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            cellStyle.fillForegroundColor = IndexedColors.YELLOW.index

            val font: XSSFFont = workbook.createFont()
            font.bold = true
            cellStyle.setFont(font)
            cellStyle.alignment = HorizontalAlignment.CENTER
            cellStyle.verticalAlignment = VerticalAlignment.CENTER
            return cellStyle
        }

        /**
         * 모든 셀 border 적용
         */
        fun applyAllBorder(sheet: XSSFSheet) {
            val rowCount = sheet.physicalNumberOfRows
            for (rowIdx in 0 until rowCount) {
                val row = sheet.getRow(rowIdx)
                val cellCount = row.physicalNumberOfCells
                for (cellIdx in 0 until cellCount) {
                    val cell = row.getCell(cellIdx)
                    val cellStyle = cell.cellStyle
                    cellStyle.borderBottom = BorderStyle.THIN
                    cellStyle.borderTop = BorderStyle.THIN
                    cellStyle.borderRight = BorderStyle.THIN
                    cellStyle.borderLeft = BorderStyle.THIN
                }
            }
        }

        fun applyDefaultFont(sheet: XSSFSheet) {
            val rowCount = sheet.physicalNumberOfRows
            for (rowIdx in 0 until rowCount) {
                val row = sheet.getRow(rowIdx)
                val cellCount = row.physicalNumberOfCells
                for (cellIdx in 0 until cellCount) {
                    val cellStyle = row.getCell(cellIdx).cellStyle
                    cellStyle.font.fontName = "맑은 고딕"
                }
            }
        }

    }
}