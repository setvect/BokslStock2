package com.setvect.bokslstock2.crawl.dart.service

import com.setvect.bokslstock2.crawl.dart.model.CompanyCode
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.charset.StandardCharsets

interface DartMakerJson {
    fun save(
        body: String,
        companyCodeMap: Map<String, CompanyCode>,
        year: Int,
        reportCode: ReportCode
    )

    fun getSavePath(): File

    /**
     * 데이터가 없는 경우 저장. 없는 데이터를 계속 수집 요청하는걸 막기 위함
     */
    fun saveNoData(stockCode: List<String>, year: Int, reportCode: ReportCode) {
        val content = stockCode.joinToString("\n") { "${it}\t${year}\t${reportCode}" } + "\n"
        FileUtils.writeStringToFile(File(getSavePath(), "no_data.txt"), content, StandardCharsets.UTF_8, true)
    }
}