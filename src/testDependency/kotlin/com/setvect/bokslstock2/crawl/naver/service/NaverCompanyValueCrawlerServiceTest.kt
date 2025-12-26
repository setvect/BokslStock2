package com.setvect.bokslstock2.crawl.naver.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class NaverCompanyValueCrawlerServiceTest {
    @Autowired
    private lateinit var naverCompanyValueCrawlerService: NaverCompanyValueCrawlerService

    @Test
    fun crawlAndSave() {
        naverCompanyValueCrawlerService.crawlAndSave()
    }
}
