package com.setvect.bokslstock2.crawl.dart.service

import com.setvect.bokslstock2.config.BokslStockProperties
import com.setvect.bokslstock2.crawl.dart.model.CompanyCode
import org.apache.commons.compress.archivers.zip.ZipFile
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Paths

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
    fun crawlCompanyFinanceInfo(companyCodeList: List<CompanyCode>) {
        val url = "https://opendart.fss.or.kr/api/corpCode.xml?crtfc_key=" + bokslStockProperties.crawl.dart.key
        val responseEntity: ResponseEntity<ByteArray> = crawlRestTemplate.exchange(url, HttpMethod.GET, null, ByteArray::class.java)

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