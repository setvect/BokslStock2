package com.setvect.bokslstock2.crawl

import com.setvect.bokslstock2.value.service.CrawlerCompanyValueService
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class CrawlCompanyValueTest {
    @Autowired
    private lateinit var crawlCompanyValueService: CrawlerCompanyValueService

    /**
     * 종목 크롤링
     */
    @Test
    @Disabled
    fun crawlCompanyListTest() {
        crawlCompanyValueService.crawlSummaryList()
    }

    /**
     * 상세 정보 크롤링
     */
    @Test
    fun crawlDetailListTest() {
        crawlCompanyValueService.crawlDetailList()
    }
}