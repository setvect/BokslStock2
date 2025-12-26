package com.setvect.bokslstock2.strategy.companyvalue.service

import com.google.gson.GsonBuilder
import com.setvect.bokslstock2.backtest.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.crawl.koreacompany.service.CrawlerKoreanCompanyProperties
import com.setvect.bokslstock2.strategy.companyvalue.model.KoreanCompanyDetail
import com.setvect.bokslstock2.strategy.companyvalue.model.Rank
import org.apache.commons.io.FileUtils
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.FileOutputStream


@Service
/**
 * 한국 기업 가치 평가 전략
 */
class ValueAnalysisKoreanCompanyService {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val log = LoggerFactory.getLogger(javaClass)
    private val upperRatio = .7
    private val lowerRatio = .9

    fun analysis() {
        val companyAllList = loadCompanyDetails()
        val targetList = filter(companyAllList)
        val listByRanking = ranking(targetList)

        val resultFile = CrawlerKoreanCompanyProperties.getResultFile()
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet()

            val header = "이름,종목코드,링크(네이버),링크(알파스퀘어),마켓,시총(억원),현재가," +
                    "현재-PER,현재-PBR,현재-배당수익률," +
                    "순위-PER,순위-PBR,순위-배당수익률,순위합계"
            ReportMakerHelperService.applyHeader(sheet, header)
            var rowIdx = 1

            val defaultStyle = ReportMakerHelperService.ExcelStyle.createDefault(workbook)
            val commaStyle = ReportMakerHelperService.ExcelStyle.createComma(workbook)
            val decimalStyle = ReportMakerHelperService.ExcelStyle.createDecimal(workbook)
            val percentStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
            val hyperlinkStyle = ReportMakerHelperService.ExcelStyle.createHyperlink(workbook)

            val createHelper: CreationHelper = workbook.creationHelper

            listByRanking.forEach {
                val row = sheet.createRow(rowIdx++)
                var cellIdx = 0
                var createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.summary.name)
                createCell.cellStyle = defaultStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.summary.code)
                createCell.cellStyle = defaultStyle

                createCell = row.createCell(cellIdx++)
                val linkNaver = createHelper.createHyperlink(HyperlinkType.URL)
                linkNaver.address = CrawlerKoreanCompanyProperties.getNaverDetailUrl(it.first.summary.code)
                createCell.setHyperlink(linkNaver)
                createCell.setCellValue(CrawlerKoreanCompanyProperties.getNaverDetailUrl(it.first.summary.code))
                createCell.cellStyle = hyperlinkStyle

                createCell = row.createCell(cellIdx++)
                val linkAlpha = createHelper.createHyperlink(HyperlinkType.URL)
                linkAlpha.address = CrawlerKoreanCompanyProperties.getAlphaSquareDetailUrl(it.first.summary.code)
                createCell.setHyperlink(linkAlpha)
                createCell.setCellValue(CrawlerKoreanCompanyProperties.getAlphaSquareDetailUrl(it.first.summary.code))
                createCell.cellStyle = hyperlinkStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.summary.market)
                createCell.cellStyle = defaultStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.summary.capitalization.toDouble())
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.summary.currentPrice.toDouble())
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.currentIndicator.per!!)
                createCell.cellStyle = decimalStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.currentIndicator.pbr!!)
                createCell.cellStyle = decimalStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.currentIndicator.dvr!! / 100.0)
                createCell.cellStyle = percentStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.second.per.toDouble())
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.second.pbr.toDouble())
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.second.dvr.toDouble())
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.second.total().toDouble())
                createCell.cellStyle = commaStyle
            }
            sheet.createFreezePane(0, 1)
            sheet.defaultColumnWidth = 14
            sheet.setColumnWidth(0, 6000)
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

    private fun filter(companyAllList: List<KoreanCompanyDetail>): List<KoreanCompanyDetail> {
        println("전체 Size: " + companyAllList.size)
        val companyFilterList = companyAllList
            .filter { it.currentIndicator.per != null && it.currentIndicator.pbr != null && it.currentIndicator.dvr != null }
            .sortedByDescending { it.summary.capitalization }
            .toList()
        println("필터 Size: " + companyFilterList.size)
        // 시총 기준 추출범위 설정
        val fromIndex = (companyFilterList.size * upperRatio).toInt()
        val toIndex = (companyFilterList.size * lowerRatio).toInt()
        return companyFilterList.subList(fromIndex, toIndex)
    }

    private fun loadCompanyDetails(): List<KoreanCompanyDetail> {
        val detailFile = CrawlerKoreanCompanyProperties.getDetailListFile()
        val listJson = FileUtils.readFileToString(detailFile, "utf-8")
        return gson.fromJson(listJson, Array<KoreanCompanyDetail>::class.java).asList()
    }

    private fun ranking(targetList: List<KoreanCompanyDetail>): List<Pair<KoreanCompanyDetail, Rank>> {
        val targetByRank = targetList.map { Pair(it, Rank()) }

        targetByRank.sortedByDescending { 1 / it.first.currentIndicator.per!! }
            .forEachIndexed { index, pair ->
                pair.second.per = index + 1
            }

        targetByRank.sortedByDescending { 1 / it.first.currentIndicator.pbr!! }
            .forEachIndexed { index, pair ->
                pair.second.pbr = index + 1
            }

        targetByRank.sortedByDescending { it.first.currentIndicator.dvr!! }
            .forEachIndexed { index, pair ->
                pair.second.dvr = index + 1
            }

        return targetByRank.sortedBy { it.second.total() }
    }
}
