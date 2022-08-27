package com.setvect.bokslstock2.value.service

import com.setvect.bokslstock2.config.BokslStockProperties
import org.springframework.stereotype.Service
import java.io.File

private const val LIST_SUMMARY_JSON = "summary-list.json"
private const val LIST_DETAIL_JSON = "detail-list.json"
private const val RESULT = "value-result.xlsx"

@Service
class ValueCommonService(
    val bokslStockProperties: BokslStockProperties
) {
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
}