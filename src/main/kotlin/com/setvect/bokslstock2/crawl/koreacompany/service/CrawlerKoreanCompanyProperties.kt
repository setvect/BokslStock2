package com.setvect.bokslstock2.crawl.koreacompany.service

import java.io.File


/**
 * TODO 이름 참 맘에 안든다.
 */
object CrawlerKoreanCompanyProperties {
    private const val LIST_SUMMARY_JSON = "summary-list.json"
    private const val LIST_DETAIL_JSON = "detail-list.json"
    private const val RESULT = "value-result.xlsx"
    private val SAVE_PATH = File("./crawl/stock.naver.com")
    const val INFO = "https://finance.naver.com/item/main.nhn?code={code}"
    const val USER_LIST = "https://finance.naver.com/sise/sise_market_sum.nhn?sosok={marketSeq}&page={page}"
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    const val STOCK_PRICE =
        "https://api.finance.naver.com/siseJson.naver?symbol={code}&requestType=1&startTime={start}&endTime={end}&timeframe=day"

    fun getDetailListFile(): File {
        return File(SAVE_PATH, LIST_DETAIL_JSON)
    }

    fun getSummaryListFile(): File {
        return File(SAVE_PATH, LIST_SUMMARY_JSON)
    }

    fun getResultFile(): File {
        return File(SAVE_PATH, RESULT)
    }

    fun getDetailUrl(code: String): String {
        return INFO.replace("{code}", code)
    }

    fun getUrlList(): String {
        return USER_LIST
    }
}