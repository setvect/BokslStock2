package com.setvect.bokslstock2.strategy.companyvalue.service

import com.google.gson.GsonBuilder
import com.setvect.bokslstock2.backtest.common.service.ReportMakerHelperService
import com.setvect.bokslstock2.util.NumberUtil
import com.setvect.bokslstock2.strategy.companyvalue.model.Rank
import com.setvect.bokslstock2.strategy.companyvalue.model.UsaCompanyDetail
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.poi.common.usermodel.HyperlinkType
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.modelmapper.TypeToken
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors


@Service
/**
 * 미국 기업 주식 가치 평가 전략
 */
class ValueAnalysisUsaCompanyService {
    private val log = LoggerFactory.getLogger(javaClass)
    private val gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        private val USA_COMPANY_VALUE_FILE = File("crawl/finviz.com/<선택하세요>.json")
        private val INCLUDE_COUNTRY = listOf("USA")
        // Energy 항목 추가할까 말까 고민. 재수 없으면 PTP 종목에 들어갈 수도 있음
        private val EXCLUDE_SECTOR = listOf("Real Estate", "Financial", "Energy")
        private val UPPER_RATIO = .7
        private val LOWER_RATIO = .9
        private val PTP_LIST_PATH = listOf("assets/PTP1.txt", "assets/PTP2.txt")
    }

    /**
     * 미국 기업 주식 가치 평가 전략
     */
    fun analysis() {
        log.info("미국 기업 주식 가치 평가 전략")
        val companyList = loadCompanyInfo()
        val usaCompanyDetailList = loadCompanyDetail(companyList)
        val targetList = filter(usaCompanyDetailList)
        val listByRanking = ranking(targetList)


        val resultFile = File("crawl/finviz.com/value-usa-result.xlsx")
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet()

            val header = "이름,티커,링크,링크2,국가,마켓,시총(달러),현재가(달러),섹터,업종," +
                    "현재-PER,현재-PBR,현재-배당수익률," +
                    "순위-PER,순위-PBR,순위-배당수익률,순위합계"
            ReportMakerHelperService.applyHeader(sheet, header)
            var rowIdx = 1

            val defaultStyle = ReportMakerHelperService.ExcelStyle.createDefault(workbook)
            val commaStyle = ReportMakerHelperService.ExcelStyle.createComma(workbook)
            val commaDecimalStyle = ReportMakerHelperService.ExcelStyle.createCommaDecimal(workbook)
            val decimalStyle = ReportMakerHelperService.ExcelStyle.createDecimal(workbook)
            val percentStyle = ReportMakerHelperService.ExcelStyle.createPercent(workbook)
            val hyperlinkStyle = ReportMakerHelperService.ExcelStyle.createHyperlink(workbook)

            val createHelper: CreationHelper = workbook.creationHelper

            listByRanking.forEach {
                val row = sheet.createRow(rowIdx++)
                var cellIdx = 0
                var createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.name)
                createCell.cellStyle = defaultStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.ticker)
                createCell.cellStyle = defaultStyle

                createCell = row.createCell(cellIdx++)
                val link = createHelper.createHyperlink(HyperlinkType.URL)
                link.address = getDetailUrl(it.first.ticker)
                createCell.setHyperlink(link)
                createCell.setCellValue(getDetailUrl(it.first.ticker))
                createCell.cellStyle = hyperlinkStyle

                createCell = row.createCell(cellIdx++)
                val link2 = createHelper.createHyperlink(HyperlinkType.URL)
                link2.address = getDetailUrl2(it.first.ticker)
                createCell.setHyperlink(link2)
                createCell.setCellValue(getDetailUrl2(it.first.ticker))
                createCell.cellStyle = hyperlinkStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.country)
                createCell.cellStyle = commaDecimalStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.index.joinToString(", "))
                createCell.cellStyle = defaultStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.marketCap!!)
                createCell.cellStyle = commaStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.price!!)
                createCell.cellStyle = decimalStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.sector)
                createCell.cellStyle = defaultStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.industry)
                createCell.cellStyle = defaultStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.currentIndicator.per!!)
                createCell.cellStyle = decimalStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.currentIndicator.pbr!!)
                createCell.cellStyle = decimalStyle

                createCell = row.createCell(cellIdx++)
                createCell.setCellValue(it.first.currentIndicator.dvr!!)
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

    private fun getDetailUrl(ticker: String): String {
        return "https://finviz.com/quote.ashx?t=$ticker"
    }

    private fun getDetailUrl2(ticker: String): String {
        return "https://alphasquare.co.kr/home/stock/financial-information?code=$ticker"
    }

    private fun filter(companyAllList: List<UsaCompanyDetail>): List<UsaCompanyDetail> {
        log.info("전체 Size: " + companyAllList.size)
        val ptp = loadPtpTicker()
        log.info("PTP 종목: " + ptp.size)
        val companyFilterList = companyAllList
            .asSequence()
            .filter { INCLUDE_COUNTRY.contains(it.country) }
            .filter { !EXCLUDE_SECTOR.contains(it.sector) }
            .filter { it.currentIndicator.per != null && it.currentIndicator.pbr != null && it.currentIndicator.dvr != null }
            .filter {
                val isPtp = ptp.contains(it.ticker)
                if (isPtp) {
                    log.info("PTP 종목: ${it.ticker}, 제외 함")
                }
                !isPtp
            }
            .sortedByDescending { it.marketCap }
            .toList()
        log.info("필터 Size: " + companyFilterList.size)

        // 시총 기준 추출범위 설정
        val fromIndex = (companyFilterList.size * UPPER_RATIO).toInt()
        val toIndex = (companyFilterList.size * LOWER_RATIO).toInt()
        return companyFilterList.subList(fromIndex, toIndex)
    }

    private fun ranking(targetList: List<UsaCompanyDetail>): List<Pair<UsaCompanyDetail, Rank>> {
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


    private fun loadCompanyDetail(companyList: List<Map<String, String>>): List<UsaCompanyDetail> {
        val usaCompanyDetailList = mutableListOf<UsaCompanyDetail>()
        for (company in companyList) {
            val ticker = company["Ticker"]!!
            val name = company["Company"]!!
            val sector = company["Sector"]!!
            val industry = company["Industry"]!!
            val country = company["Country"]!!
            val index = company["Index"]!!.split(",").map { it.trim() }.toSet()
            val price = company["Price"]?.let { NumberUtil.unitToNumber(it) }
            val marketCap = company["Market Cap"]?.let { NumberUtil.unitToNumber(it) }

            val currentIndicator = UsaCompanyDetail.CurrentIndicator(
                per = company["P/E"]?.takeIf { it != "-" }?.toDouble(),
                eps = company["EPS"]?.takeIf { it != "-" }?.toDouble(),
                pbr = company["P/B"]?.takeIf { it != "-" }?.toDouble(),
                dvr = company["Dividend"]?.takeIf { it != "-" }?.let { NumberUtil.percentToNumber(it) },
            )
            val usaCompanyDetail = UsaCompanyDetail(
                ticker = ticker,
                name = name,
                sector = sector,
                industry = industry,
                country = country,
                index = index,
                price = price,
                marketCap = marketCap,
                currentIndicator = currentIndicator,
            )
            usaCompanyDetailList.add(usaCompanyDetail)
        }
        return usaCompanyDetailList
    }

    /**
     * 중복제거된 기업 정보
     */
    private fun loadCompanyInfo(): List<Map<String, String>> {
        val json = FileUtils.readFileToString(USA_COMPANY_VALUE_FILE, "UTF-8")
        val type = object : TypeToken<List<Map<String, String>>>() {}.type
        val loadCompanyDetails: List<Map<String, String>> = gson.fromJson(json, type)
        return loadCompanyDetails.distinctBy { it["Ticker"] }
    }

    /**
     * PTP 종목 리스트
     */
    private fun loadPtpTicker(): Set<String> {
        // PTP_LIST_PATH 파일을 읽어서 Set으로 반환
        val content = PTP_LIST_PATH.stream().map {
            val inputStream = {}.javaClass.classLoader.getResourceAsStream(it)
            return@map IOUtils.toString(inputStream, StandardCharsets.UTF_8)
        }.collect(Collectors.joining("\n"))

        return content.split("\n")
            .filter { it.isNotBlank() }
            .filter { !it.startsWith("#") }
            .map { it.trim() }
            .toSet()
    }
}