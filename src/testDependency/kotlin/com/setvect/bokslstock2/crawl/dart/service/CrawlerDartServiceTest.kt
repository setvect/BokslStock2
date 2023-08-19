package com.setvect.bokslstock2.crawl.dart.service

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

    /**
     * 기업 코드 수집
     */
    @Test
    fun downloadCorporationList() {
        crawlerDartService.downloadCorporationList()
        log.info("끝.")
    }

    /**
     * 주요 재무 정보 수집
     */
    @Test
    fun crawlCompanyFinanceInfo() {
        val companyAll = crawlerDartService.parsingCompanyList(File("crawl/dart/CORPCODE.xml"))
        log.info("기업수: {}", companyAll.size)
        crawlerDartService.crawlCompanyFinancialInfo(companyAll)

        log.info("끝.")
    }

    /**
     * 전체 재무재표 정보 수집
     */
    @Test
    fun crawlCompanyFinanceInfoDetail() {
        val companyAll = crawlerDartService.parsingCompanyList(File("crawl/dart/CORPCODE.xml"))
        log.info("기업수: {}", companyAll.size)
        crawlerDartService.crawlCompanyFinancialInfoDetail(companyAll)
        log.info("끝.")
    }

    /**
     * 주식 총수 수집
     */
    @Test
    fun crawlStockQuantity() {
        val companyAll = crawlerDartService.parsingCompanyList(File("crawl/dart/CORPCODE.xml"))
        log.info("기업수: {}", companyAll.size)
        crawlerDartService.crawlStockQuantity(companyAll)

        log.info("끝.")
    }

    /**
     * 배당 내역 수집
     */
    @Test
    fun crawlDividend() {
        val companyAll = crawlerDartService.parsingCompanyList(File("crawl/dart/CORPCODE.xml"))
        log.info("기업수: {}", companyAll.size)
        crawlerDartService.crawlDividend(companyAll)

        log.info("끝.")
    }
}