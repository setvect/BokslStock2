package com.setvect.bokslstock2.crawl.koreacompany.service

import com.google.gson.GsonBuilder
import com.setvect.bokslstock2.value.dto.KoreanCompanyDetail
import com.setvect.bokslstock2.value.dto.KoreanCompanySummary
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import java.util.stream.IntStream

/**
 * 한국 기업 정보 크롤링
 */
@Service
class CrawlerKoreanCompanyService {
    private val regexCompanyLink = Regex("code=(\\w*).*>(.*)<")
    private val log = LoggerFactory.getLogger(javaClass)
    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * 상세 정보 크롤링
     */
    fun crawlDetailList() {
        val listFile = CrawlerKoreanCompanyProperties.getSummaryListFile()
        val listJson = FileUtils.readFileToString(listFile, "utf-8")
        val companyList = gson.fromJson(listJson, Array<KoreanCompanySummary>::class.java).asList()

        val koreanCompanyDetailList = mutableListOf<KoreanCompanyDetail>()
        var count = 0
        companyList.forEach { company ->
            val url = CrawlerKoreanCompanyProperties.getDetailUrl(company.code)
            log.info("${company.name} 조회, $url, [${count++}/${companyList.size}]")

            val document = Jsoup.connect(url).get()
            val infoBox = document.select("#tab_con1")
            // eps 항목이 있으면 일반주식
            if (infoBox.select("#_eps").size == 0) {
                log.info("${company.name} 통과")
                return@forEach
            }

            val companySummaryDto = parsingCompanyDetail(infoBox, document, company)
            koreanCompanyDetailList.add(companySummaryDto)
            if (koreanCompanyDetailList.size % 50 == 0) {
                saveDetailList(koreanCompanyDetailList)
            }

            sleep(1500, 1000)
        }
        saveDetailList(koreanCompanyDetailList)
        log.info("수집 종료")
    }


    private fun sleep(baseSleep: Int, randomSleep: Int) {
        val wait = baseSleep + (Math.random() * randomSleep).toLong()
        TimeUnit.MILLISECONDS.sleep(wait)
    }

    private fun parsingCompanyDetail(
        infoBox: Elements,
        document: Document,
        company: KoreanCompanySummary
    ): KoreanCompanyDetail {
        val currentIndicator = extractCurrentIndicator(infoBox)
        val industry = document.select(".sub_tit7 em a").text()
        val valueHistory = document.select(".tb_type1_ifrs")
        val historyValueList = IntStream.range(0, 10).mapToObj { colIdx ->
            val date = valueHistory.select("tr")[1].select("th:eq(${colIdx})").text()
            if (StringUtils.isEmpty(date)) {
                return@mapToObj null
            }
            val historyData = KoreanCompanyDetail.HistoryData(
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
        }.filter { it != null }.toList() as List<KoreanCompanyDetail.HistoryData>

        return KoreanCompanyDetail(
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
    private fun extractCurrentIndicator(infoBox: Elements): KoreanCompanyDetail.CurrentIndicator {
        val select = infoBox.select("#tab_con1 > .first tbody tr")
        val shareText = select[2].select("td")[0].text()
        return KoreanCompanyDetail.CurrentIndicator(
            shareNumber = shareText.replace(",", "").toLong(),
            per = elementToDoubleOrNull(infoBox.select("#_per")),
            eps = elementToDoubleOrNull(infoBox.select("#_eps")),
            pbr = elementToDoubleOrNull(infoBox.select("#_pbr")),
            dvr = elementToDoubleOrNull(infoBox.select("#_dvr")),
        )
    }

    /**
     * 종목 크롤링
     */
    fun crawlSummaryList() {
        val crawCompanyList = crawlList()
        saveSummaryList(crawCompanyList)
    }

    /**
     * 종목 목록
     */
    private fun crawlList(): List<KoreanCompanySummary> {
        val companyList = mutableListOf<KoreanCompanySummary>()

        KoreaMarket.values().forEach { stockType ->
            var page = 1
            while (true) {
                val url = CrawlerKoreanCompanyProperties.getUrlList()
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
                        KoreanCompanySummary(
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

    private fun saveDetailList(detailList: List<KoreanCompanyDetail>) {
        val companyListJson = gson.toJson(detailList)

        val listFile = CrawlerKoreanCompanyProperties.getDetailListFile()
        FileUtils.writeStringToFile(listFile, companyListJson, "utf-8")
        log.info("상세 정보 저장. 건수: ${detailList.size}, 경로: ${listFile.absoluteFile}")
    }

    /**
     * 종목 저장
     */
    private fun saveSummaryList(crawCompanyList: List<KoreanCompanySummary>) {
        val companyListJson = gson.toJson(crawCompanyList)

        val listFile = CrawlerKoreanCompanyProperties.getSummaryListFile()
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

    enum class KoreaMarket(val code: Int) {
        KOSPI(0), KOSDAQ(1);
    }
}