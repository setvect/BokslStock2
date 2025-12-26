package com.setvect.bokslstock2.crawl.naver.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.setvect.bokslstock2.crawl.koreacompany.service.CrawlerKoreanCompanyProperties
import com.setvect.bokslstock2.strategy.companyvalue.model.KoreanCompanyDetail
import com.setvect.bokslstock2.strategy.companyvalue.model.KoreanCompanySummary
import com.setvect.bokslstock2.util.JsonUtil
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

@Service
class NaverCompanyValueCrawlerService(
    @Qualifier("crawlRestTemplate")
    private val crawlRestTemplate: RestTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun crawlAndSave(pageSize: Int = 200, maxPages: Int = Int.MAX_VALUE): List<KoreanCompanyDetail> {
        val details = crawlAll(pageSize, maxPages)
        writeDetailList(details)
        return details
    }

    fun crawlAll(pageSize: Int = 200, maxPages: Int = Int.MAX_VALUE): List<KoreanCompanyDetail> {
        val result = mutableListOf<KoreanCompanyDetail>()
        var startIdx = 0
        var page = 0
        var lastFirstCode: String? = null

        while (page < maxPages) {
            val items = fetchPage(startIdx, pageSize)

            if (items.isEmpty()) {
                break
            }

            val firstCode = items.first().itemcode?.trim()
            if (!firstCode.isNullOrBlank() && firstCode == lastFirstCode && startIdx > 0) {
                log.warn("naver market startIdx ignored? stop at page={}, startIdx={}", page + 1, startIdx)
                break
            }
            lastFirstCode = firstCode

            val mapped = items.mapNotNull { toCompanyDetailOrNull(it) }
            result.addAll(mapped)
            log.info("naver market page={} size={} mapped={}", page + 1, items.size, mapped.size)

            if (items.size < pageSize) {
                break
            }

            startIdx += pageSize
            page += 1
        }

        log.info("naver market total={}", result.size)
        return result
    }

    private fun writeDetailList(details: List<KoreanCompanyDetail>) {
        val file = CrawlerKoreanCompanyProperties.getDetailListFile()
        FileUtils.forceMkdirParent(file)
        val json = JsonUtil.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(details)
        FileUtils.writeStringToFile(file, json, StandardCharsets.UTF_8)
        log.info("save: {}", file.absoluteFile)
    }

    private fun buildUrl(startIdx: Int, pageSize: Int): String {
        return "https://stock.naver.com/api/domestic/market/stock/default" +
            "?tradeType=KRX&marketType=ALL&orderType=marketSum&startIdx=$startIdx&pageSize=$pageSize"
    }

    private fun fetchPage(startIdx: Int, pageSize: Int): List<NaverStockItem> {
        val url = buildUrl(startIdx, pageSize)
        val headers = HttpHeaders()
        headers["User-Agent"] = CrawlerKoreanCompanyProperties.USER_AGENT
        headers["Accept"] = "application/json"
        headers["Referer"] = "https://stock.naver.com/"
        val httpEntity = HttpEntity<Void>(headers)
        val response = crawlRestTemplate.exchange(url, HttpMethod.GET, httpEntity, String::class.java)
        val body = response.body ?: return emptyList()
        return JsonUtil.mapper.readValue(body, object : TypeReference<List<NaverStockItem>>() {})
    }

    private fun toCompanyDetailOrNull(item: NaverStockItem): KoreanCompanyDetail? {
        val code = item.itemcode?.trim().orEmpty()
        val name = item.itemname?.trim().orEmpty()
        if (code.isBlank() || name.isBlank()) {
            return null
        }

        val market = when (item.sosok?.trim()) {
            "0" -> "KOSPI"
            "1" -> "KOSDAQ"
            else -> "ETC"
        }

        val capitalization = parseLong(item.marketSum)?.let { (it / 100_000_000L).toInt() } ?: 0
        val currentPrice = parseInt(item.nowVal) ?: 0

        val summary = KoreanCompanySummary(
            code = code,
            name = name,
            market = market,
            capitalization = capitalization,
            currentPrice = currentPrice
        )

        val currentIndicator = KoreanCompanyDetail.CurrentIndicator(
            shareNumber = parseLong(item.listedStockCnt) ?: 0L,
            per = parseDouble(item.per),
            eps = parseDouble(item.eps),
            pbr = parseDouble(item.pbr),
            dvr = parseDouble(item.dividendRate),
        )

        return KoreanCompanyDetail(
            summary = summary,
            normalStock = item.type?.trim() == "ST",
            industry = "",
            currentIndicator = currentIndicator,
            historyData = emptyList(),
        )
    }

    private fun parseInt(value: String?): Int? {
        return parseLong(value)?.toInt()
    }

    private fun parseLong(value: String?): Long? {
        if (value.isNullOrBlank()) {
            return null
        }
        return try {
            BigDecimal(value.replace(",", "")).toBigInteger().longValueExact()
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDouble(value: String?): Double? {
        if (value.isNullOrBlank()) {
            return null
        }
        return value.replace(",", "").toDoubleOrNull()
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class NaverStockItem(
        @JsonProperty("itemname")
        val itemname: String?,
        @JsonProperty("itemcode")
        val itemcode: String?,
        @JsonProperty("sosok")
        val sosok: String?,
        @JsonProperty("type")
        val type: String?,
        @JsonProperty("nowVal")
        val nowVal: String?,
        @JsonProperty("marketSum")
        val marketSum: String?,
        @JsonProperty("listedStockCnt")
        val listedStockCnt: String?,
        @JsonProperty("eps")
        val eps: String?,
        @JsonProperty("per")
        val per: String?,
        @JsonProperty("pbr")
        val pbr: String?,
        @JsonProperty("dividendRate")
        val dividendRate: String?,
    )
}
