package com.setvect.bokslstock2.strategy.inheritancetax.service

import com.setvect.bokslstock2.backtest.dart.entity.CorporateDisclosureInfoEntity
import com.setvect.bokslstock2.backtest.dart.model.FinancialDetailMetric
import com.setvect.bokslstock2.backtest.dart.repository.CorporateDisclosureInfoRepository
import com.setvect.bokslstock2.crawl.koreacompany.service.CrawlerKoreanCompanyService
import com.setvect.bokslstock2.index.repository.CandleRepository
import com.setvect.bokslstock2.strategy.companyvalue.model.KoreanCompanySummary
import com.setvect.bokslstock2.strategy.inheritancetax.model.InheritanceTaxScore
import com.setvect.bokslstock2.strategy.inheritancetax.model.Quarter
import com.setvect.bokslstock2.util.NumberUtil
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
    private val candleRepository: CandleRepository,
) {
    private val notExistCompany: MutableList<String> = mutableListOf()
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun analysis(year: Int, quarter: Quarter) {
        val koreanCompanyList = crawlerKoreanCompanyService.getCompanyList()
        log.info("종목 수: ${koreanCompanyList.size}")

        // 시가총액 기준 정렬, 하위 70 ~ 90% 만 조회
        val koreanCompanySubList = koreanCompanyList
            .sortedByDescending { it.capitalization }
//            .subList((koreanCompanyList.size * 0.0).toInt(), (koreanCompanyList.size * 0.1).toInt())

        log.info("조회 종목 수: ${koreanCompanySubList.size}개")

        financialInfo(koreanCompanySubList, year, quarter)
    }

    private fun financialInfo(companyItemList: List<KoreanCompanySummary>, year: Int, quarter: Quarter) {
        val codeByName = companyItemList.associate { it.code to it.name }
        val inheritanceScore = companyItemList.map { company ->
            val assets: Long
            val liabilities: Long

            val currentYearProfit: Long
            val previousYearProfit: Long
            val twoYearsAgoProfit: Long

            try {
                val netAssetValue = getNetAssetValue(company.code, year, quarter)
                assets = netAssetValue.first
                liabilities = netAssetValue.second
                val netProfitValue = getNetProfitValue(company.code, year, quarter)
                currentYearProfit = netProfitValue.first
                previousYearProfit = netProfitValue.second
                twoYearsAgoProfit = netProfitValue.third

            } catch (e: Exception) {
                log.error("${e.message}, ${codeByName[company.code]}")
                notExistCompany.add(company.code)
                return@map null
            }
            InheritanceTaxScore(
                stockCode = company.code,
                assets = assets,
                liabilities = liabilities,
                currentYearProfit = currentYearProfit,
                previousYearProfit = previousYearProfit,
                twoYearsAgoProfit = twoYearsAgoProfit,
                capitalization = company.capitalization * 100_000_000L,
            )
        }.filterNotNull()

        log.info("종목 수: ${inheritanceScore.size}")


        log.info("\n\n---------------------------------------------\n\n")

        inheritanceScore
            .sortedByDescending { it.inheritanceTaxScore }
            .forEach { score ->
                log.info(
                    "${codeByName[score.stockCode]}(${score.stockCode}), " +
                            "상속세 점수: ${score.inheritanceTaxScore}, " +
                            "순자산가치: ${NumberUtil.comma(score.netAssetValue)}, " +
                            "순이익가치: ${NumberUtil.comma(score.netProfitValue)}, " +
                            "시총: ${NumberUtil.comma(score.capitalization)}"
                )
            }
    }

    /**
     * @return <자산, 부채>
     */
    fun getNetAssetValue(stockCode: String, year: Int, quarter: Quarter): Pair<Long, Long> {
        val totalAssets: CorporateDisclosureInfoEntity
        val totalLiabilities: CorporateDisclosureInfoEntity
        try {
            totalAssets = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.TOTAL_ASSETS, year)
                .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
            totalLiabilities = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.TOTAL_LIABILITIES, year)
                .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
        } catch (e: Exception) {
            throw e
        }

        return when (quarter) {
            Quarter.Q1 -> Pair(totalAssets.q1Value, totalLiabilities.q1Value)
            Quarter.Q2 -> Pair(totalAssets.q2Value, totalLiabilities.q2Value)
            Quarter.Q3 -> Pair(totalAssets.q3Value, totalLiabilities.q3Value)
            Quarter.Q4 -> Pair(totalAssets.q4Value, totalLiabilities.q4Value)
        }
    }

    /**
     * @return <최근 4분기 순이익, 직전 4분기 순이익, 전전 4분기 순이익>
     */
    fun getNetProfitValue(stockCode: String, year: Int, quarter: Quarter): Triple<Long, Long, Long> {
        val currentYearNetProfitThis = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.NET_PROFIT, year)
            .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
        val previousNetProfitThis1 = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.NET_PROFIT, year - 1)
            .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
        val twoYearNetProfitThis = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.NET_PROFIT, year - 2)
            .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }

        var threeYearNetProfitThis3: CorporateDisclosureInfoEntity? = null
        if (quarter != Quarter.Q4) {
            threeYearNetProfitThis3 = corporateDisclosureInfoRepository.findByMetric(stockCode, FinancialDetailMetric.NET_PROFIT, year - 3)
                .orElseThrow { RuntimeException("존재하지 않는 회사: $stockCode") }
        }

        val currentYearProfit: Long
        val previousYearProfit: Long
        val twoYearsAgoProfit: Long
        when (quarter) {
            Quarter.Q1 -> {
                currentYearProfit = currentYearNetProfitThis.q1Value + previousNetProfitThis1.q4Value + previousNetProfitThis1.q3Value + previousNetProfitThis1.q2Value
                previousYearProfit = previousNetProfitThis1.q1Value + twoYearNetProfitThis.q4Value + twoYearNetProfitThis.q3Value + twoYearNetProfitThis.q2Value
                twoYearsAgoProfit = twoYearNetProfitThis.q1Value + threeYearNetProfitThis3!!.q4Value + threeYearNetProfitThis3.q3Value + threeYearNetProfitThis3.q2Value
            }

            Quarter.Q2 -> {
                currentYearProfit = currentYearNetProfitThis.q2Value + currentYearNetProfitThis.q1Value + previousNetProfitThis1.q4Value + previousNetProfitThis1.q3Value
                previousYearProfit = previousNetProfitThis1.q2Value + previousNetProfitThis1.q1Value + twoYearNetProfitThis.q4Value + twoYearNetProfitThis.q3Value
                twoYearsAgoProfit = twoYearNetProfitThis.q2Value + twoYearNetProfitThis.q1Value + threeYearNetProfitThis3!!.q4Value + threeYearNetProfitThis3.q3Value
            }

            Quarter.Q3 -> {
                currentYearProfit = currentYearNetProfitThis.q3Value + currentYearNetProfitThis.q2Value + currentYearNetProfitThis.q1Value + previousNetProfitThis1.q4Value
                previousYearProfit = previousNetProfitThis1.q3Value + previousNetProfitThis1.q2Value + previousNetProfitThis1.q1Value + twoYearNetProfitThis.q4Value
                twoYearsAgoProfit = twoYearNetProfitThis.q3Value + twoYearNetProfitThis.q2Value + twoYearNetProfitThis.q1Value + threeYearNetProfitThis3!!.q4Value
            }

            Quarter.Q4 -> {
                currentYearProfit = currentYearNetProfitThis.getTotalValue()
                previousYearProfit = previousNetProfitThis1.getTotalValue()
                twoYearsAgoProfit = twoYearNetProfitThis.getTotalValue()
            }
        }

        return Triple(currentYearProfit, previousYearProfit, twoYearsAgoProfit)
    }
}