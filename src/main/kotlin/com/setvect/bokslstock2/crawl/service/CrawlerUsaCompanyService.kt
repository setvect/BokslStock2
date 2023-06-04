package com.setvect.bokslstock2.crawl.service

import com.setvect.bokslstock2.util.JsonUtil
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 미국 기업 정보 크롤링
 */
@Service
class CrawlerUsaCompanyService {

    private val URL =
        "https://finviz.com/screener.ashx?v=152&ft=4" +
                "&c=0,1,2,3,4,5,79,6,7,8,9,10,11,12,13,73,74,14,15,16,77,17,18,19,20,21,23,22,82,78,24,25,26,27,28,29,30,31,84,32,33" +
                ",34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,68,70,80,83,76,60,61,62,63,64,67,69,81,65,66,71,72"

    private val log = LoggerFactory.getLogger(javaClass)

    fun crawl(maxPages: Int = Int.MAX_VALUE): FinvizCrawlResult {
        val baseUrl = URL
        val header = mutableListOf<String>()
        val dataMatrix = mutableListOf<List<String>>()
        var pagePerCount = 0

        outerLoop@ for (page in 1..maxPages) {
            val r = (page - 1) * pagePerCount + 1
            val url = "$baseUrl&r=$r"
            val document = Jsoup.connect(url).get()
            val tableRows = document.select("table[class=table-light is-new] > tbody > tr")

            var first = true
            for (row in tableRows) {
                if (first) {
                    first = false
                    if (header.isEmpty()) {
                        val ths = row.children()
                        for (th in ths) {
                            header.add(th.text())
                        }
                    }
                    pagePerCount = tableRows.size - 1
                    continue
                }

                // dataMatrix 값을 누적한다.
                val ths = row.children()
                val item: MutableList<String> = mutableListOf()
                for (th in ths) {
                    item.add(th.text())
                }
                dataMatrix.add(item)
            }

            log.info("page: $page 수집완료")
            // 페이지 당 수집된 건수가 한 페이지 표시 건수보다 작으면 수집 종료
            if (tableRows.size < pagePerCount) {
                // 이럴 경우 상황에 따라 마지막 한건이 중복 수집될 가능성이 있음. 그냥 넘어가자.
                break@outerLoop
            }
            // 1000 ~ 2000ms 사이로 랜덤하게 sleep 한다.
            Thread.sleep((1000..2000).random().toLong())

        }
        return FinvizCrawlResult(header, dataMatrix)
    }

    // FinvizCrawlResult 값을 Map 넣고 JSON으로 변환환
    fun convertJson(result: FinvizCrawlResult): String {
        val header = result.header
        val dataMatrix = result.dataMatrix
        val json = dataMatrix.map { row ->
            val item = header.zip(row).toMap()
            item
        }
        return JsonUtil.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json)
    }


    data class FinvizCrawlResult(
        val header: List<String>,
        val dataMatrix: List<List<String>>
    )
}