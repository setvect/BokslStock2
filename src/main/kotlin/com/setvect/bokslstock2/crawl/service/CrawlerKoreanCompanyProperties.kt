package com.setvect.bokslstock2.crawl.service

import com.setvect.bokslstock2.config.BokslStockProperties
import org.springframework.stereotype.Service
import java.io.File


/**
 * TODO 이름 참 맘에 안든다.
 */
@Service
class CrawlerKoreanCompanyProperties(
    val bokslStockProperties: BokslStockProperties
) {
    companion object {
        private const val LIST_SUMMARY_JSON = "summary-list.json"
        private const val LIST_DETAIL_JSON = "detail-list.json"
        private const val RESULT = "value-result.xlsx"
    }
    fun getDetailListFile(): File {
        return File(bokslStockProperties.crawl.korea.savePath, LIST_DETAIL_JSON)
    }

    fun getSummaryListFile(): File {
        return File(bokslStockProperties.crawl.korea.savePath, LIST_SUMMARY_JSON)
    }

    fun getResultFile(): File {
        return File(bokslStockProperties.crawl.korea.savePath, RESULT)
    }

    fun getDetailUrl(code: String): String {
        return bokslStockProperties.crawl.korea.url.info.replace("{code}", code)
    }

    fun getUrlList(): String {
        return bokslStockProperties.crawl.korea.url.list
    }
}