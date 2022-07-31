package com.setvect.bokslstock2.crawl

import com.setvect.bokslstock2.value.service.CrawlerCompanyValueService
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class CrawlCompanyValueTest {
    @Autowired
    private lateinit var crawlCompanyValueService: CrawlerCompanyValueService

    @Test
    @Disabled
    fun crawlCompanyListTest(){
        crawlCompanyValueService.crawlSummaryList()
    }
    @Test
    fun crawlDetailListTest(){
        crawlCompanyValueService.crawlDetailList()
    }
}