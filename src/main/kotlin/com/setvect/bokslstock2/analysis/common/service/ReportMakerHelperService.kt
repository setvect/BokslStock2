package com.setvect.bokslstock2.analysis.common.service

import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.analysis.common.model.EvaluationRateItem
import com.setvect.bokslstock2.analysis.common.model.TradeType
import com.setvect.bokslstock2.analysis.common.model.YieldRateItem
import com.setvect.bokslstock2.common.entity.AnalysisCondition
import com.setvect.bokslstock2.common.entity.AnalysisReportResult
import com.setvect.bokslstock2.common.entity.ConditionEntity
import com.setvect.bokslstock2.common.entity.TradeReportItem
import com.setvect.bokslstock2.index.entity.CandleEntity
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.util.ApplicationUtil
import com.setvect.bokslstock2.util.DateRange
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
import org.springframework.stereotype.Service
import kotlin.streams.toList

/**
 * 리포트 생성에 필요한 공통 메소드 제공
 */
@Service
class ReportMakerHelperService(
    val candleRepository: CandleRepository,
) {
    /**
     * @return <조건아이디, 투자 종목에 대한 Buy & Hold시 수익 정보>
     */
    fun calculateBuyAndHoldYield(
        condition: AnalysisCondition,
    ): Map<Long, CommonAnalysisReportResult.YieldMdd> {
        val mapOfCandleList = getConditionOfCandle(condition)

        // <조건아이디, 직전 가격>
        val mapOfBeforePrice =
            getConditionOfFirstOpenPrice(condition.conditionList, mapOfCandleList)

        return mapOfCandleList.entries.associate { entry ->
            val priceHistory = entry.value.stream().map { it.closePrice }.toList().toMutableList()
            // 해당 캔들의 시초가를 맨 앞에 넣기
            priceHistory.add(0, mapOfBeforePrice[entry.key])

            entry.key to CommonAnalysisReportResult.YieldMdd(
                ApplicationUtil.getYield(priceHistory),
                ApplicationUtil.getMdd(priceHistory)
            )
        }
    }

    /**
     *@return <조건아아디, List(캔들)>
     */
    fun getConditionOfCandle(condition: AnalysisCondition): Map<Long, List<CandleEntity>> {
        return condition.conditionList.associate { tradeCondition ->
            tradeCondition.getConditionId() to candleRepository.findByRange(
                tradeCondition.stock,
                condition.basic.range.from,
                condition.basic.range.to
            )
        }
    }

    /**
     * Buy & Hold 투자금액 대비 날짜별 평가율
     * @return <날짜, 평가율>
     */
    fun getBuyAndHoldEvalRate(condition: AnalysisCondition): SortedMap<LocalDateTime, Double> {
        val combinedYield: SortedMap<LocalDateTime, Double> = calculateBuyAndHoldProfitRatio(condition)
        val initial = TreeMap<LocalDateTime, Double>()
        initial[condition.basic.range.from] = 1.0
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
    fun calculateBuyAndHoldProfitRatio(condition: AnalysisCondition): SortedMap<LocalDateTime, Double> {
        val range = condition.basic.range

        val tradeConditionList = condition.conditionList
        // <조건아아디, List(캔들)>2
        val mapOfCandleList = getConditionOfCandle(condition)

        // <조건아이디, Map<날짜, 종가>>
        val mapOfCondClosePrice: Map<Long, Map<LocalDateTime, Double>> =
            getConditionByClosePriceMap(tradeConditionList, mapOfCandleList)

        // <조건아이디, 직전 가격>
        val mapOfBeforePrice = getConditionOfFirstOpenPrice(tradeConditionList, mapOfCandleList)
        var currentDate = range.from
        // <날짜, Map<조건아이디, 상대 수익률>>
        val mapOfDayRelativeRate = mutableMapOf<LocalDateTime, Map<Long, Double>>()
        while (currentDate.isBefore(range.to) || (currentDate == range.to)) {
            // Map<조건아이디, 상대 수익률>
            val mapCondRelativeRate: Map<Long, Double> = mapOfCondClosePrice.entries
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
     * @return 날짜별 평가금 계산
     */
    fun applyEvaluationAmount(
        tradeItemHistory: List<TradeReportItem>,
        condition: AnalysisCondition
    ): List<EvaluationRateItem> {
        val buyHoldRateMap: SortedMap<LocalDateTime, Double> = getBuyAndHoldEvalRate(condition)
        // <조건아아디, List(캔들)>
        val candleListMap = getConditionOfCandle(condition)

        // <조건아이디, Map<날짜, 종가>>
        val condClosePriceMap: Map<Long, Map<LocalDateTime, Double>> =
            getConditionByClosePriceMap(condition.conditionList, candleListMap)

        val allDateList =
            condClosePriceMap.entries
                .flatMap { it.value.entries }
                .map { it.key }.toSortedSet()

        var buyHoldLastRate = 1.0
        var backtestLastRate = 1.0
        var backtestLastCash = condition.basic.cash // 마지막 보유 현금

        // <거래날짜, 거래내용>
        val tradeByDate: Map<LocalDateTime, List<TradeReportItem>> =
            tradeItemHistory.groupBy { it.tradeEntity.tradeDate }

        // 현재 가지고 있는 주식 수
        // <조건아이디, 주식수>
        val condByStockQty = condition.conditionList.associate { it.getConditionId() to 0 }.toMutableMap()

        val result = allDateList.map { date ->
            val buyHoldRate = buyHoldRateMap[date] ?: buyHoldLastRate
            val currentTradeList = tradeByDate[date] ?: emptyList()
            for (trade in currentTradeList) {
                val vbsConditionSeq = trade.tradeEntity.getConditionEntity().getConditionId()
                condByStockQty[vbsConditionSeq] = trade.common.qty
                backtestLastCash = trade.common.cash
            }

            // 종가기준으로 보유 주식 평가금액 구하기
            val evalStockAmount =
                condByStockQty.entries.stream().filter { it.value > 0 }
                    .mapToDouble {
                        val closePrice = condClosePriceMap[it.key]!![date]
                            ?: throw RuntimeException("${date}에 대한 조건아이디(${it.key})의 종가 정보가 없습니다.")
                        closePrice * it.value
                    }.sum()


            val backtestRate = (backtestLastCash + evalStockAmount) / condition.basic.cash
            val buyHoldYield = ApplicationUtil.getYield(buyHoldLastRate, buyHoldRate)
            val backtestYield = ApplicationUtil.getYield(backtestLastRate, backtestRate)

            buyHoldLastRate = buyHoldRate
            backtestLastRate= backtestRate
            EvaluationRateItem(
                baseDate = date,
                buyHoldRate = buyHoldRate,
                backtestRate = backtestRate,
                buyHoldYield = buyHoldYield,
                backtestYield = backtestYield
            )
        }.toMutableList()
        // 최초 시작은 비율은 1.0
        result.add(
            0,
            EvaluationRateItem(
                baseDate = allDateList.first(),
                buyHoldRate = 1.0,
                backtestRate = 1.0,
                buyHoldYield = 0.0,
                backtestYield = 0.0
            )
        )
        return result
    }


    companion object {
        /**
         * 날짜에 따른 평가금액(Buy&Hold, 벡테스트) 변화 시트 만듦
         */
        fun createReportEvalAmount(
            evaluationAmountHistory: List<EvaluationRateItem>,
            workbook: XSSFWorkbook
        ): XSSFSheet {
            val sheet = workbook.createSheet()
            val header = "날짜,Buy&Hold 평가금,백테스트 평가금,Buy&Hold 일일 수익률,백테스트 일일 수익률,Buy&Hold Maxdrawdown,백테스트 Maxdrawdown"
            applyHeader(sheet, header)
            var rowIdx = 1

            val dateStyle = ExcelStyle.createDate(workbook)
            val commaStyle = ExcelStyle.createDecimal(workbook)
            val percentStyle = ExcelStyle.createPercent(workbook)

            var buyAndHoldMax = 0.0
            var backtestMax = 0.0

            evaluationAmountHistory.forEach { evalItem: EvaluationRateItem ->
                val row = sheet.createRow(rowIdx++)
                var cellIdx = 0
                var createCell = row.createCell(cellIdx++)
                buyAndHoldMax = buyAndHoldMax.coerceAtLeast(evalItem.buyHoldRate)
                backtestMax = backtestMax.coerceAtLeast(evalItem.backtestRate)

                createCell.setCellValue(evalItem.baseDate)
                createCell.cellStyle = dateStyle

                // 변화량
                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(evalItem.buyHoldRate)
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(evalItem.backtestRate)
                createCell.cellStyle = commaStyle

                // 일일 수익률
                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(evalItem.buyHoldYield)
                createCell.cellStyle = percentStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(evalItem.backtestYield)
                createCell.cellStyle = percentStyle

                // Maxdrawdown
                createCell = row.createCell(cellIdx++)
                createCell.setCellValue((evalItem.buyHoldRate - buyAndHoldMax) / buyAndHoldMax)
                createCell.cellStyle = percentStyle

                createCell = row.createCell(cellIdx)
                createCell.setCellValue((evalItem.backtestRate - backtestMax) / backtestMax)
                createCell.cellStyle = percentStyle
            }
            sheet.createFreezePane(0, 1)
            sheet.defaultColumnWidth = 20
            ExcelStyle.applyAllBorder(sheet)
            ExcelStyle.applyDefaultFont(sheet)
            return sheet
        }

        /**
         * 기간별(년,월) 수익률
         */
        fun createReportRangeReturn(
            yieldHistory: List<YieldRateItem>,
            workbook: XSSFWorkbook
        ): XSSFSheet {
            val sheet = workbook.createSheet()
            val header = "날짜,Buy&Hold 수익률,백테스트 수익률"
            applyHeader(sheet, header)
            var rowIdx = 1

            val dateStyle = ExcelStyle.createYearMonth(workbook)
            val percentStyle = ExcelStyle.createPercent(workbook)

            yieldHistory.forEach { monthYield ->
                val row = sheet.createRow(rowIdx++)
                var cellIdx = 0
                var createCell = row.createCell(cellIdx++)
                createCell.setCellValue(monthYield.baseDate)
                createCell.cellStyle = dateStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(monthYield.buyHoldYield)
                createCell.cellStyle = percentStyle

                createCell = row.createCell(cellIdx)
                createCell.setCellValue(monthYield.backtestYield)
                createCell.cellStyle = percentStyle
            }
            sheet.createFreezePane(0, 1)
            sheet.defaultColumnWidth = 20
            ExcelStyle.applyAllBorder(sheet)
            ExcelStyle.applyDefaultFont(sheet)
            return sheet
        }


        /**
         * @return 전체 투자 종목에 대한 Buy & Hold시 수익 정보
         */
        fun calculateTotalBuyAndHoldYield(
            evaluationRateList: List<EvaluationRateItem>,
            range: DateRange
        ): CommonAnalysisReportResult.TotalYield {
            val rateList = evaluationRateList.map { it.buyHoldRate }.toList()
            return CommonAnalysisReportResult.TotalYield(
                ApplicationUtil.getYield(rateList),
                ApplicationUtil.getMdd(rateList),
                range.diffDays.toInt()
            )
        }

        /**
         * @return 수익률 정보
         */
        fun calculateTotalYield(
            evaluationAmountList: List<EvaluationRateItem>, range: DateRange
        ): CommonAnalysisReportResult.TotalYield {
            if (evaluationAmountList.isEmpty()) {
                return CommonAnalysisReportResult.TotalYield(
                    yield = 0.0, mdd = 0.0, dayCount = range.diffDays.toInt()
                )
            }

            val lastCash = evaluationAmountList.last().backtestRate
            val startCash = evaluationAmountList.first().backtestRate
            val realYield = ApplicationUtil.getYield(startCash, lastCash)

            val finalResultList = evaluationAmountList.stream().map(EvaluationRateItem::backtestRate).toList()
            val realMdd = ApplicationUtil.getMdd(finalResultList)
            return CommonAnalysisReportResult.TotalYield(realYield, realMdd, range.diffDays.toInt())
        }

        /**
         * [currentBuyStockCount] 현재 매수중인 종목 수
         * [cash] 현재 보유 현금
         * [stockBuyTotalCount] 매매 대상 종목수
         * [investRatio] 전체 현금 대비 투자 비율. 1: 모든 현금을 투자, 0.5 현금의 50%만 매수에 사용
         *
         * @return 매수에 사용될 금액 반환
         */
        fun getBuyCash(
            currentBuyStockCount: Int,
            cash: Double,
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

        fun textToSheet(summary: String, sheet: XSSFSheet) {
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


        fun applyHeader(
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


        /**
         * @return <조건아이디, 최초 가격>
         */
        fun getConditionOfFirstOpenPrice(
            conditionList: List<ConditionEntity>,
            mapOfCandleList: Map<Long, List<CandleEntity>>
        ): MutableMap<Long, Double?> {
            return conditionList.associate {
                it.getConditionId() to mapOfCandleList[it.getConditionId()]?.get(0)?.openPrice
            }
                .toMutableMap()
        }


        /**
         * @return <조건아이디, Map<날짜, 종가>>
         */
        fun getConditionByClosePriceMap(
            tradeConditionList: List<ConditionEntity>,
            candleListMap: Map<Long, List<CandleEntity>>
        ): Map<Long, Map<LocalDateTime, Double>> {
            return tradeConditionList.associate { tradeCondition ->
                tradeCondition.getConditionId() to (candleListMap[tradeCondition.getConditionId()]
                    ?.map { it.candleDateTime to it.closePrice })!!.toMap()
            }
        }

        /**
         * @return <조건아이디, 투자 종목 수익 정보>
         */
        fun calculateCoinInvestment(
            tradeItemHistory: List<TradeReportItem>
        ): Map<Long, CommonAnalysisReportResult.WinningRate> {
            val sellList = tradeItemHistory.filter { it.tradeEntity.tradeType == TradeType.SELL }.toList()
            val groupBy: Map<Long, List<TradeReportItem>> =
                sellList.groupBy { it.tradeEntity.getConditionEntity().getConditionId() }

            return groupBy.entries.associate { entity ->
                val totalInvest = entity.value.sumOf { it.common.gains }
                val gainCount = entity.value.count { it.common.gains > 0 }
                entity.key to CommonAnalysisReportResult.WinningRate(
                    gainCount,
                    entity.value.size - gainCount,
                    totalInvest
                )
            }.toMap()
        }

        /**
         * @return 조건 정보가 담긴 리포트 파일명 subfix
         */
        fun getReportFileSuffix(
            result: AnalysisReportResult
        ): String {
            val tradeConditionList = result.analysisCondition.conditionList
            return String.format(
                "%s_%s~%s_%s.xlsx",
                tradeConditionList.joinToString(",") { it.getConditionId().toString() },
                result.analysisCondition.basic.range.fromDateFormat,
                result.analysisCondition.basic.range.toDateFormat,
                tradeConditionList.joinToString(",") { it.stock.code }
            )
        }
    }

    /**
     * 엑셀 리포트에 사용될 셀 스타일 모음
     */
    object ExcelStyle {
        fun createDefault(workbook: XSSFWorkbook): XSSFCellStyle {
            return workbook.createCellStyle()
        }

        fun createDate(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            val createHelper: CreationHelper = workbook.creationHelper
            cellStyle.dataFormat = createHelper.createDataFormat().getFormat("yyyy/MM/dd")
            return cellStyle
        }

        fun createYearMonth(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            val createHelper: CreationHelper = workbook.creationHelper
            cellStyle.dataFormat = createHelper.createDataFormat().getFormat("yyyy/MM")
            return cellStyle
        }

        fun createComma(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            val format: DataFormat = workbook.createDataFormat()
            cellStyle.dataFormat = format.getFormat("###,###")
            return cellStyle
        }

        /**
         * 소수점 표시
         */
        fun createDecimal(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            val format: DataFormat = workbook.createDataFormat()
            cellStyle.dataFormat = format.getFormat("0.00")
            return cellStyle
        }

        fun createPercent(workbook: XSSFWorkbook): XSSFCellStyle {
            val cellStyle = workbook.createCellStyle()
            val format: DataFormat = workbook.createDataFormat()
            cellStyle.dataFormat = format.getFormat("###,##0.00%")
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