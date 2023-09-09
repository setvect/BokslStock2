package com.setvect.bokslstock2.backtest.dart.service

import com.setvect.bokslstock2.backtest.dart.entity.CorporateDisclosureInfoEntity
import com.setvect.bokslstock2.backtest.dart.model.DartFilter
import com.setvect.bokslstock2.backtest.dart.model.FinancialDetailMetric
import com.setvect.bokslstock2.backtest.dart.model.FinancialItemValue
import com.setvect.bokslstock2.backtest.dart.repository.CorporateDisclosureInfoRepository
import com.setvect.bokslstock2.crawl.dart.DartConstants
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import com.setvect.bokslstock2.crawl.dart.service.CrawlerDartService
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * DART 통해서 수집한 자료 구조화
 *
 * DART에서 수집한 데이터를 메모리에 올려 놓고 데이터를 조회하기 때문에 Scope를 prototype으로 설정
 */
@Service
class DartDataStoreService(
    private val dartStructuringService: DartStructuringService,
    private val crawlerDartService: CrawlerDartService,
    private val corporateDisclosureInfoRepository: CorporateDisclosureInfoRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)
    fun loadFinancial() {
        val companyAll = crawlerDartService.parsingCompanyList(DartConstants.CORP_CODE_PATH)
        val companyCodeList = companyAll.filter { StringUtils.isNotBlank(it.stockCode) }
        val stockCodes = companyCodeList.map { it.stockCode }.toSet()

        var count = 0
        stockCodes.chunked(500).forEach { codes ->
            val filter = DartFilter(
                year = (2015..2023).toSet(),
                quarter = ReportCode.values().toSet(),
                stockCodes = codes.toSet()
            )
            count = count + codes.size
            val runningMsg = "${count}/${stockCodes.size}건"
            store(filter, runningMsg)
        }
    }

    private fun store(filter: DartFilter, runningMsg: String) {
        dartStructuringService.loadFinancial(filter)
        dartStructuringService.loadFinancialDetail(filter)
        var count = 0

        filter.stockCodes.forEach { stockCode ->
            (2016..2023).forEach { year ->
                FinancialDetailMetric.values().forEach label1@{ financialDetailMetric ->
                    val financialItemValue: FinancialItemValue
                    try {
                        financialItemValue = dartStructuringService.getFinancialItemValue(stockCode, year, financialDetailMetric)
                    } catch (e: Exception) {
                        log.info("데이터 없음 $stockCode $year $financialDetailMetric")
                        return@label1
                    }
                    if (!financialItemValue.exist()) {
                        log.info("데이터 없음 $stockCode $year $financialDetailMetric")
                        return@label1
                    }

                    val existingEntity = corporateDisclosureInfoRepository.findByMetric(
                        stockCode,
                        financialDetailMetric,
                        year
                    )

                    val entity = if (existingEntity.isPresent) {
                        val foundEntity = existingEntity.get()
                        // Update the existing entity
                        foundEntity.accountClose = financialItemValue.accountClose
                        foundEntity.itemName = financialItemValue.itemName
                        foundEntity.q1Value = financialItemValue.q1Value
                        foundEntity.q2Value = financialItemValue.q2Value
                        foundEntity.q3Value = financialItemValue.q3Value
                        foundEntity.q4Value = financialItemValue.q4Value
                        foundEntity
                    } else {
                        CorporateDisclosureInfoEntity(
                            code = stockCode,
                            year = year,
                            accountClose = financialItemValue.accountClose,
                            financialDetailMetric = financialDetailMetric,
                            itemName = financialItemValue.itemName,
                            q1Value = financialItemValue.q1Value,
                            q2Value = financialItemValue.q2Value,
                            q3Value = financialItemValue.q3Value,
                            q4Value = financialItemValue.q4Value
                        )
                    }
                    corporateDisclosureInfoRepository.save(entity)
                    count++
                    if (count % 100 == 0) {
                        log.info("진행: $runningMsg :: ${count}건 $stockCode $year $financialDetailMetric")
                    }
                }
            }
        }
        log.info("진행: $runningMsg :: ${count}건 ")
    }
}