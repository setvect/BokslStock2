package com.setvect.bokslstock2.value.service

import com.google.gson.GsonBuilder
import com.setvect.bokslstock2.config.CrawlResourceProperties
import com.setvect.bokslstock2.value.dto.CompanySummaryDto
import org.apache.commons.io.FileUtils
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File


@Service
class CrawlCompanyValueService(
    val crawlResourceProperties: CrawlResourceProperties
) {
    private val regexCompanyLink = Regex("code=(\\w*).*>(.*)<")
    private val log = LoggerFactory.getLogger(javaClass)


    fun craw() {
        val crawCompanyList = crawCompanyList()
        save(crawCompanyList)
    }

    private fun save(crawCompanyList: List<CompanySummaryDto>) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val companyListJson = gson.toJson(crawCompanyList)

        val file = File(crawlResourceProperties.savePath, "stock-list.json")
        FileUtils.writeStringToFile(file, companyListJson, "utf-8")
        log.info("크롤링 결과 저장. 건수: ${crawCompanyList.size}, 경로: ${file.absoluteFile}")
    }

    private fun crawCompanyList(): List<CompanySummaryDto> {
        val companyList = mutableListOf<CompanySummaryDto>()

        KoreaStockType.values().forEach { stockType ->
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
                            capitalization = row.select("td:eq(6)").text().replace(",", "").toInt(),
                            currentPrice = row.select("td:eq(2)").text().replace(",", "").toInt()
                        )
                    )
                }
                page++
            }
        }
        return companyList.toList()
    }
}