package com.setvect.bokslstock2.value.service

import com.setvect.bokslstock2.config.CrawlResourceProperties
import org.springframework.stereotype.Service
import java.io.File

private const val LIST_SUMMARY_JSON = "summary-list.json"
private const val LIST_DETAIL_JSON = "detail-list.json"
private const val RESULT = "value-result.xlsx"

@Service
class ValueCommonService(
    val crawlResourceProperties: CrawlResourceProperties
) {
    fun getDetailListFile(): File {
        return File(crawlResourceProperties.savePath, LIST_DETAIL_JSON)
    }

    fun getSummaryListFile(): File {
        return File(crawlResourceProperties.savePath, LIST_SUMMARY_JSON)
    }

    fun getResultFile(): File {
        return File(crawlResourceProperties.savePath, RESULT)
    }

    fun getDetailUrl(code: String): String {
        return crawlResourceProperties.url.info.replace("{code}", code)
    }
}