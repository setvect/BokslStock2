package com.setvect.bokslstock2.crawl.dart.service

import com.fasterxml.jackson.core.type.TypeReference
import com.setvect.bokslstock2.backtest.dart.model.CommonStatement
import com.setvect.bokslstock2.backtest.dart.model.DetailStatement
import com.setvect.bokslstock2.backtest.dart.model.FinancialFs
import com.setvect.bokslstock2.backtest.dart.service.DartStructuringService
import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.crawl.dart.DartConstants
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
                val saveBaseDir = DartConstants.FINANCIAL_PATH
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
                val saveBaseDir = DartConstants.QUANTITY_PATH
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
                val saveBaseDir = DartConstants.DIVIDEND_PATH
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
                            break
                        } catch (e: Exception) {
                            log.info("Exception: {}, retry: $retry", e.message)
                            Thread.sleep(1000)
                            if (retry == 3) {
                                throw e
                            }
                        }
                    }

                    log.info("API 호출수: ${apiCallCount.incrementAndGet()}")

                    // 100건 마다 1초 정지
                    if (apiCallCount.get() % 100 == 0) {
                        // 분당 호출 건수가 1,000건으로 제한됨
                        log.info("100건 호출 후 6.1초 정지")
                        Thread.sleep(6100)
                    }

                    if (!response!!.statusCode.is2xxSuccessful) {
                        log.info("Response without list: {}", response.statusCode)
                        return@inner
                    }

                    val body = JsonUtil.mapper.writeValueAsString(response.body)!!
                    val status = JsonUtil.mapper.readTree(body).get("status").asText()
                    if (status == "020") {
                        log.info("API 사용한도 초과했음, Response without list: $body")
                        return
                    } else if (status != "000") {
                        log.info("수집 대상 없음 ${year}년, reportCode: ${reportCode}, corpCodes: ${companyList.map { it.stockCode }}, Response without list: $body")
                        return@inner
                    }
                    toSave.save(body, companyCodeMap, year, reportCode)
                }
            }
        }
    }

    /**
     * 단일회사 전체 기업 재무재표 수집
     * 전체 기업 재무제표는 하나씩 조회 해야됨
     * 존재하는 데이터를 조회 하기 위해서 주요 재무제표에 수집된 내용이 존재하는지 확인한 후 수집 실행
     * @param companyCodeList 기업코드 목록
     */
    fun crawlCompanyFinancialInfoDetail(companyAll: List<CompanyCode>) {
        val existFinancialInfo = makeExistFinancialInfo(DartConstants.FINANCIAL_PATH)
        val existFinancialDetailInfo = makeExistFinancialDetailInfo(DartConstants.FINANCIAL_DETAIL_PATH)
        log.info("존재하는 재무제표 수: {}, 이미수집된 재무재표 수: {}", existFinancialInfo.size, existFinancialDetailInfo.size)

        val companyCodeList = companyAll.filter { StringUtils.isNotBlank(it.stockCode) }
        log.info("상장 회사수: {}", companyCodeList.size)
        val companyCodeMap = companyCodeList.associateBy { it.corpCode }
        val apiCallCount = AtomicInteger(0)
        for (year in 2021..LocalDate.now().year) {
            ReportCode.values().forEach { reportCode ->
                for (fs in FinancialFs.values()) {
                    companyCodeList
                        .filter { existFinancialInfo.contains(CommonStatement(year, reportCode, it.stockCode)) }
                        // 이미 수집한 재무제표는 수집하지 않음
                        .filter { !existFinancialDetailInfo.contains(DetailStatement(year, reportCode, it.stockCode, fs)) }
                        .forEach inner@{ company ->
                            val uri = UriComponentsBuilder
                                .fromHttpUrl("https://opendart.fss.or.kr/api/fnlttSinglAcntAll.json")
                                .queryParam("crtfc_key", bokslStockProperties.crawl.dart.key)
                                .queryParam("corp_code", company.corpCode)
                                .queryParam("bsns_year", year.toString())
                                .queryParam("reprt_code", reportCode.code)
                                .queryParam("fs_div", fs)
                                .build()
                                .encode()
                                .toUri()

                            var response: ResponseEntity<Any>? = null
                            // 3번까지 재시도
                            for (retry in 1..3) {
                                try {
                                    response = crawlRestTemplate.exchange(uri, HttpMethod.GET, null, Any::class.java)
                                    break
                                } catch (e: Exception) {
                                    log.info("Exception: {}, retry: $retry", e.message)
                                    Thread.sleep(1000)
                                    if (retry == 3) {
                                        throw e
                                    }
                                }
                            }

                            log.info("API 호출수: ${apiCallCount.incrementAndGet()}")

                            // 100건 마다 1초 정지
                            if (apiCallCount.get() % 100 == 0) {
                                // 분당 호출 건수가 1,000건으로 제한됨
                                log.info("100건 호출 후 6.1초 정지")
                                Thread.sleep(6100)
                            }

                            if (!response!!.statusCode.is2xxSuccessful) {
                                log.info("Response without list: {}", response.statusCode)
                                return@inner
                            }

                            val resBodyJson = JsonUtil.mapper.writeValueAsString(response.body)!!
                            val status = JsonUtil.mapper.readTree(resBodyJson).get("status").asText()
                            if (status == "020") {
                                log.info("API 사용한도 초과했음, Response without list: $resBodyJson")
                                return
                            } else if (status != "000") {
                                log.info("수집 대상 없음 ${year}년, reportCode: ${reportCode}, corpCodes: ${company.stockCode}, fs: ${fs}, Response without list: $resBodyJson")
                                return@inner
                            }

                            saveDetail(resBodyJson, companyCodeMap, year, reportCode, fs)
                        }
                }
            }
        }
        println("끝.")
    }

    private fun makeExistFinancialInfo(file: File): Set<CommonStatement> {
        val existFinancialInfo = file.walk()
            .filter { it.isFile && it.extension == "json" }
            .mapNotNull { financialFile ->
                val matcher = DartStructuringService.PATTERN.matcher(financialFile.name)
                if (!matcher.find()) {
                    return@mapNotNull null
                }
                val year = matcher.group(1).toInt()
                val reportCode = ReportCode.valueOf(matcher.group(2))
                val stockCode = matcher.group(3)

                CommonStatement(year, reportCode, stockCode)
            }
            .toSet()
        return existFinancialInfo
    }

    private fun makeExistFinancialDetailInfo(file: File): Set<DetailStatement> {
        val existFinancialInfo = file.walk()
            .filter { it.isFile && it.extension == "json" }
            .mapNotNull { financialDetailFile ->
                val matcher = DartStructuringService.PATTERN_DETAIL.matcher(financialDetailFile.name)
                if (!matcher.find()) {
                    return@mapNotNull null
                }
                val year = matcher.group(1).toInt()
                val reportCode = ReportCode.valueOf(matcher.group(2))
                val stockCode = matcher.group(3)
                val fs = matcher.group(4)

                DetailStatement(year, reportCode, stockCode, FinancialFs.valueOf(fs))
            }
            .toSet()
        return existFinancialInfo
    }

    private fun saveDetail(
        resBodyJson: String,
        companyCodeMap: Map<String, CompanyCode>,
        year: Int,
        reportCode: ReportCode,
        fs: FinancialFs
    ) {
        val saveBaseDir = DartConstants.FINANCIAL_DETAIL_PATH
        val saveDir = File(saveBaseDir, "$year/${reportCode}")
        saveDir.mkdirs()
        val typeRef = object : TypeReference<ResDart<ResFinancialDetailStatement>>() {}

        val parsedResponse = JsonUtil.mapper.readValue(resBodyJson, typeRef)
        val financialResult = parsedResponse.list
        financialResult.groupBy { it.corpCode }.forEach { (corpCode, financialList) ->
            val company = companyCodeMap[corpCode]!!
            val stockCode = company.stockCode
            val fileName = "${year}_${reportCode}_${stockCode}_${fs}_${company.corpName}.json"
            val file = File(saveDir, fileName)

            // 수집 안되는 항목 수동으로 넣기
            financialList.forEach {
                it.stockCode = stockCode
                it.fsDiv = fs.code
            }

            JsonUtil.mapper.writeValue(file, financialList)
            log.info("저장: {}", file)
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