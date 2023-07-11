package com.setvect.bokslstock2.crawl.service

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
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
    fun parsingCorporationList() {
        val corporationList = crawlerDartService.parsingCorporationList(File("crawl/dart/CORPCODE.xml"))
        log.info("기업수: {}", corporationList.size)
        log.info("끝.")
    }
}