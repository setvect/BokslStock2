package com.setvect.bokslstock2.strategy.inheritancetax.service

import com.setvect.bokslstock2.backtest.dart.model.DartFilter
import com.setvect.bokslstock2.backtest.dart.model.FinancialDetailMetric
import com.setvect.bokslstock2.backtest.dart.service.DartStructuringService
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import com.setvect.bokslstock2.crawl.koreacompany.service.CrawlerKoreanCompanyService
import com.setvect.bokslstock2.strategy.companyvalue.model.KoreanCompanySummary
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 상소세 전략. 자세한 설명은 README.md 참고
 */
@Service
class InheritanceTaxService(
    private val crawlerKoreanCompanyService: CrawlerKoreanCompanyService,
    private val dartStructuringService: DartStructuringService,
) {
    private val notExistCompany: MutableList<String> = mutableListOf()
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun analysis() {
        val koreanCompanyList = crawlerKoreanCompanyService.getKoreanCompanyList()
        log.info("종목 수: ${koreanCompanyList.size}")

        // koreanCompanyList 100개씩 chunk 나누기
        val chunkSize = 1000

        koreanCompanyList.chunked(chunkSize).forEach { companyList ->
            financialInfo(companyList)
            log.info("-------------")

        }

        println("존재하지 않는 회사: ${notExistCompany.size}개")
        notExistCompany.forEach {
            println(it)
        }
    }

    private fun financialInfo(companyItemList: List<KoreanCompanySummary>) {
        val filter = DartFilter(
            year = linkedSetOf(2021, 2022),
            quarter = ReportCode.values().toSet(),
            stockCodes = companyItemList.map { it.code }.toSet()
        )

        val codeByName = companyItemList.associate { it.code to it.name }

        dartStructuringService.loadFinancial(filter)
        dartStructuringService.loadFinancialDetail(filter)
        FinancialDetailMetric.values().forEach {
            filter.stockCodes.forEach inner@{ stockCode ->
                try {
                    val incomeStatement = dartStructuringService.getFinancialItemValue(stockCode, 2022, it)
//                    println("${codeByName[stockCode]} 2022년 ${it.accountName[0]}: $incomeStatement")
                } catch (e: Exception) {
                    log.warn("재무제표 오류: ${codeByName[stockCode]}, ${it.accountName[0]}")
                    this.notExistCompany.add("${codeByName[stockCode]}, ${it.accountName[0]}")
                    return@inner
                }
            }
        }
    }
}