package com.setvect.bokslstock2.value.service

import com.google.gson.GsonBuilder
import com.setvect.bokslstock2.config.CrawlResourceProperties
import com.setvect.bokslstock2.value.dto.CompanyDetailDto
import com.setvect.bokslstock2.value.dto.CompanySummaryDto
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.stream.IntStream
import kotlin.streams.toList



@Service
class CrawlerCompanyValueService(
    val crawlResourceProperties: CrawlResourceProperties
) {
    private val regexCompanyLink = Regex("code=(\\w*).*>(.*)<")
    private val log = LoggerFactory.getLogger(javaClass)
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun crawlDetailList() {
        val listFile = getSummaryListFile()
        val listJson = FileUtils.readFileToString(listFile, "utf-8")
        val companyList = gson.fromJson(listJson, Array<CompanySummaryDto>::class.java).asList()

        val companyDetailList = mutableListOf<CompanyDetailDto>()
        var count = 0
        companyList.forEach { company ->
            val url = crawlResourceProperties.url.info.replace("{code}", company.code)
            log.info("${company.name} 조회, $url, [${count++}]")

            val document = Jsoup.connect(url).get()
            val infoBox = document.select("#tab_con1")
            // eps 항목이 있으면 일반주식
            if (infoBox.select("#_eps").size == 0) {
                log.info("${company.name} 통과")
                return@forEach
            }

            val companySummaryDto = parsingCompanyDetail(infoBox, document, company)
            companyDetailList.add(companySummaryDto)
            if (companyDetailList.size % 50 == 0) {
                saveDetailList(companyDetailList)
            }

            val wait = 1500 + (Math.random() * 1000).toLong()
            TimeUnit.MILLISECONDS.sleep(wait)
        }
        saveDetailList(companyDetailList)
        log.info("수집 종료")
    }

    private fun parsingCompanyDetail(
        infoBox: Elements,
        document: Document,
        company: CompanySummaryDto
    ): CompanyDetailDto {
        val currentIndicator = extractCurrentIndicator(infoBox)
        val industry = document.select(".sub_tit7 em a").text()
        val valueHistory = document.select(".tb_type1_ifrs")
        val historyValueList = IntStream.range(0, 10).mapToObj { colIdx ->
            val date = valueHistory.select("tr")[1].select("th:eq(${colIdx})").text()
            if (StringUtils.isEmpty(date)) {
                return@mapToObj null
            }
            val historyData = CompanyDetailDto.HistoryData(
                date = date,
                sales = elementToIntOrNull(
                    valueHistory.select("tbody tr:eq(0)").select("td:eq(${colIdx + 1})")
                ),
                op = elementToIntOrNull(
                    valueHistory.select("tbody tr:eq(1)").select("td:eq(${colIdx + 1})")
                ),
                np = elementToIntOrNull(
                    valueHistory.select("tbody tr:eq(2)").select("td:eq(${colIdx + 1})")
                ),
                dr = elementToDoubleOrNull(
                    valueHistory.select("tbody tr:eq(6)").select("td:eq(${colIdx + 1})")
                ),
                cr = elementToDoubleOrNull(
                    valueHistory.select("tbody tr:eq(7)").select("td:eq(${colIdx + 1})")
                ),
                eps = elementToDoubleOrNull(
                    valueHistory.select("tbody tr:eq(9)").select("td:eq(${colIdx + 1})")
                ),
                per = elementToDoubleOrNull(
                    valueHistory.select("tbody tr:eq(10)").select("td:eq(${colIdx + 1})")
                ),
                pbr = elementToDoubleOrNull(
                    valueHistory.select("tbody tr:eq(12)").select("td:eq(${colIdx + 1})")
                ),
                dvr = elementToDoubleOrNull(
                    valueHistory.select("tbody tr:eq(14)").select("td:eq(${colIdx + 1})")
                ),
                dvrPayout = elementToDoubleOrNull(
                    valueHistory.select("tbody tr:eq(15)").select("td:eq(${colIdx + 1})")
                )
            )
            historyData
        }.filter { it != null }.toList() as List<CompanyDetailDto.HistoryData>
        return CompanyDetailDto(
            companySummaryDto = company,
            normalStock = true,
            industry = industry,
            currentIndicator = currentIndicator,
            historyData = historyValueList,
        )
    }


    /**
     * 현재 지표
     */
    private fun extractCurrentIndicator(infoBox: Elements): CompanyDetailDto.CurrentIndicator {
        val select = infoBox.select("#tab_con1 > .first tbody tr")
        val shareText = select[2].select("td")[0].text()
        return CompanyDetailDto.CurrentIndicator(
            shareNumber = shareText.replace(",", "").toLong(),
            per = elementToDoubleOrNull(infoBox.select("#_per")),
            eps = elementToDoubleOrNull(infoBox.select("#_eps")),
            pbr = elementToDoubleOrNull(infoBox.select("#_pbr")),
            dvr = elementToDoubleOrNull(infoBox.select("#_dvr")),
        )
    }

    fun crawlSummaryList() {
        val crawCompanyList = crawlList()
        saveSummaryList(crawCompanyList)
    }

    private fun getDetailListFile(): File {
        return File(crawlResourceProperties.savePath, ValueConstant.LIST_DETAIL_JSON)
    }

    private fun getSummaryListFile(): File {
        return File(crawlResourceProperties.savePath, ValueConstant.LIST_SUMMARY_JSON)
    }

    private fun crawlList(): List<CompanySummaryDto> {
        val companyList = mutableListOf<CompanySummaryDto>()

        KoreaMarket.values().forEach { stockType ->
            var page = 1
            while (true) {
                val url = crawlResourceProperties.url.list
                    .replace("{marketSeq}", stockType.code.toString())
                    .replace("{page}", page.toString())


                val document = Jsoup.connect(url).get()

                val elements = document.select("table.type_2 tbody tr[onmouseover]")
                if (elements.size == 0) {
                    break
                }
                elements.forEach { row ->
                    val link = row.select("td:eq(1)").html()
                    val matchResult = regexCompanyLink.find(link)
                    val matchGroup = matchResult!!.groupValues

                    companyList.add(
                        CompanySummaryDto(
                            code = matchGroup[1],
                            name = matchGroup[2],
                            market = stockType.name,
                            capitalization = elementToInt(row.select("td:eq(6)")),
                            currentPrice = elementToInt(row.select("td:eq(2)"))
                        )
                    )
                }
                page++
            }
        }
        return companyList.toList()
    }

    private fun saveDetailList(detailList: List<CompanyDetailDto>) {
        val companyListJson = gson.toJson(detailList)

        val listFile = getDetailListFile()
        FileUtils.writeStringToFile(listFile, companyListJson, "utf-8")
        log.info("상세 정보 저장. 건수: ${detailList.size}, 경로: ${listFile.absoluteFile}")
    }

    private fun saveSummaryList(crawCompanyList: List<CompanySummaryDto>) {
        val companyListJson = gson.toJson(crawCompanyList)

        val listFile = getSummaryListFile()
        FileUtils.writeStringToFile(listFile, companyListJson, "utf-8")
        log.info("요약 정보 저장. 건수: ${crawCompanyList.size}, 경로: ${listFile.absoluteFile}")
    }


    private fun elementToIntOrNull(element: Elements): Int? {
        return try {
            elementToInt(element)
        } catch (e: Exception) {
            null
        }
    }

    private fun elementToDoubleOrNull(element: Elements): Double? {
        return try {
            elementToDouble(element)
        } catch (e: Exception) {
            null
        }
    }

    private fun elementToDouble(element: Elements) = element.text().replace(",", "").toDouble()

    private fun elementToInt(element: Elements) = element.text().replace(",", "").toInt()


}