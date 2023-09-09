package com.setvect.bokslstock2.strategy.inheritancetax.service

import com.setvect.bokslstock2.backtest.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.backtest.dart.entity.CorporateDisclosureInfoEntity
import com.setvect.bokslstock2.backtest.dart.model.FinancialDetailMetric
import com.setvect.bokslstock2.backtest.dart.repository.CorporateDisclosureInfoRepository
import com.setvect.bokslstock2.crawl.koreacompany.service.CrawlerKoreanCompanyProperties
import com.setvect.bokslstock2.crawl.koreacompany.service.CrawlerKoreanCompanyService
import com.setvect.bokslstock2.strategy.inheritancetax.model.InheritanceTaxScore
import com.setvect.bokslstock2.strategy.inheritancetax.model.Quarter
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream

/**
 * 상속세 전략. 자세한 설명은 README.md 참고
 */
@Service
class InheritanceTaxService(
    private val crawlerKoreanCompanyService: CrawlerKoreanCompanyService,
    private val corporateDisclosureInfoRepository: CorporateDisclosureInfoRepository,
) {
    private val notExistCompany: MutableList<String> = mutableListOf()
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val baseDir = File("crawl/dart/상속세매매전략")

    fun analysis(year: Int, quarter: Quarter): List<InheritanceTaxScore> {
        val koreanCompanyList = crawlerKoreanCompanyService.getCompanyList()
        log.info("종목 수: ${koreanCompanyList.size}")
        log.info("조회 종목 수: ${koreanCompanyList.size}개")

        val codeByName = koreanCompanyList.associate { it.code to it.name }
        val inheritanceScore = koreanCompanyList.map { company ->
            val assets: Long
            val liabilities: Long

            val currentYearProfit: Long
            val previousYearProfit: Long
            val twoYearsAgoProfit: Long

            try {
                val netAssetValue = getNetAssetValue(company.code, year, quarter)
                assets = netAssetValue.first
                liabilities = netAssetValue.second
                val netProfitValue = getNetProfitValue(company.code, year, quarter)
                currentYearProfit = netProfitValue.first
                previousYearProfit = netProfitValue.second
                twoYearsAgoProfit = netProfitValue.third

            } catch (e: Exception) {
                log.info("${e.message}, ${codeByName[company.code]}")
                notExistCompany.add(company.code)
                return@map null
            }
            InheritanceTaxScore(
                companyInfo = company,
                assets = assets,
                liabilities = liabilities,
                currentYearProfit = currentYearProfit,
                previousYearProfit = previousYearProfit,
                twoYearsAgoProfit = twoYearsAgoProfit,
            )
        }.filterNotNull()

        return inheritanceScore
    }

    /**
     * 엑셀 리포트 만듦
     */
    fun makeReport(inheritanceTaxScore: List<InheritanceTaxScore>, reportNamePrefix: String) {
        val resultFile = File(baseDir, reportNamePrefix + "_상속세매매전략.xlsx")

        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet()
            val header = "이름,종목코드,링크(네이버),링크(알파스퀘어),마켓,시총(억원)," +
                    "상속세 매매전략 점수,순자산가치,순이익가치," +
                    "자산,부채,최근 4분기 순이익,직전 최근 4분기 순이익,전전 최근 4분기 순이익"
            ReportMakerHelperService.applyHeader(sheet, header)

            val defaultStyle = ReportMakerHelperService.ExcelStyle.createDefault(workbook)
            val commaStyle = ReportMakerHelperService.ExcelStyle.createComma(workbook)
            val decimalStyle = ReportMakerHelperService.ExcelStyle.createDecimal(workbook)
            val hyperlinkStyle = ReportMakerHelperService.ExcelStyle.createHyperlink(workbook)

            val createHelper: CreationHelper = workbook.creationHelper
            var rowIdx = 1

            inheritanceTaxScore
                .sortedByDescending { it.inheritanceTaxScore }
                .forEach {
                    val row = sheet.createRow(rowIdx++)
                    var cellIdx = 0
                    var createCell = row.createCell(cellIdx++)
                    createCell.setCellValue(it.companyInfo.name)
                    createCell.cellStyle = defaultStyle

                    createCell = row.createCell(cellIdx++)
                    createCell.setCellValue(it.companyInfo.code)
                    createCell.cellStyle = defaultStyle

                    createCell = row.createCell(cellIdx++)
                    val link = createHelper.createHyperlink(HyperlinkType.URL)
                    link.address = CrawlerKoreanCompanyProperties.getDetailNaverDetailUrl(it.companyInfo.code)
                    createCell.setHyperlink(link)
                    createCell.setCellValue(CrawlerKoreanCompanyProperties.getNaverDetailUrl(it.companyInfo.code))
                    createCell.cellStyle = hyperlinkStyle

                    createCell = row.createCell(cellIdx++)
                    val link2 = createHelper.createHyperlink(HyperlinkType.URL)
                    link2.address = CrawlerKoreanCompanyProperties.getAlphaSquareDetailUrl(it.companyInfo.code)
                    createCell.setHyperlink(link2)
                    createCell.setCellValue(CrawlerKoreanCompanyProperties.getAlphaSquareDetailUrl(it.companyInfo.code))
                    createCell.cellStyle = hyperlinkStyle

                    createCell = row.createCell(cellIdx++)
                    createCell.setCellValue(it.companyInfo.market)
                    createCell.cellStyle = defaultStyle

                    createCell = row.createCell(cellIdx++)
                    createCell.setCellValue(it.companyInfo.capitalization.toDouble())
                    createCell.cellStyle = commaStyle

                    createCell = row.createCell(cellIdx++)
                    createCell.setCellValue(it.inheritanceTaxScore)
                    createCell.cellStyle = decimalStyle

                    createCell = row.createCell(cellIdx++)
                    createCell.setCellValue(it.netAssetValue.toDouble())
                    createCell.cellStyle = commaStyle

                    createCell = row.createCell(cellIdx++)
                    createCell.setCellValue(it.netProfitValue.toDouble())
                    createCell.cellStyle = commaStyle

                    createCell = row.createCell(cellIdx++)
                    createCell.setCellValue(it.assets.toDouble())
                    createCell.cellStyle = commaStyle

                    createCell = row.createCell(cellIdx++)
                    createCell.setCellValue(it.liabilities.toDouble())
                    createCell.cellStyle = commaStyle

                    createCell = row.createCell(cellIdx++)
                    createCell.setCellValue(it.currentYearProfit.toDouble())
                    createCell.cellStyle = commaStyle

                    createCell = row.createCell(cellIdx++)
                    createCell.setCellValue(it.previousYearProfit.toDouble())
                    createCell.cellStyle = commaStyle

                    createCell = row.createCell(cellIdx++)
                    createCell.setCellValue(it.twoYearsAgoProfit.toDouble())
                    createCell.cellStyle = commaStyle
                }

            sheet.createFreezePane(0, 1)
            sheet.defaultColumnWidth = 14
            for (i in 1..header.split(",").size) {
                sheet.autoSizeColumn(i)
            }

            sheet.setAutoFilter(CellRangeAddress(0, 0, 0, header.split(",").size - 1))
            ReportMakerHelperService.ExcelStyle.applyAllBorder(sheet)
            ReportMakerHelperService.ExcelStyle.applyDefaultFont(sheet)

            workbook.setSheetName(workbook.getSheetIndex(sheet), "매수 대상 순위")

            log.info("결과 저장: ${resultFile.absoluteFile}")
            FileOutputStream(resultFile).use { ous ->
                workbook.write(ous)
            }
        }
    }

    /**
     * @return <자산, 부채>
     */
    fun getNetAssetValue(stockCode: String, year: Int, quarter: Quarter): Pair<Long, Long> {
        val totalAssets: CorporateDisclosureInfoEntity
        val totalLiabilities: CorporateDisclosureInfoEntity
        try {
            totalAssets = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.TOTAL_ASSETS, year)
                .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
            totalLiabilities = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.TOTAL_LIABILITIES, year)
                .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
        } catch (e: Exception) {
            throw e
        }

        return when (quarter) {
            Quarter.Q1 -> Pair(totalAssets.q1Value, totalLiabilities.q1Value)
            Quarter.Q2 -> Pair(totalAssets.q2Value, totalLiabilities.q2Value)
            Quarter.Q3 -> Pair(totalAssets.q3Value, totalLiabilities.q3Value)
            Quarter.Q4 -> Pair(totalAssets.q4Value, totalLiabilities.q4Value)
        }
    }

    /**
     * @return <최근 4분기 순이익, 직전 4분기 순이익, 전전 4분기 순이익>
     */
    fun getNetProfitValue(stockCode: String, year: Int, quarter: Quarter): Triple<Long, Long, Long> {
        val currentYearNetProfitThis = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.NET_PROFIT, year)
            .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
        val previousNetProfitThis1 = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.NET_PROFIT, year - 1)
            .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
        val twoYearNetProfitThis = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.NET_PROFIT, year - 2)
            .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }

        var threeYearNetProfitThis3: CorporateDisclosureInfoEntity? = null
        if (quarter != Quarter.Q4) {
            threeYearNetProfitThis3 = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.NET_PROFIT, year - 3)
                .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
        }

        val currentYearProfit: Long
        val previousYearProfit: Long
        val twoYearsAgoProfit: Long
        when (quarter) {
            Quarter.Q1 -> {
                currentYearProfit = currentYearNetProfitThis.q1Value + previousNetProfitThis1.q4Value +
                        previousNetProfitThis1.q3Value + previousNetProfitThis1.q2Value
                previousYearProfit = previousNetProfitThis1.q1Value + twoYearNetProfitThis.q4Value +
                        twoYearNetProfitThis.q3Value + twoYearNetProfitThis.q2Value
                twoYearsAgoProfit = twoYearNetProfitThis.q1Value + threeYearNetProfitThis3!!.q4Value +
                        threeYearNetProfitThis3.q3Value + threeYearNetProfitThis3.q2Value
            }

            Quarter.Q2 -> {
                currentYearProfit = currentYearNetProfitThis.q2Value + currentYearNetProfitThis.q1Value +
                        previousNetProfitThis1.q4Value + previousNetProfitThis1.q3Value
                previousYearProfit = previousNetProfitThis1.q2Value + previousNetProfitThis1.q1Value +
                        twoYearNetProfitThis.q4Value + twoYearNetProfitThis.q3Value
                twoYearsAgoProfit = twoYearNetProfitThis.q2Value + twoYearNetProfitThis.q1Value +
                        threeYearNetProfitThis3!!.q4Value + threeYearNetProfitThis3.q3Value
            }

            Quarter.Q3 -> {
                currentYearProfit = currentYearNetProfitThis.q3Value + currentYearNetProfitThis.q2Value +
                        currentYearNetProfitThis.q1Value + previousNetProfitThis1.q4Value
                previousYearProfit = previousNetProfitThis1.q3Value + previousNetProfitThis1.q2Value +
                        previousNetProfitThis1.q1Value + twoYearNetProfitThis.q4Value
                twoYearsAgoProfit = twoYearNetProfitThis.q3Value + twoYearNetProfitThis.q2Value +
                        twoYearNetProfitThis.q1Value + threeYearNetProfitThis3!!.q4Value
            }

            Quarter.Q4 -> {
                currentYearProfit = currentYearNetProfitThis.getTotalValue()
                previousYearProfit = previousNetProfitThis1.getTotalValue()
                twoYearsAgoProfit = twoYearNetProfitThis.getTotalValue()
            }
        }

        return Triple(currentYearProfit, previousYearProfit, twoYearsAgoProfit)
    }
}