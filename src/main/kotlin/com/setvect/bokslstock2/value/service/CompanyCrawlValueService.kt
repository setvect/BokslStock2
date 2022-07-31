package com.setvect.bokslstock2.value.service

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.setvect.bokslstock2.config.CrawlResourceProperties
import com.setvect.bokslstock2.value.dto.CompanyDetailDto
import com.setvect.bokslstock2.value.dto.CompanySummaryDto
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.stream.IntStream
import kotlin.streams.toList


private const val LIST_JSON = "stock-list.json"

@Service
class CompanyCrawlValueService(
    val crawlResourceProperties: CrawlResourceProperties
) {
    private val regexCompanyLink = Regex("code=(\\w*).*>(.*)<")
    private val log = LoggerFactory.getLogger(javaClass)

    fun crawlCompanyDetail() {
        val listFile = getCompanyList()
        val listJson = FileUtils.readFileToString(listFile, "utf-8")
        val companyList = Gson().fromJson(listJson, Array<CompanySummaryDto>::class.java).asList()

        companyList.forEach { company ->
            val url = crawlResourceProperties.url.companyInfo.replace("{code}", company.code)
            log.info("${company.name} 조회, $url")

            val document = Jsoup.connect(url).get()


            val infoBox = document.select("#tab_con1")
            var normalStock = false
            // eps 항목이 있으면 일반주식
            if (infoBox.select("#_eps").size != 0) {
                normalStock = true
                val currentIndicator = extractCurrentIndicator(infoBox)
                val industry = document.select(".sub_tit7 em a").text()
                val valueHistory = document.select(".tb_type1_ifrs")

                val toList = IntStream.range(0, 10).mapToObj { colIdx ->
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
                    print("\n=========\n$historyData")
                    historyData
                }.filter { it != null }.toList()
                println(toList)
            }

            val wait = 1500 + (Math.random() * 1000).toLong()
            TimeUnit.MICROSECONDS.sleep(wait)
        }
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
            eps = elementToDouble(infoBox.select("#_eps")),
            pbr = elementToDouble(infoBox.select("#_pbr")),
            dvr = elementToDoubleOrNull(infoBox.select("#_dvr")),
        )
    }

    fun crawlCompanyList() {
        val crawCompanyList = crawlList()
        saveList(crawCompanyList)
    }

    private fun saveList(crawCompanyList: List<CompanySummaryDto>) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val companyListJson = gson.toJson(crawCompanyList)

        val listFile = getCompanyList()
        FileUtils.writeStringToFile(listFile, companyListJson, "utf-8")
        log.info("크롤링 결과 저장. 건수: ${crawCompanyList.size}, 경로: ${listFile.absoluteFile}")
    }

    private fun getCompanyList(): File {
        return File(crawlResourceProperties.savePath, LIST_JSON)
    }

    private fun crawlList(): List<CompanySummaryDto> {
        val companyList = mutableListOf<CompanySummaryDto>()

        KoreaMarket.values().forEach { stockType ->
            var page = 1
            while (true) {
                val url = crawlResourceProperties.url.stockList
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