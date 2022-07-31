package com.setvect.bokslstock2.value.service

import com.google.gson.GsonBuilder
import com.setvect.bokslstock2.config.CrawlResourceProperties
import com.setvect.bokslstock2.value.dto.CompanyDetail
import com.setvect.bokslstock2.value.dto.CompanySummary
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import java.util.stream.IntStream
import kotlin.streams.toList



@Service
class CrawlerCompanyValueService(
    val crawlResourceProperties: CrawlResourceProperties,
    val valueCommonService: ValueCommonService
) {
    private val regexCompanyLink = Regex("code=(\\w*).*>(.*)<")
    private val log = LoggerFactory.getLogger(javaClass)
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun crawlDetailList() {
        val listFile = valueCommonService.getSummaryListFile()
        val listJson = FileUtils.readFileToString(listFile, "utf-8")
        val companyList = gson.fromJson(listJson, Array<CompanySummary>::class.java).asList()

        val companyDetailList = mutableListOf<CompanyDetail>()
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

            sleep(1500, 1000)
        }
        saveDetailList(companyDetailList)
        log.info("수집 종료")
    }

    private fun sleep(baseSleep: Int, randomSleep: Int) {
        val wait = baseSleep + (Math.random() * randomSleep).toLong()
        TimeUnit.MILLISECONDS.sleep(wait)
    }

    private fun parsingCompanyDetail(
        infoBox: Elements,
        document: Document,
        company: CompanySummary
    ): CompanyDetail {
        val currentIndicator = extractCurrentIndicator(infoBox)
        val industry = document.select(".sub_tit7 em a").text()
        val valueHistory = document.select(".tb_type1_ifrs")
        val historyValueList = IntStream.range(0, 10).mapToObj { colIdx ->
            val date = valueHistory.select("tr")[1].select("th:eq(${colIdx})").text()
            if (StringUtils.isEmpty(date)) {
                return@mapToObj null
            }
            val historyData = CompanyDetail.HistoryData(
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
        }.filter { it != null }.toList() as List<CompanyDetail.HistoryData>
        return CompanyDetail(
            summary = company,
            normalStock = true,
            industry = industry,
            currentIndicator = currentIndicator,
            historyData = historyValueList,
        )
    }


    /**
     * 현재 지표
     */
    private fun extractCurrentIndicator(infoBox: Elements): CompanyDetail.CurrentIndicator {
        val select = infoBox.select("#tab_con1 > .first tbody tr")
        val shareText = select[2].select("td")[0].text()
        return CompanyDetail.CurrentIndicator(
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

    private fun crawlList(): List<CompanySummary> {
        val companyList = mutableListOf<CompanySummary>()

        KoreaMarket.values().forEach { stockType ->
            var page = 1
            while (true) {
                val url = crawlResourceProperties.url.list
                    .replace("{marketSeq}", stockType.code.toString())
                    .replace("{page}", page.toString())
                log.info("페이지: $url")
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
                        CompanySummary(
                            code = matchGroup[1],
                            name = matchGroup[2],
                            market = stockType.name,
                            capitalization = elementToInt(row.select("td:eq(6)")),
                            currentPrice = elementToInt(row.select("td:eq(2)"))
                        )
                    )
                }
                page++
                sleep(500, 200)
            }
        }
        return companyList.toList()
    }

    private fun saveDetailList(detailList: List<CompanyDetail>) {
        val companyListJson = gson.toJson(detailList)

        val listFile = valueCommonService.getDetailListFile()
        FileUtils.writeStringToFile(listFile, companyListJson, "utf-8")
        log.info("상세 정보 저장. 건수: ${detailList.size}, 경로: ${listFile.absoluteFile}")
    }

    private fun saveSummaryList(crawCompanyList: List<CompanySummary>) {
        val companyListJson = gson.toJson(crawCompanyList)

        val listFile = valueCommonService.getSummaryListFile()
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