package com.setvect.bokslstock2.crawl

import com.setvect.bokslstock2.value.service.CrawlCompanyValueService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class CrawlCompanyValueTest {
    @Autowired
    private lateinit var crawlCompanyValueService: CrawlCompanyValueService

    @Test
    fun crawlTest(){
        crawlCompanyValueService.craw()
    }
}