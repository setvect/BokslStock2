package com.setvect.bokslstock2.crawl

import com.setvect.bokslstock2.index.service.crawl.CrawlFinvizService
import com.setvect.bokslstock2.util.DateUtil
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("test")
class CrawlFinvizServiceTest {
    @Autowired
    private lateinit var crawlFinvizService: CrawlFinvizService
    private val log = LoggerFactory.getLogger(javaClass)

    @Test
    fun crawl() {
        try {
            val result = crawlFinvizService.crawl(500)
            log.info("header size: ${result.header.size}")
            log.info("data size: ${result.dataMatrix.size}")

            val convertJson = crawlFinvizService.convertJson(result)
            log.info("json: $convertJson")
            val date = DateUtil.format(LocalDateTime.now(), "yyyyMMdd_HHmmss")

            val dir = FileUtils.getFile("crawl/finviz.com")
            FileUtils.forceMkdir(dir)
            val file = FileUtils.getFile(dir, "/finviz_$date.json")
            FileUtils.writeStringToFile(file, convertJson, "UTF-8")

            log.info("$file 저장 완료.")
            log.info("끝.")
        } catch (e: Exception) {
            log.error("${e.message}", e)
        }
    }
}