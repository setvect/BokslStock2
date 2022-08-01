package com.setvect.bokslstock2.value.service

import com.google.gson.GsonBuilder
import com.setvect.bokslstock2.analysis.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.value.dto.CompanyDetail
import com.setvect.bokslstock2.value.dto.Rank
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
 * 가치 평가 전략
 */
class ValueAnalysisService(
    val valueCommonService: ValueCommonService
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val log = LoggerFactory.getLogger(javaClass)
    private val excludeIndustry = listOf("기타금융", "생명보험", "손해보험", "은행", "증권", "창업투자")
    private val upperRatio = .7
    private val lowerRatio = .9

    fun analysis() {
        val companyAllList = loadCompanyDetails()
        val targetList = filter(companyAllList)
        val listByRanking = ranking(targetList)

        val resultFile = valueCommonService.getResultFile()
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet()

            val header = "이름,종목코드,링크,마켓,시총,현재가,업종," +
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
                val link = createHelper.createHyperlink(HyperlinkType.URL)
                link.address = valueCommonService.getDetailUrl(it.first.summary.code)
                createCell.setHyperlink(link)
                createCell.setCellValue(valueCommonService.getDetailUrl(it.first.summary.code))
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
                createCell.setCellValue(it.first.industry)
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
                createCell.setCellValue(it.second.per!!.toDouble())
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.second.pbr!!.toDouble())
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.second.dvr!!.toDouble())
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.second.total()!!.toDouble())
                createCell.cellStyle = commaStyle
            }
            sheet.createFreezePane(0, 1)
            sheet.defaultColumnWidth = 14
            sheet.setColumnWidth(0, 6000)
            sheet.setColumnWidth(2, 12000)
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

    private fun filter(companyAllList: List<CompanyDetail>): List<CompanyDetail> {
        println("전체 Size: " + companyAllList.size)
        val companyFilterList = companyAllList
            .filter { !excludeIndustry.contains(it.industry) }
            .filter { it.currentIndicator.per != null && it.currentIndicator.pbr != null && it.currentIndicator.dvr != null }
            .sortedByDescending { it.summary.capitalization }
            .toList()
        println("필터 Size: " + companyFilterList.size)

        val fromIndex = (companyFilterList.size * upperRatio).toInt()
        val toIndex = (companyFilterList.size * lowerRatio).toInt()
        return companyFilterList.subList(fromIndex, toIndex)
    }

    private fun loadCompanyDetails(): List<CompanyDetail> {
        val detailFile = valueCommonService.getDetailListFile()
        val listJson = FileUtils.readFileToString(detailFile, "utf-8")
        return gson.fromJson(listJson, Array<CompanyDetail>::class.java).asList()
    }

    private fun ranking(targetList: List<CompanyDetail>): List<Pair<CompanyDetail, Rank>> {
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

        val listByRanking = targetByRank.sortedBy { it.second.total() }
        return listByRanking
    }
}