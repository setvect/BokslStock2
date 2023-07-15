package com.setvect.bokslstock2.crawl.unemployment

import com.setvect.bokslstock2.crawl.unemployment.service.CrawlerUnemploymentRateService
import com.setvect.bokslstock2.util.GsonUtil
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class CrawlerUnemploymentRateServiceTest {

    @Autowired
    private lateinit var crawlerUnemploymentRateService: CrawlerUnemploymentRateService
    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    @Test
    fun crawl() {
        val unemploymentData = crawlerUnemploymentRateService.crawl()
        val dir = FileUtils.getFile("crawl/미국실업률통계")
        FileUtils.forceMkdir(dir)
        val file = FileUtils.getFile(dir, "/unemployment_rate.json")

        FileUtils.writeStringToFile(file, GsonUtil.GSON.toJson(unemploymentData), "UTF-8")
        log.info("${file.absoluteFile} 저장 완료.")

    }
}