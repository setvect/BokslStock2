package com.setvect.bokslstock2.analysis.vbs.service

import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult
import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult.TotalYield
import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult.WinningRate
import com.setvect.bokslstock2.analysis.common.model.CommonAnalysisReportResult.YieldMdd
import com.setvect.bokslstock2.analysis.common.model.CommonTradeReportItem
import com.setvect.bokslstock2.analysis.common.model.TradeType.BUY
import com.setvect.bokslstock2.analysis.common.model.TradeType.SELL
import com.setvect.bokslstock2.analysis.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.analysis.vbs.entity.VbsConditionEntity
import com.setvect.bokslstock2.analysis.vbs.entity.VbsTradeEntity
import com.setvect.bokslstock2.analysis.vbs.model.VbsAnalysisCondition
import com.setvect.bokslstock2.analysis.vbs.model.VbsAnalysisReportResult
import com.setvect.bokslstock2.analysis.vbs.model.VbsTradeReportItem
import com.setvect.bokslstock2.util.DateRange
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp
import java.time.LocalDateTime
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
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
    val reportMakerHelperService: ReportMakerHelperService,
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

            sheet = ReportMakerHelperService.createReportEvalAmount(result.common.evaluationAmountHistory, workbook)
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
        ReportMakerHelperService.applyHeader(sheet, header)
        var rowIdx = 1

        val defaultStyle = ReportMakerHelperService.ExcelStyle.createDefault(workbook)
        val dateStyle = ReportMakerHelperService.ExcelStyle.createDate(workbook)
        val commaStyle = ReportMakerHelperService.ExcelStyle.createComma(workbook)
        val percentStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        val percentImportantStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        percentImportantStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        percentImportantStyle.fillForegroundColor = IndexedColors.LEMON_CHIFFON.index

        resultList.forEach { result ->

            val multiCondition = result.vbsAnalysisCondition
            val tradeConditionList = multiCondition.tradeConditionList

            val row = sheet.createRow(rowIdx++)
            var cellIdx = 0
            var createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.range.toString())
            createCell.cellStyle = dateStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString("|") { it.vbsConditionSeq.toString() })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeConditionList.joinToString(",") { it.stock.name })
            createCell.cellStyle = defaultStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.investRatio)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.cash.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.feeBuy)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(multiCondition.basic.feeSell)
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
            createCell.setCellValue(multiCondition.basic.comment)
            createCell.cellStyle = defaultStyle

            val sumYield: TotalYield = result.common.buyAndHoldYieldTotal
            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(sumYield.yield)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(sumYield.mdd)
            createCell.cellStyle = percentStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(sumYield.getCagr())
            createCell.cellStyle = percentStyle

            val totalYield: TotalYield = result.common.yieldTotal

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
     * @return 백테스트 조건 정보를 가지고 있는 시트
     */
    private fun createMultiCondition(
        workbook: XSSFWorkbook,
        conditionList: List<VbsAnalysisCondition>
    ): XSSFSheet {
        val sheet = workbook.createSheet()
        val conditionHeader = "분석 아이디,종목이름,종목코드,매매주기,변동성비율,이동평균단위,갭 상승 매도 넘김,하루에 한번 거래,호가단위,조건설명"
        ReportMakerHelperService.applyHeader(sheet, conditionHeader)

        val vbsConditionList: List<VbsConditionEntity> = conditionList
            .flatMap { it.tradeConditionList }
            .distinct()
            .toList()

        var rowIdx = 1
        val defaultStyle = ReportMakerHelperService.ExcelStyle.createDate(workbook)
        val percentStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        val decimalStyle = ReportMakerHelperService.ExcelStyle.createDecimal(workbook)

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

        ReportMakerHelperService.ExcelStyle.applyAllBorder(sheet)
        ReportMakerHelperService.ExcelStyle.applyDefaultFont(sheet)
        return sheet
    }

    /**
     * 매매 백테스트
     */
    private fun trade(condition: VbsAnalysisCondition): ArrayList<VbsTradeReportItem> {
        val rangeInList: List<List<VbsTradeEntity>> =
            condition.tradeConditionList.map { mainList ->
                mainList.tradeList.filter { condition.basic.range.isBetween(it.tradeDate) }
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

        var cash = condition.basic.cash
        val tradeItemHistory = ArrayList<VbsTradeReportItem>()
        val buyStock = HashMap<String, VbsTradeReportItem>()
        tradeAllList.forEach { tradeItem ->
            if (tradeItem.tradeType == BUY) {
                // 매도 처리
                val buyCash =
                    ReportMakerHelperService.getBuyCash(
                        buyStock.size,
                        cash,
                        condition.tradeConditionList.size,
                        condition.basic.investRatio
                    )

                val buyQty: Int = (buyCash / tradeItem.unitPrice).toInt()
                val buyAmount: Long = buyQty * tradeItem.unitPrice.toLong()
                val feePrice = (condition.basic.feeBuy * buyAmount).toInt()
                cash -= buyAmount + feePrice
                val stockEvalPrice = buyStock.entries.map { it.value }
                    .sumOf { it.vbsTradeEntity.unitPrice.toLong() * it.common.qty } + buyQty * tradeItem.unitPrice.toLong()
                val vbsTradeReportItem = VbsTradeReportItem(
                    vbsTradeEntity = tradeItem,
                    common = CommonTradeReportItem(
                        qty = buyQty,
                        cash = cash,
                        feePrice = feePrice,
                        gains = 0,
                        stockEvalPrice = stockEvalPrice
                    )
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
                val sellFee = (sellPrice * condition.basic.feeSell).toInt()
                val gains = (sellPrice - buyTrade.getBuyAmount())

                // 매매후 현금
                cash += sellPrice - sellFee

                val stockEvalPrice =
                    buyStock.entries.map { it.value }.sumOf { it.vbsTradeEntity.unitPrice.toLong() * it.common.qty }
                val vbsTradeReportItem = VbsTradeReportItem(
                    vbsTradeEntity = tradeItem,
                    common = CommonTradeReportItem(
                        qty = 0,
                        cash = cash,
                        feePrice = sellFee,
                        gains = gains,
                        stockEvalPrice = stockEvalPrice
                    )
                )
                tradeItemHistory.add(vbsTradeReportItem)
            }
        }
        return tradeItemHistory
    }

    /**
     * 매매 결과에 대한 통계적 분석을 함
     */
    private fun analysis(
        tradeItemHistory: ArrayList<VbsTradeReportItem>, condition: VbsAnalysisCondition
    ): VbsAnalysisReportResult {
        // 날짜별로 Buy&Hold 및 투자전략 평가금액 얻기
        val evaluationAmountHistory = reportMakerHelperService.applyEvaluationAmount(tradeItemHistory, condition)

        val buyAndHoldYieldMdd: TotalYield =
            ReportMakerHelperService.calculateTotalBuyAndHoldYield(evaluationAmountHistory, condition.basic.range)
        val buyAndHoldYieldCondition: Map<Int, YieldMdd> =
            reportMakerHelperService.calculateBuyAndHoldYield(condition)

        val yieldTotal: TotalYield =
            ReportMakerHelperService.calculateTotalYield(evaluationAmountHistory, condition.basic.range)

        val winningRate: Map<Int, WinningRate> = ReportMakerHelperService.calculateCoinInvestment(tradeItemHistory)

        return VbsAnalysisReportResult(
            vbsAnalysisCondition = condition,
            tradeHistory = tradeItemHistory,
            common = CommonAnalysisReportResult(
                evaluationAmountHistory = evaluationAmountHistory,
                yieldTotal = yieldTotal,
                winningRateCondition = winningRate,
                buyAndHoldYieldCondition = buyAndHoldYieldCondition,
                buyAndHoldYieldTotal = buyAndHoldYieldMdd,
            )
        )
    }


    /**
     * 분석 요약결과
     */
    private fun getSummary(result: VbsAnalysisReportResult): String {
        val report = StringBuilder()
        val tradeConditionList = result.vbsAnalysisCondition.tradeConditionList

        report.append("----------- Buy&Hold 결과 -----------\n")
        report.append(String.format("합산 동일비중 수익\t %,.2f%%", result.common.buyAndHoldYieldTotal.yield * 100))
            .append("\n")
        report.append(String.format("합산 동일비중 MDD\t %,.2f%%", result.common.buyAndHoldYieldTotal.mdd * 100)).append("\n")
        report.append(String.format("합산 동일비중 CAGR\t %,.2f%%", result.common.buyAndHoldYieldTotal.getCagr() * 100))
            .append("\n")

        for (i in 1..tradeConditionList.size) {
            val tradeCondition = tradeConditionList[i - 1]
            report.append(
                "${i}. 조건번호: ${tradeCondition.vbsConditionSeq}, 종목: ${tradeCondition.stock.name}(${tradeCondition.stock.code}), " +
                        "매매주기: ${tradeCondition.periodType}, 변동성 비율: ${tradeCondition.kRate}, 이동평균 단위:${tradeCondition.maPeriod}, " +
                        "갭상승 통과: ${tradeCondition.gapRisenSkip}, 하루에 한번 거래: ${tradeCondition.onlyOneDayTrade}\n"
            )
            val sumYield = result.common.buyAndHoldYieldCondition[tradeCondition.vbsConditionSeq]
            if (sumYield == null) {
                log.warn("조건에 해당하는 결과가 없습니다. vbsConditionSeq: ${tradeCondition.vbsConditionSeq}")
                break
            }
            report.append(String.format("${i}. 동일비중 수익\t %,.2f%%", sumYield.yield * 100)).append("\n")
            report.append(String.format("${i}. 동일비중 MDD\t %,.2f%%", sumYield.mdd * 100)).append("\n")
        }

        val totalYield: TotalYield = result.common.yieldTotal
        report.append("----------- 전략 결과 -----------\n")
        report.append(String.format("합산 실현 수익\t %,.2f%%", totalYield.yield * 100)).append("\n")
        report.append(String.format("합산 실현 MDD\t %,.2f%%", totalYield.mdd * 100)).append("\n")
        report.append(String.format("합산 매매회수\t %d", result.common.getWinningRateTotal().getTradeCount())).append("\n")
        report.append(String.format("합산 승률\t %,.2f%%", result.common.getWinningRateTotal().getWinRate() * 100))
            .append("\n")
        report.append(String.format("합산 CAGR\t %,.2f%%", totalYield.getCagr() * 100)).append("\n")

        for (i in 1..tradeConditionList.size) {
            val tradeCondition = tradeConditionList[i - 1]
            report.append(
                "${i}. 조건번호: ${tradeCondition.vbsConditionSeq}, 종목: ${tradeCondition.stock.name}(${tradeCondition.stock.code}), " +
                        "매매주기: ${tradeCondition.periodType}, 변동성 비율: ${tradeCondition.kRate}, 이동평균 단위:${tradeCondition.maPeriod}, " +
                        "갭상승 통과: ${tradeCondition.gapRisenSkip}, 하루에 한번 거래: ${tradeCondition.onlyOneDayTrade}\n"
            )

            val winningRate = result.common.winningRateCondition[tradeCondition.vbsConditionSeq]
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
        ReportMakerHelperService.applyHeader(sheet, header)
        var rowIdx = 1

        val defaultStyle = ReportMakerHelperService.ExcelStyle.createDate(workbook)
        val commaStyle = ReportMakerHelperService.ExcelStyle.createComma(workbook)
        val percentStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
        val decimalStyle = ReportMakerHelperService.ExcelStyle.createDecimal(workbook)

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
            createCell.setCellValue(tradeItem.common.qty.toDouble())
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
            createCell.setCellValue(tradeItem.common.feePrice.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.common.gains.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.common.stockEvalPrice.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.common.cash.toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx++)
            createCell.setCellValue(tradeItem.common.getEvalPrice().toDouble())
            createCell.cellStyle = commaStyle

            createCell = row.createCell(cellIdx)
            createCell.setCellValue(tradeItem.common.getEvalPrice() / result.vbsAnalysisCondition.basic.cash.toDouble())
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

        ReportMakerHelperService.ExcelStyle.applyAllBorder(sheet)
        ReportMakerHelperService.ExcelStyle.applyDefaultFont(sheet)
        return sheet
    }

    /**
     * 매매 결과 요약 및 조건 시트 만듦
     */
    private fun createReportSummary(result: VbsAnalysisReportResult, workbook: XSSFWorkbook): XSSFSheet {
        val sheet = workbook.createSheet()
        val summary = getSummary(result)
        ReportMakerHelperService.textToSheet(summary, sheet)
        val conditionSummary = getConditionSummary(result)
        ReportMakerHelperService.textToSheet(conditionSummary, sheet)

        sheet.defaultColumnWidth = 60
        return sheet
    }

    /**
     * 백테스트 조건 요약 정보
     */
    private fun getConditionSummary(
        result: VbsAnalysisReportResult
    ): String {
        val range: DateRange = result.vbsAnalysisCondition.basic.range
        val condition = result.vbsAnalysisCondition

        val report = StringBuilder()

        report.append("----------- 백테스트 조건 -----------\n")
        report.append(String.format("분석기간\t %s", range)).append("\n")
        report.append(String.format("투자비율\t %,.2f%%", condition.basic.investRatio * 100)).append("\n")
        report.append(String.format("최초 투자금액\t %,d", condition.basic.cash)).append("\n")
        report.append(String.format("매수 수수료\t %,.2f%%", condition.basic.feeBuy * 100)).append("\n")
        report.append(String.format("매도 수수료\t %,.2f%%", condition.basic.feeSell * 100)).append("\n")

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
            result.vbsAnalysisCondition.basic.range.fromDateFormat,
            result.vbsAnalysisCondition.basic.range.toDateFormat,
            tradeConditionList.joinToString(",") { it.stock.code }
        )
    }
}