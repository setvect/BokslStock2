package com.setvect.bokslstock2.crawl.dart.service

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

        crawlerDartService.crawlCompanyFinancialInfo(companyAll)

        log.info("끝.")
    }
}