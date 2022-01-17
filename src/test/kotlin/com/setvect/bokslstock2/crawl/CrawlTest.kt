package com.setvect.bokslstock2.crawl

import com.setvect.bokslstock2.index.service.CrawlService
import com.setvect.bokslstock2.util.DateRange
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class CrawlTest {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    @Autowired
    private lateinit var crawlService: CrawlService

    @Test
    fun test() {
        log.info("{}", crawlService)
        val range = DateRange("2022-01-01T00:00:00", "2022-01-31T00:00:00")
        crawlService.crawlStock("233740", range)
    }
}