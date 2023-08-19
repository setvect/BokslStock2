package com.setvect.bokslstock2.backtest.dart.service

import com.setvect.bokslstock2.backtest.dart.model.*
import com.setvect.bokslstock2.crawl.dart.DartConstants
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import com.setvect.bokslstock2.util.NumberUtil.comma
import okhttp3.internal.toImmutableList
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * DART 통해서 수집한 자료 구조화
 *
 * DART에서 수집한 데이터를 메모리에 올려 놓고 데이터를 조회하기 때문에 Scope를 prototype으로 설정
 */
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DartStructuringService {
    companion object {
        val PATTERN: Pattern = Pattern.compile("(\\d{4})_(QUARTER\\d|HALF_ANNUAL|ANNUAL)_(\\d{6})_.+\\.json")
        val PATTERN_DETAIL: Pattern = Pattern.compile("(\\d{4})_(QUARTER\\d|HALF_ANNUAL|ANNUAL)_(\\d{6})_(OFS|CFS)_.+\\.json")
    }

    private val financialStatementList = mutableListOf<FinancialStatement>()
    private val financialDetailStatementList = mutableListOf<FinancialDetailStatement>()
    private val stockQuantityStatementList = mutableListOf<StockQuantityStatement>()

    private val dividendStatementList = mutableListOf<DividendStatement>()

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * DART에서 수집한 데이터를 메모리에 올려 놓음
     */
    fun loadFinancial(filter: DartFilter) {
        financialStatementList.clear()
        val result = loader(DartConstants.FINANCIAL_PATH, filter, FinancialStatement::loader)
        financialStatementList.addAll(result)
    }

    fun loadFinancialDetail(filter: DartFilter) {
        financialDetailStatementList.clear()
        val result = loader(DartConstants.FINANCIAL_DETAIL_PATH, filter, FinancialDetailStatement::loader)
        financialDetailStatementList.addAll(result)
    }

    fun loadStockQuantity(filter: DartFilter) {
        stockQuantityStatementList.clear()
        val result = loader(DartConstants.QUANTITY_PATH, filter, StockQuantityStatement::loader)
        stockQuantityStatementList.addAll(result)
    }

    fun loadDividend(filter: DartFilter) {
        dividendStatementList.clear()
        val result = loader(DartConstants.DIVIDEND_PATH, filter, DividendStatement::loader)
        dividendStatementList.addAll(result)
    }

    private fun <T> loader(
        jsonSaveDir: File,
        filter: DartFilter,
        loader: (jsonFile: File, year: Int, reportCode: ReportCode, stockCode: String) -> List<T>
    ): List<T> {
        val countAtom = AtomicInteger()
        val r = jsonSaveDir.walk()
            .filter { it.isFile && it.extension == "json" }
            .mapNotNull { financialFile ->
                val matcher = PATTERN.matcher(financialFile.name)
                if (!matcher.find()) {
                    return@mapNotNull null
                }
                val year = matcher.group(1).toInt()
                val reportCode = ReportCode.valueOf(matcher.group(2))
                val stockCode = matcher.group(3)

                val yearFilter = filter.year.isEmpty() || filter.year.contains(year)
                val quarterFilter = filter.quarter.isEmpty() || filter.quarter.contains(reportCode)
                val stockCodesFilter = filter.stockCodes.isEmpty() || filter.stockCodes.contains(stockCode)
                val find = !(yearFilter && quarterFilter && stockCodesFilter)
                if (find) {
                    return@mapNotNull null
                }

                val result = loader(financialFile, year, reportCode, stockCode)
                val count = countAtom.incrementAndGet()
                if (count % 200 == 0) {
                    log.info("load: ${comma(count)}, ${financialFile.name}")
                }
                result
            }
            .flatten()
            .toList()

        log.info("loadFinancial size: ${comma(r.size)}")
        return r
    }

    fun searchFinancial(condition: FinancialSearchCondition): List<FinancialStatement> {
        return financialStatementList
            .filter {
                if (!condition.stockCode.isNullOrEmpty()) {
                    if (!condition.stockCode.contains(it.commonStatement.stockCode)) {
                        return@filter false
                    }
                }
                if (!condition.reportCode.isNullOrEmpty()) {
                    if (!condition.reportCode.contains(it.commonStatement.reportCode)) {
                        return@filter false
                    }
                }
                if (!condition.accountNm.isNullOrEmpty()) {
                    if (!condition.accountNm.contains(it.accountNm)) {
                        return@filter false
                    }
                }
                if (!condition.fsDiv.isNullOrEmpty()) {
                    if (!condition.fsDiv.contains(it.fsDiv)) {
                        return@filter false
                    }
                }
                return@filter true
            }
            .toImmutableList()
    }

    fun searchFinancialDetail(stockCode: String, financialDetailMetric: FinancialDetailMetric): List<FinancialDetailStatement> {
        return financialDetailStatementList.filter {    //
            // TODO OFS도 되도록 변경
            if (it.fsDiv != FinancialFs.CFS) {
                return@filter false
            }
            if (it.commonStatement.stockCode != stockCode) {
                return@filter false
            }
            if (!financialDetailMetric.financialSj.contains(it.sjDiv)) {
                return@filter false
            }
            if (it.accountId == financialDetailMetric.accountId) {
                return@filter true
            }
            return@filter financialDetailMetric.accountName.contains(it.accountNm)
        }.toImmutableList()
    }

    @Deprecated("리플랙션을 웬만해서 사용하지 말자. 지저분해 진다.")
    private fun filter(target: Any, filter: Map<String, Any>): Boolean {
        return filter.entries.all { it ->
            val fieldNames = it.key.split(".")
            var currentObj: Any? = target
            fieldNames.let { names ->
                var result = false
                for (name in names) {
                    val prop: KProperty1<Any, *> = currentObj?.javaClass?.kotlin?.memberProperties?.find { it.name == name }
                        ?: throw IllegalArgumentException("No such field: $name in the class ${currentObj?.javaClass?.kotlin}")

                    currentObj = prop.get(currentObj!!)

                    if (name != names.last()) {
                        continue
                    }

                    if (it.value is Collection<*>) {
                        val values = it.value as Collection<String>
                        if (values.contains(currentObj)) {
                            result = true
                        }
                    } else {
                        result = currentObj == it.value
                    }
                }
                result
            }
        }
    }

    /**
     * 데이터를 로드한 상태에서 본 메소드 사용
     * - loadFinancial() 호출 후 사용
     * @param stockCode 종목코드
     * @return 회사의 회계 마감 기준 제공
     */
    fun getAccountClose(stockCode: String): AccountClose {
        if (financialStatementList.isEmpty()) {
            throw IllegalStateException("loadFinancial() 호출 후 사용")
        }

        val condition = FinancialSearchCondition(
            stockCode = stockCode,
            reportCode = setOf(ReportCode.ANNUAL),
            accountNm = setOf("영업이익"),
            fsDiv = setOf(FinancialFs.CFS)
        )

        val financialList = searchFinancial(condition)
        if (financialList.isEmpty()) {
            throw IllegalArgumentException("영업이익 정보가 없는 회사 $stockCode")
        }
        val financialStatement = financialList[0]

        return when (financialStatement.thstrmDtEnd!!.month.value) {
            3 -> AccountClose.Q1
            6 -> AccountClose.Q2
            9 -> AccountClose.Q3
            12 -> AccountClose.Q4
            else -> throw IllegalArgumentException("영업이익 정보가 없는 회사 $stockCode")
        }
    }

    /**
     * @return 손익계산서에 '매출액' 항목이 없다면 서비스업, 있다면 제조업
     */
    fun getIndustryType(stockCode: String): IndustryType {
        if (financialStatementList.isEmpty()) {
            throw IllegalStateException("loadFinancial() 호출 후 사용")
        }
        val condition = FinancialSearchCondition(
            stockCode = stockCode,
            accountNm = setOf("매출액"),
            fsDiv = setOf(FinancialFs.CFS)
        )

        val financialList = searchFinancial(condition)
        // 매출액 정보가 없다면 서비스업으로 간주
        return if (financialList.isEmpty()) {
            IndustryType.SERVICES
        } else {
            IndustryType.MFG
        }
    }

    /**
     * 재무제표 항목을 분기별로 변환
     * - 기업 회계 마감 분기를 우리가 흔히 사용하고 있는 분기 기준으로 변환
     *
     * 데이터를 로드한 상태에서 본 메소드 사용
     * @param stockCode 종목코드
     * @param year 연도
     * @param financialDetailMetric 재무제표 항목명
     */
    fun getFinancialItemValue(stockCode: String, year: Int, financialDetailMetric: FinancialDetailMetric): FinancialItemValue {
        val financialDetailList = searchFinancialDetail(stockCode, financialDetailMetric)

        if (financialDetailMetric.isIs()) {
            return getIncomeStatement(financialDetailList, stockCode, year, financialDetailMetric)
        } else if (financialDetailMetric.isBs()) {
            return getBalanceSheet(financialDetailList, stockCode, year, financialDetailMetric)
        }

        return FinancialItemValue(
            stockCode = stockCode,
            year = year,
            itemName = financialDetailMetric.accountName[0],
            q1Value = 0,
            q2Value = 0,
            q3Value = 0,
            q4Value = 0,
        )
    }

    /**
     * @return 재무상태표 항목을 분기별로 변환
     */
    private fun getBalanceSheet(
        financialDetailList: List<FinancialDetailStatement>,
        stockCode: String,
        year: Int,
        financialDetailMetric: FinancialDetailMetric
    ): FinancialItemValue {
        val accountClose = getAccountClose(stockCode)

        var financialItemValue = FinancialItemValue(
            stockCode = stockCode,
            year = year,
            itemName = financialDetailMetric.accountName[0],
            q1Value = 0,
            q2Value = 0,
            q3Value = 0,
            q4Value = 0,
        )

        when (accountClose) {
            // ReportCode.ANNUAL: 작년4 ~ 3월
            // ReportCode.QUARTER1: 4 ~ 6월
            // ReportCode.HALF_ANNUAL: 4 ~ 9월
            // ReportCode.QUARTER3: 10 ~ 12월
            AccountClose.Q1 -> {
                val q1 = findFinancialStatement(financialDetailList, ReportCode.ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q1Value = q1.thstrmAmount!!)

                val q2 = findFinancialStatement(financialDetailList, ReportCode.QUARTER1, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q2Value = q2.thstrmAmount!!)

                val q3 = findFinancialStatement(financialDetailList, ReportCode.HALF_ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q3Value = q3.thstrmAmount!!)

                val q4 = findFinancialStatement(financialDetailList, ReportCode.QUARTER3, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q4Value = q4.thstrmAmount!!)

                return financialItemValue
            }

            // ReportCode.QUARTER3: 1 ~ 3월
            // ReportCode.ANNUAL: 작년7 ~ 6월
            // ReportCode.QUARTER1: 7 ~ 9월
            // ReportCode.HALF_ANNUAL: 7 ~ 12월
            AccountClose.Q2 -> {
                val q1 = findFinancialStatement(financialDetailList, ReportCode.QUARTER3, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q1Value = q1.thstrmAmount!!)

                val q2 = findFinancialStatement(financialDetailList, ReportCode.ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q2Value = q2.thstrmAmount!!)

                val q3 = findFinancialStatement(financialDetailList, ReportCode.QUARTER1, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q3Value = q3.thstrmAmount!!)

                val q4 = findFinancialStatement(financialDetailList, ReportCode.HALF_ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q4Value = q4.thstrmAmount!!)

                return financialItemValue
            }

            // ReportCode.HALF_ANNUAL: 작년10 ~ 3월
            // ReportCode.QUARTER3: 4 ~ 6월
            // ReportCode.ANNUAL: 작년10 ~ 9월
            // ReportCode.QUARTER1: 10 ~ 12월
            AccountClose.Q3 -> {
                val q1 = findFinancialStatement(financialDetailList, ReportCode.HALF_ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q1Value = q1.thstrmAmount!!)

                val q2 = findFinancialStatement(financialDetailList, ReportCode.QUARTER3, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q2Value = q2.thstrmAmount!!)

                val q3 = findFinancialStatement(financialDetailList, ReportCode.ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q3Value = q3.thstrmAmount!!)

                val q4 = findFinancialStatement(financialDetailList, ReportCode.QUARTER1, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q4Value = q4.thstrmAmount!!)
                return financialItemValue
            }

            // ReportCode.QUARTER1: 1 ~ 3월
            // ReportCode.HALF_ANNUAL: 1 ~ 6월
            // ReportCode.QUARTER3: 7 ~ 9월
            // ReportCode.ANNUAL: 1 ~ 12월
            AccountClose.Q4 -> {
                val q1 = findFinancialStatement(financialDetailList, ReportCode.QUARTER1, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q1Value = q1.thstrmAmount!!)

                val q2 = findFinancialStatement(financialDetailList, ReportCode.HALF_ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q2Value = q2.thstrmAmount!!)

                val q3 = findFinancialStatement(financialDetailList, ReportCode.QUARTER3, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q3Value = q3.thstrmAmount!!)

                val q4 = findFinancialStatement(financialDetailList, ReportCode.ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q4Value = q4.thstrmAmount!!)

                return financialItemValue
            }
        }
    }

    /**
     * @return 손익계산서 항목을 분기별로 변환
     */
    private fun getIncomeStatement(
        financialDetailList: List<FinancialDetailStatement>,
        stockCode: String,
        year: Int,
        financialDetailMetric: FinancialDetailMetric
    ): FinancialItemValue {
        val accountClose = getAccountClose(stockCode)

        var financialItemValue = FinancialItemValue(
            stockCode = stockCode,
            year = year,
            itemName = financialDetailMetric.accountName[0],
            q1Value = 0,
            q2Value = 0,
            q3Value = 0,
            q4Value = 0,
        )

        when (accountClose) {
            // ReportCode.ANNUAL: 작년4 ~ 3월
            // ReportCode.QUARTER1: 4 ~ 6월
            // ReportCode.HALF_ANNUAL: 4 ~ 9월
            // ReportCode.QUARTER3: 10 ~ 12월
            AccountClose.Q1 -> {
                val beforeHalf = findFinancialStatement(financialDetailList, ReportCode.HALF_ANNUAL, year - 1) ?: return financialItemValue
                val beforeQ4 = findFinancialStatement(financialDetailList, ReportCode.QUARTER3, year - 1) ?: return financialItemValue

                val q1 = findFinancialStatement(financialDetailList, ReportCode.ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q1Value = q1.thstrmAmount!! - beforeQ4.thstrmAmount!! - beforeHalf.thstrmAddAmount!!)

                val q2 = findFinancialStatement(financialDetailList, ReportCode.QUARTER1, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q2Value = q2.thstrmAmount!!)

                val q3 = findFinancialStatement(financialDetailList, ReportCode.HALF_ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q3Value = q3.thstrmAmount!!)

                val q4 = findFinancialStatement(financialDetailList, ReportCode.QUARTER3, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q4Value = q4.thstrmAmount!!)

                return financialItemValue
            }

            // ReportCode.QUARTER3: 1 ~ 3월
            // ReportCode.ANNUAL: 작년7 ~ 6월
            // ReportCode.QUARTER1: 7 ~ 9월
            // ReportCode.HALF_ANNUAL: 7 ~ 12월
            AccountClose.Q2 -> {
                val q1 = findFinancialStatement(financialDetailList, ReportCode.QUARTER3, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q1Value = q1.thstrmAmount!!)
                val beforeHalf = findFinancialStatement(financialDetailList, ReportCode.HALF_ANNUAL, year - 1) ?: return financialItemValue

                val q2 = findFinancialStatement(financialDetailList, ReportCode.ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q2Value = q2.thstrmAmount!! - beforeHalf.thstrmAddAmount!! - q1.thstrmAmount)

                val q3 = findFinancialStatement(financialDetailList, ReportCode.QUARTER1, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q3Value = q3.thstrmAmount!!)

                val q4 = findFinancialStatement(financialDetailList, ReportCode.HALF_ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q4Value = q4.thstrmAmount!!)

                return financialItemValue
            }

            // ReportCode.HALF_ANNUAL: 작년10 ~ 3월
            // ReportCode.QUARTER3: 4 ~ 6월
            // ReportCode.ANNUAL: 작년10 ~ 9월
            // ReportCode.QUARTER1: 10 ~ 12월
            AccountClose.Q3 -> {
                val q1 = findFinancialStatement(financialDetailList, ReportCode.HALF_ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q1Value = q1.thstrmAmount!!)

                val q2 = findFinancialStatement(financialDetailList, ReportCode.QUARTER3, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q2Value = q2.thstrmAmount!!)

                val q3 = findFinancialStatement(financialDetailList, ReportCode.ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q3Value = q3.thstrmAmount!! - q1.thstrmAddAmount!! - q2.thstrmAmount)

                val q4 = findFinancialStatement(financialDetailList, ReportCode.QUARTER1, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q4Value = q4.thstrmAmount!!)
                return financialItemValue
            }

            // ReportCode.QUARTER1: 1 ~ 3월
            // ReportCode.HALF_ANNUAL: 1 ~ 6월
            // ReportCode.QUARTER3: 7 ~ 9월
            // ReportCode.ANNUAL: 1 ~ 12월
            AccountClose.Q4 -> {
                val q1 = findFinancialStatement(financialDetailList, ReportCode.QUARTER1, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q1Value = q1.thstrmAmount!!)

                val q2 = findFinancialStatement(financialDetailList, ReportCode.HALF_ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q2Value = q2.thstrmAmount!!)

                val q3 = findFinancialStatement(financialDetailList, ReportCode.QUARTER3, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q3Value = q3.thstrmAmount!!)

                val q4 = findFinancialStatement(financialDetailList, ReportCode.ANNUAL, year) ?: return financialItemValue
                financialItemValue = financialItemValue.copy(q4Value = q4.thstrmAmount!! - q1.thstrmAmount - q2.thstrmAmount - q3.thstrmAmount)

                return financialItemValue
            }
        }
    }

    private fun findFinancialStatement(
        financialDetailStatementList: List<FinancialDetailStatement>,
        reportCode: ReportCode,
        year: Int
    ): FinancialDetailStatement? {
        return financialDetailStatementList.find { it.commonStatement.reportCode == reportCode && it.commonStatement.year == year }
    }
}