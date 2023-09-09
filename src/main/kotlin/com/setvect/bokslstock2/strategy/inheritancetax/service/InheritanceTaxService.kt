package com.setvect.bokslstock2.strategy.inheritancetax.service

import com.setvect.bokslstock2.backtest.dart.entity.CorporateDisclosureInfoEntity
import com.setvect.bokslstock2.backtest.dart.model.DartFilter
import com.setvect.bokslstock2.backtest.dart.model.FinancialDetailMetric
import com.setvect.bokslstock2.backtest.dart.repository.CorporateDisclosureInfoRepository
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import com.setvect.bokslstock2.crawl.koreacompany.service.CrawlerKoreanCompanyService
import com.setvect.bokslstock2.strategy.companyvalue.model.KoreanCompanySummary
import com.setvect.bokslstock2.strategy.inheritancetax.model.Quarter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 상속세 전략. 자세한 설명은 README.md 참고
 */
@Service
class InheritanceTaxService(
    private val crawlerKoreanCompanyService: CrawlerKoreanCompanyService,
    private val corporateDisclosureInfoRepository: CorporateDisclosureInfoRepository,
) {
    private val notExistCompany: MutableList<String> = mutableListOf()
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun analysis(year: Int, quarter: Quarter) {
        val koreanCompanyList = crawlerKoreanCompanyService.getKoreanCompanyList()
        log.info("종목 수: ${koreanCompanyList.size}")

        // 시가총액 기준 정렬, 하위 70 ~ 90% 만 조회
        val koreanCompanySubList = koreanCompanyList
            .sortedByDescending { it.capitalization }
//            .subList((koreanCompanyList.size * 0.0).toInt(), (koreanCompanyList.size * 0.1).toInt())

        log.info("조회 종목 수: ${koreanCompanySubList.size}개")

        financialInfo(koreanCompanySubList, year, quarter)
    }

    private fun financialInfo(companyItemList: List<KoreanCompanySummary>, year: Int, quarter: Quarter) {
        val filter = DartFilter(
            year = linkedSetOf(2022, year),
            quarter = ReportCode.values().toSet(),
            stockCodes = companyItemList.map { it.code }.toSet()
        )

        val codeByName = companyItemList.associate { it.code to it.name }
        val inheritanceScore = filter.stockCodes.map { stockCode ->
            val netAssetValue: Long
            val netProfitValue: Long
            try {
                netAssetValue = getNetAssetValue(stockCode, year, quarter)
                netProfitValue = getNetProfitValue(stockCode, year, quarter)
            } catch (e: Exception) {
                log.error("${e.message}, ${codeByName[stockCode]}")
                notExistCompany.add(stockCode)
                return@map null
            }
            InheritanceTaxScore(
                stockCode = stockCode,
                netAssetValue = netAssetValue,
                netProfitValue = netProfitValue,
            )
        }.filterNotNull()

        log.info("종목 수: ${inheritanceScore.size}")
        inheritanceScore.forEach { score ->
            log.info("${codeByName[score.stockCode]}(${score.stockCode})  ${score.netAssetValue} ${score.netProfitValue}")
        }
    }

    /**
     * @return 순자산가치
     */
    fun getNetAssetValue(stockCode: String, year: Int, quarter: Quarter): Long {
        val totalAssets: CorporateDisclosureInfoEntity
        val totalLiabilities: CorporateDisclosureInfoEntity
        try {
            totalAssets = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.TOTAL_ASSETS, year)
                .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
            totalLiabilities = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.TOTAL_LIABILITIES, year)
                .orElseThrow() { RuntimeException("존재하지 않는 회사: $stockCode") }
        } catch (e: Exception) {
            throw e
        }

        return when (quarter) {
            Quarter.Q1 -> totalAssets.q1Value - totalLiabilities.q1Value
            Quarter.Q2 -> totalAssets.q2Value - totalLiabilities.q2Value
            Quarter.Q3 -> totalAssets.q3Value - totalLiabilities.q3Value
            Quarter.Q4 -> totalAssets.q4Value - totalLiabilities.q4Value
        }
    }

    /**
     * @return 순이익가치
     */
    fun getNetProfitValue(stockCode: String, year: Int, quarter: Quarter): Long {
        val netProfitThis = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.NET_PROFIT, year)
            .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
        val netProfitThis1 = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.NET_PROFIT, year - 1)
            .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
        val netProfitThis2 = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.NET_PROFIT, year - 2)
            .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }

        var netProfitThis3: CorporateDisclosureInfoEntity? = null
        if (quarter != Quarter.Q4) {
            netProfitThis3 = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.NET_PROFIT, year - 3)
                .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
        }

        val yearProfit: Long
        val yearProfit1: Long
        val yearProfit2: Long
        when (quarter) {
            Quarter.Q1 -> {
                yearProfit = netProfitThis.q1Value + netProfitThis1.q4Value + netProfitThis1.q3Value + netProfitThis1.q2Value
                yearProfit1 = netProfitThis1.q1Value + netProfitThis2.q4Value + netProfitThis2.q3Value + netProfitThis2.q2Value
                yearProfit2 = netProfitThis2.q1Value + netProfitThis3!!.q4Value + netProfitThis3.q3Value + netProfitThis3.q2Value
            }

            Quarter.Q2 -> {
                yearProfit = netProfitThis.q2Value + netProfitThis.q1Value + netProfitThis1.q4Value + netProfitThis1.q3Value
                yearProfit1 = netProfitThis1.q2Value + netProfitThis1.q1Value + netProfitThis2.q4Value + netProfitThis2.q3Value
                yearProfit2 = netProfitThis2.q2Value + netProfitThis2.q1Value + netProfitThis3!!.q4Value + netProfitThis3.q3Value
            }

            Quarter.Q3 -> {
                yearProfit = netProfitThis.q3Value + netProfitThis.q2Value + netProfitThis.q1Value + netProfitThis1.q4Value
                yearProfit1 = netProfitThis1.q3Value + netProfitThis1.q2Value + netProfitThis1.q1Value + netProfitThis2.q4Value
                yearProfit2 = netProfitThis2.q3Value + netProfitThis2.q2Value + netProfitThis2.q1Value + netProfitThis3!!.q4Value
            }

            Quarter.Q4 -> {
                yearProfit = netProfitThis.getTotalValue()
                yearProfit1 = netProfitThis1.getTotalValue()
                yearProfit2 = netProfitThis2.getTotalValue()
            }
        }
        // 순이익가치 = 최근 3년 순이익 가중평균 = 최근 연도 3/6 + 전 연도 2/6 + 전전 1/6
        return (yearProfit * 3 / 6) + (yearProfit1 * 2 / 6) + (yearProfit2 * 1 / 6)
    }

    data class InheritanceTaxScore(
        val stockCode: String,
        // 순자산가치
        val netAssetValue: Long,
        // 순이익가치
        val netProfitValue: Long,
    )
}