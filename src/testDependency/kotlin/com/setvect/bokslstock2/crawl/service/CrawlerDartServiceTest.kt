package com.setvect.bokslstock2.crawl.service

import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.io.File

@SpringBootTest
@ActiveProfiles("test")
class CrawlerDartServiceTest {

    @Autowired
    private lateinit var crawlerDartService: CrawlerDartService

    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Test
    fun downloadCorporationList() {
        crawlerDartService.downloadCorporationList()
        log.info("끝.")
    }

    @Test
    fun crawlCompanyFinanceInfo() {
        val companyAll = crawlerDartService.parsingCompanyList(File("crawl/dart/CORPCODE.xml"))
        log.info("기업수: {}", companyAll.size)

        val companyCodeList = companyAll.filter { StringUtils.isNotBlank(it.stockCode) }
        log.info("상장 회사수: {}", companyCodeList.size)

        companyCodeList.forEach {
            println("${it.corpCode} ${it.corpName} ${it.stockCode} ${it.modifyDate}")
        }

//        val joinToString: String = companyCodeList.filter {it.modifyDate  > "2023" }. take(100).joinToString(",") { it.corpCode }
//        println(joinToString)


//        crawlerDartService.crawlCompanyFinanceInfo(companyCodeList)

        log.info("끝.")
    }
}