package com.setvect.bokslstock2.strategy.inheritancetax.service

import com.setvect.bokslstock2.backtest.dart.model.DartFilter
import com.setvect.bokslstock2.backtest.dart.model.FinancialDetailMetric
import com.setvect.bokslstock2.backtest.dart.model.FinancialItemValue
import com.setvect.bokslstock2.backtest.dart.service.DartStructuringService
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import com.setvect.bokslstock2.crawl.koreacompany.service.CrawlerKoreanCompanyService
import com.setvect.bokslstock2.strategy.companyvalue.model.KoreanCompanySummary
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 상속세 전략. 자세한 설명은 README.md 참고
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

        // 시가총액 기준 정렬, 하위 70 ~ 90% 만 조회
        val koreanCompanySubList = koreanCompanyList
            .sortedByDescending { it.capitalization }
            .subList((koreanCompanyList.size * 0.0).toInt(), (koreanCompanyList.size * 0.1).toInt())

        log.info("조회 종목 수: ${koreanCompanySubList.size}개")

        financialInfo(koreanCompanySubList)
        log.info("-------------")

//        println("존재하지 않는 회사: ${notExistCompany.size}개")
//        notExistCompany.forEach {
//            println(it)
//        }
    }

    private fun financialInfo(companyItemList: List<KoreanCompanySummary>) {
        val filter = DartFilter(
            year = linkedSetOf(2022, 2023),
            quarter = ReportCode.values().toSet(),
            stockCodes = companyItemList.map { it.code }.toSet()
        )

        val codeByName = companyItemList.associate { it.code to it.name }

        dartStructuringService.loadFinancial(filter)
        dartStructuringService.loadFinancialDetail(filter)

        val inheritanceScore = filter.stockCodes.map {
            val totalAssets: FinancialItemValue
            val totalLiabilities: FinancialItemValue
            try {
                totalAssets = dartStructuringService.getFinancialItemValue(it, 2023, FinancialDetailMetric.TOTAL_ASSETS)
                totalLiabilities = dartStructuringService.getFinancialItemValue(it, 2023, FinancialDetailMetric.TOTAL_LIABILITIES)
                if (totalAssets.q2Value == 0L) {
                    throw RuntimeException("$it 총자산이 0임")
                }
            } catch (e: Exception) {
                notExistCompany.add("${codeByName[it]}($it)")
                return@map null
            }


            val totalCapital = totalAssets.q2Value - totalLiabilities.q2Value

            InheritanceTaxScore(
                stockCode = it,
                netAssetValue = totalCapital,
                netProfitValue = 0,
            )
        }.filterNotNull()

        log.info("종목 수: ${inheritanceScore.size}")
        inheritanceScore.forEach { score ->
            log.info("${codeByName[score.stockCode]}(${score.stockCode})  ${score.netAssetValue} ${score.netProfitValue}")
        }
    }

    data class InheritanceTaxScore(
        val stockCode: String,
        // 순자산가치
        val netAssetValue: Long,
        // 순이익가치
        val netProfitValue: Long,
    )
}