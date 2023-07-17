package com.setvect.bokslstock2.crawl.dart.service

import com.fasterxml.jackson.core.type.TypeReference
import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.crawl.dart.model.*
import com.setvect.bokslstock2.util.JsonUtil
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/**
 * OPEN DART API 이용해 기업 정보 크롤링
 */
@Service
class CrawlerDartService(
    private val bokslStockProperties: BokslStockProperties,
    private val crawlRestTemplate: RestTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val path = Paths.get("crawl/dart/corpCode.zip")

    /**
     * @return 기업목록 저장 경로
     */
    fun downloadCorporationList(): File {
        val url = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=" + bokslStockProperties.crawl.dart.key
        val responseEntity: ResponseEntity<ByteArray> = crawlRestTemplate.exchange(url, HttpMethod.GET, null, ByteArray::class.java)
        if (!responseEntity.statusCode.is2xxSuccessful) {
            throw RuntimeException("HTTP 요청 실패: " + responseEntity.statusCode)
        }
        val path = Files.write(path, responseEntity.body!!)
        log.info("파일 저장: {}", path)
        unzip(path.toFile(), path.parent.toFile())
        log.info("압축 해제: {}", path.parent)
        return path.parent.toFile()
    }

    /**
     * 기업코드 정보를 가지고 있는 XML을 파싱함
     * @return 기업(코드)목록
     */
    fun parsingCompanyList(file: File): List<CompanyCode> {
        val doc: Document = Jsoup.parse(file, "UTF-8", "")
        val lists = doc.select("result > list")
        return lists.map { element: Element ->
            val corpCode = element.select("corp_code").text()
            val corpName = element.select("corp_name").text()
            val stockCode = element.select("stock_code").text()
            val modifyDate = element.select("modify_date").text()
            CompanyCode(corpCode, corpName, stockCode, modifyDate)
        }
    }

    /**
     * @param companyCodeList 기업코드 목록
     */
    fun crawlCompanyFinancialInfo(companyAll: List<CompanyCode>) {
        val financialInfoToSave = object : DartMakerJson {
            override fun save(body: String, companyCodeMap: Map<String, CompanyCode>, year: Int, reportCode: ReportCode) {
                val saveBaseDir = File("crawl/dart/financial")
                val saveDir = File(saveBaseDir, "$year/${reportCode}")
                saveDir.mkdirs()
                val typeRef = object : TypeReference<ResDart<ResFinancialStatement>>() {}

                val parsedResponse = JsonUtil.mapper.readValue(body, typeRef)
                val financialResult = parsedResponse.list
                financialResult.groupBy { it.corpCode }.forEach { (corpCode, financialList) ->
                    val company = companyCodeMap[corpCode]!!
                    val stockCode = company.stockCode
                    val fileName = "${year}_${reportCode}_${stockCode}_${company.corpName}.json"
                    val file = File(saveDir, fileName)
                    JsonUtil.mapper.writeValue(file, financialList)
                    log.info("저장: {}", file)
                }
            }
        }

        crawl(companyAll, financialInfoToSave, "https://opendart.fss.or.kr/api/fnlttMultiAcnt.json")
        println("끝.")
    }

    /**
     * 주식 보유 현황 수집
     */
    fun crawlStockQuantity(companyAll: List<CompanyCode>) {
        val stockQuantity = object : DartMakerJson {
            override fun save(body: String, companyCodeMap: Map<String, CompanyCode>, year: Int, reportCode: ReportCode) {
                val saveBaseDir = File("crawl/dart/quantity")
                val saveDir = File(saveBaseDir, "$year/${reportCode}")
                saveDir.mkdirs()
                val typeRef = object : TypeReference<ResDart<ResStockQuantity>>() {}

                val parsedResponse = JsonUtil.mapper.readValue(body, typeRef)
                val financialResult = parsedResponse.list
                financialResult.groupBy { it.corpCode }.forEach { (corpCode, financialList) ->
                    val company = companyCodeMap[corpCode]!!
                    val stockCode = company.stockCode
                    val fileName = "${year}_${reportCode}_${stockCode}_${company.corpName}_quantity.json"
                    val file = File(saveDir, fileName)
                    JsonUtil.mapper.writeValue(file, financialList)
                    log.info("저장: {}", file)
                }
            }
        }

        crawl(companyAll, stockQuantity, "https://opendart.fss.or.kr/api/stockTotqySttus.json")
        println("끝.")
    }

    /**
     * 배당 내역 수집
     */
    fun crawlDividend(companyAll: List<CompanyCode>) {
        val stockQuantity = object : DartMakerJson {
            override fun save(body: String, companyCodeMap: Map<String, CompanyCode>, year: Int, reportCode: ReportCode) {
                val saveBaseDir = File("crawl/dart/dividend")
                val saveDir = File(saveBaseDir, "$year/${reportCode}")
                saveDir.mkdirs()
                val typeRef = object : TypeReference<ResDart<ResDividend>>() {}

                val parsedResponse = JsonUtil.mapper.readValue(body, typeRef)
                val financialResult = parsedResponse.list
                financialResult.groupBy { it.corpCode }.forEach { (corpCode, financialList) ->
                    val company = companyCodeMap[corpCode]!!
                    val stockCode = company.stockCode
                    val fileName = "${year}_${reportCode}_${stockCode}_${company.corpName}_dividend.json"
                    val file = File(saveDir, fileName)
                    JsonUtil.mapper.writeValue(file, financialList)
                    log.info("저장: {}", file)
                }
            }
        }

        crawl(companyAll, stockQuantity, "https://opendart.fss.or.kr/api/alotMatter.json")
        println("끝.")
    }

    private fun crawl(companyAll: List<CompanyCode>, toSave: DartMakerJson, endpointUrl: String) {
        val companyCodeList = companyAll.filter { StringUtils.isNotBlank(it.stockCode) }
        log.info("상장 회사수: {}", companyCodeList.size)
        val companyCodeMap = companyCodeList.associateBy { it.corpCode }

        val chunkedCompany = companyCodeList
            .chunked(100)
        val apiCallCount = AtomicInteger(0)
        for (year in 2015..LocalDate.now().year) {
            ReportCode.values().forEach { reportCode ->
                chunkedCompany.forEach inner@{ companyList ->
                    val corpCodes = companyList.joinToString(",") { it.corpCode }

                    val uri = UriComponentsBuilder
                        .fromHttpUrl(endpointUrl)
                        .queryParam("crtfc_key", bokslStockProperties.crawl.dart.key)
                        .queryParam("corp_code", corpCodes)
                        .queryParam("bsns_year", year.toString())
                        .queryParam("reprt_code", reportCode.code)
                        .build()
                        .encode()
                        .toUri()

                    var response: ResponseEntity<Any>? = null
                    // 3번까지 재시도
                    for (retry in 1..3) {
                        try {
                            response = crawlRestTemplate.exchange(uri, HttpMethod.GET, null, Any::class.java)
                        } catch (e: Exception) {
                            log.info("Exception: {}, retry: $retry", e.message)
                            if (retry == 3) {
                                throw e
                            }
                        }
                    }

                    log.info("API 호출수: ${apiCallCount.incrementAndGet()}")

                    // 100건 마다 1초 정지
                    if (apiCallCount.get() % 100 == 0) {
                        log.info("100건 호출 후 1초 정지")
                        Thread.sleep(1000)
                    }

                    if (!response!!.statusCode.is2xxSuccessful) {
                        log.info("Response without list: {}", response.statusCode)
                        return@inner
                    }

                    val body = JsonUtil.mapper.writeValueAsString(response.body)!!
                    val status = JsonUtil.mapper.readTree(body).get("status").asText()
                    if (status != "000") {
                        log.info("Response without list: $body")
                        return@inner
                    }
                    toSave.save(body, companyCodeMap, year, reportCode)
                }
            }
        }
    }

    companion object {
        fun unzip(zipFile: File, destDirectory: File) {
            ZipFile(zipFile).use { archive ->
                archive.entries.asSequence().forEach { entry ->
                    val outputFile = File(destDirectory, entry.name)
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile.mkdirs()
                        archive.getInputStream(entry).use { input ->
                            FileOutputStream(outputFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        }
    }
}